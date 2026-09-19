package com.vyrncore.palestra.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.notification.NotificationHelper
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.PlotoneFeedRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val plotoneFeedRepository: PlotoneFeedRepository,
    private val notificationHelper: NotificationHelper,
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
                    imageUrl = exercises.firstOrNull { it.id == planExercise.exerciseId }?.imageUrl,
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
            val isNewRecord = workoutRepository.logSet(sessionId, exerciseId, setNumber, reps, weightKg, rpe = null)
            if (isNewRecord) {
                val exerciseName = uiState.value.exercises.firstOrNull { it.exerciseId == exerciseId }?.name ?: return@launch
                val estimatedOneRepMaxKg = weightKg * (1 + reps / 30.0)
                notificationHelper.showPersonalRecordNotification(exerciseName, estimatedOneRepMaxKg)
            }
        }
    }

    fun endWorkout(onDone: () -> Unit) {
        viewModelScope.launch {
            workoutRepository.endSessionById(sessionId, notes = null)
            postToPlotoneFeed()
            onDone()
        }
    }

    /** Best-effort share to the plotone feed: a light social nudge for the other allievi of the
     * same PT, never something that should block or fail the workout ending. */
    private suspend fun postToPlotoneFeed() {
        val userId = authRepository.currentUserId ?: return
        val profile = authRepository.observeProfile(userId).first() ?: return
        val ptId = profile.ptId ?: return
        val exerciseCount = uiState.value.exercises.count { it.completedSets > 0 }
        if (exerciseCount == 0) return
        plotoneFeedRepository.postWorkoutCompleted(userId, ptId, profile.fullName, exerciseCount)
    }
}

data class ActiveWorkoutUiState(
    val exercises: List<ActiveExerciseUi> = emptyList(),
)

data class ActiveExerciseUi(
    val planExerciseId: String,
    val exerciseId: String,
    val name: String,
    val imageUrl: String? = null,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    val completedSets: Int,
)
