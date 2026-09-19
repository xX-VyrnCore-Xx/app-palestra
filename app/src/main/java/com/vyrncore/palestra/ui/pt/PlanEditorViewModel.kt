package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DraftPlanExercise(
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val restSeconds: Int = 90,
)

/** Common plan categories offered in the editor; a PT can still leave this unset. */
val PLAN_CATEGORIES = listOf("Full Body", "Push", "Pull", "Gambe", "Cardio", "Mobilità")

@HiltViewModel
class PlanEditorViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])

    val exerciseCatalog = workoutRepository.observeExercises()
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
                        syncStatus = SyncStatus.PENDING_CREATE,
                    )
                },
            )
            syncScheduler.syncNow()
            onSaved()
        }
    }
}
