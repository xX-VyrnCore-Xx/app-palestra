package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanTemplateExerciseEntity
import com.vyrncore.palestra.data.repository.AiPlanBuilderRepository
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.PlanTemplateRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DraftPlanExercise(
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double? = null,
    val restSeconds: Int = 90,
    val notes: String? = null,
)

/** Common plan categories offered in the editor; a PT can still leave this unset. */
val PLAN_CATEGORIES = listOf("Full Body", "Push", "Pull", "Gambe", "Cardio", "Mobilità")

@HiltViewModel
class PlanEditorViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    private val planTemplateRepository: PlanTemplateRepository,
    private val aiPlanBuilderRepository: AiPlanBuilderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])
    private val ptId: String get() = authRepository.currentUserId.orEmpty()

    val exerciseCatalog = workoutRepository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates = planTemplateRepository.observeForPt(ptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Injuries/limitations the PT recorded for this client - shown as a warning while building the plan. */
    val clientInjuries = authRepository.observeProfile(clientId).map { it?.injuries }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _draftExercises = MutableStateFlow<List<DraftPlanExercise>>(emptyList())
    val draftExercises: StateFlow<List<DraftPlanExercise>> = _draftExercises.asStateFlow()

    fun addExercise(exerciseId: String, exerciseName: String) {
        if (_draftExercises.value.any { it.exerciseId == exerciseId }) return
        _draftExercises.value = _draftExercises.value + DraftPlanExercise(exerciseId, exerciseName)
    }

    fun createCustomExercise(name: String, muscleGroup: String, imageUrl: String?) {
        viewModelScope.launch {
            val ptId = authRepository.currentUserId.orEmpty()
            val id = workoutRepository.addCustomExercise(name, muscleGroup, ptId, imageUrl)
            addExercise(id, name)
        }
    }

    fun removeExercise(exerciseId: String) {
        _draftExercises.value = _draftExercises.value.filterNot { it.exerciseId == exerciseId }
    }

    fun updateExercise(exerciseId: String, sets: Int, reps: Int, restSeconds: Int) {
        _draftExercises.value = _draftExercises.value.map {
            if (it.exerciseId == exerciseId) it.copy(targetSets = sets, targetReps = reps, restSeconds = restSeconds) else it
        }
    }

    fun updateExerciseNote(exerciseId: String, notes: String) {
        _draftExercises.value = _draftExercises.value.map {
            if (it.exerciseId == exerciseId) it.copy(notes = notes.ifBlank { null }) else it
        }
    }

    /** Loads a saved template's exercises into the draft, replacing whatever's there - a PT
     * starts a new plan from "Push day" instead of re-adding every exercise by hand. */
    fun applyTemplate(templateId: String) {
        viewModelScope.launch {
            val exercises = planTemplateRepository.observeExercisesForTemplate(templateId).first()
            val catalog = exerciseCatalog.value
            _draftExercises.value = exercises.map { templateExercise ->
                DraftPlanExercise(
                    exerciseId = templateExercise.exerciseId,
                    exerciseName = catalog.firstOrNull { it.id == templateExercise.exerciseId }?.name ?: "Esercizio",
                    targetSets = templateExercise.targetSets,
                    targetReps = templateExercise.targetReps,
                    targetWeightKg = templateExercise.targetWeightKg,
                    restSeconds = templateExercise.restSeconds,
                    notes = templateExercise.notes,
                )
            }
        }
    }

    fun saveAsTemplate(name: String, category: String?) {
        val exercises = _draftExercises.value
        if (exercises.isEmpty()) return
        viewModelScope.launch {
            planTemplateRepository.saveTemplate(
                ptId = ptId,
                name = name,
                category = category,
                exercises = exercises.map {
                    PlanTemplateExerciseEntity(
                        id = "",
                        templateId = "",
                        exerciseId = it.exerciseId,
                        orderIndex = 0,
                        targetSets = it.targetSets,
                        targetReps = it.targetReps,
                        targetWeightKg = it.targetWeightKg,
                        restSeconds = it.restSeconds,
                        notes = it.notes,
                    )
                },
            )
        }
    }

    private val _aiGenerating = MutableStateFlow(false)
    val aiGenerating: StateFlow<Boolean> = _aiGenerating.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    /** Replaces the draft with exercises the AI proposed for [goal] - always still just a draft
     * the PT reviews (sets/reps/rest are editable, nothing is saved until "Salva scheda"). */
    fun generateWithAi(goal: String) {
        if (goal.isBlank()) return
        _aiError.value = null
        _aiGenerating.value = true
        viewModelScope.launch {
            runCatching { aiPlanBuilderRepository.generatePlan(goal, clientId) }
                .onSuccess { suggestion ->
                    val catalog = exerciseCatalog.value
                    _draftExercises.value = suggestion.exercises.mapNotNull { suggested ->
                        val name = catalog.firstOrNull { it.id == suggested.exerciseId }?.name ?: return@mapNotNull null
                        DraftPlanExercise(
                            exerciseId = suggested.exerciseId,
                            exerciseName = name,
                            targetSets = suggested.sets,
                            targetReps = suggested.reps,
                            restSeconds = suggested.restSeconds,
                        )
                    }
                }
                .onFailure { e -> _aiError.value = e.message ?: "Errore durante la generazione della scheda." }
            _aiGenerating.value = false
        }
    }

    fun clearAiError() {
        _aiError.value = null
    }

    fun savePlan(name: String, description: String?, category: String?, onSaved: () -> Unit) {
        val ptId = authRepository.currentUserId.orEmpty()
        val exercises = _draftExercises.value
        // Rough estimate: ~1.5 min per set (work + rest), so a PT sees a sensible default
        // without having to type a duration by hand.
        val estimatedMinutes = exercises.sumOf { it.targetSets } * 3 / 2
        viewModelScope.launch {
            workoutRepository.createPlan(
                name = name,
                description = description,
                createdByPtId = ptId,
                assignedToUserId = clientId,
                category = category,
                estimatedMinutes = estimatedMinutes.takeIf { it > 0 },
                exercises = exercises.map {
                    PlanExerciseEntity(
                        id = "",
                        planId = "",
                        exerciseId = it.exerciseId,
                        orderIndex = 0,
                        targetSets = it.targetSets,
                        targetReps = it.targetReps,
                        restSeconds = it.restSeconds,
                        notes = it.notes,
                        syncStatus = SyncStatus.PENDING_CREATE,
                    )
                },
            )
            syncScheduler.syncNow()
            onSaved()
        }
    }
}
