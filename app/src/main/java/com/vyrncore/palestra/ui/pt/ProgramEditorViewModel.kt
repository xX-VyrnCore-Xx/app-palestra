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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramEditorViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])

    val exerciseCatalog = workoutRepository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updateExercise(exerciseId: String, sets: Int, reps: Int, weightKg: Double?, restSeconds: Int) {
        _draftExercises.value = _draftExercises.value.map {
            if (it.exerciseId == exerciseId) {
                it.copy(targetSets = sets, targetReps = reps, targetWeightKg = weightKg, restSeconds = restSeconds)
            } else {
                it
            }
        }
    }

    fun saveProgram(name: String, totalWeeks: Int, weeklyIncrementPercent: Double, category: String?, onSaved: () -> Unit) {
        val ptId = authRepository.currentUserId.orEmpty()
        val exercises = _draftExercises.value
        viewModelScope.launch {
            workoutRepository.createProgram(
                name = name,
                createdByPtId = ptId,
                assignedToUserId = clientId,
                totalWeeks = totalWeeks,
                weeklyIncrementPercent = weeklyIncrementPercent,
                category = category,
                baseExercises = exercises.map {
                    PlanExerciseEntity(
                        id = "",
                        planId = "",
                        exerciseId = it.exerciseId,
                        orderIndex = 0,
                        targetSets = it.targetSets,
                        targetReps = it.targetReps,
                        targetWeightKg = it.targetWeightKg,
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
