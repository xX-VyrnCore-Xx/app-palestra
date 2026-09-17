package com.vyrncore.palestra.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])
    val planId: String = checkNotNull(savedStateHandle["planId"])

    val uiState = combine(
        workoutRepository.observePlanExercises(planId),
        workoutRepository.observeExercises(),
        workoutRepository.observeSetsForSession(sessionId),
    ) { planExercises, exercises, loggedSets ->
        ActiveWorkoutUiState(
            exercises = planExercises.map { planExercise ->
                ActiveExerciseUi(
                    planExerciseId = planExercise.id,
                    exerciseId = planExercise.exerciseId,
                    name = exercises.firstOrNull { it.id == planExercise.exerciseId }?.name ?: "Esercizio",
                    targetSets = planExercise.targetSets,
                    targetReps = planExercise.targetReps,
                    restSeconds = planExercise.restSeconds,
                    completedSets = loggedSets.count { it.exerciseId == planExercise.exerciseId },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveWorkoutUiState())

    fun logSet(exerciseId: String, setNumber: Int, reps: Int, weightKg: Double) {
        viewModelScope.launch {
            workoutRepository.logSet(sessionId, exerciseId, setNumber, reps, weightKg, rpe = null)
        }
    }

    fun endWorkout(onDone: () -> Unit) {
        viewModelScope.launch {
            workoutRepository.endSessionById(sessionId, notes = null)
            onDone()
        }
    }
}

data class ActiveWorkoutUiState(
    val exercises: List<ActiveExerciseUi> = emptyList(),
)

data class ActiveExerciseUi(
    val planExerciseId: String,
    val exerciseId: String,
    val name: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    val completedSets: Int,
)
