package com.vyrncore.palestra.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummaryExerciseVolume(
    val exerciseName: String,
    val volumeKg: Double,
    val setCount: Int,
)

data class SummaryPrEntry(
    val exerciseName: String,
    val sessionE1rmKg: Double,
    /** Best e1RM across all previous sessions; null when this is the first time logging it. */
    val previousBestKg: Double?,
) {
    /** Positive when this session beat the previous best; null when there was no previous best. */
    val deltaKg: Double? = previousBestKg?.let { sessionE1rmKg - it }
}

data class WorkoutSummaryUiState(
    val planName: String = "Allenamento",
    val durationMinutes: Long? = null,
    val setCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val fullName: String = "",
    /** Volume per exercise, biggest first - the summary's bar chart. */
    val volumeByExercise: List<SummaryExerciseVolume> = emptyList(),
    /** Exercises where this session's best e1RM can be compared with the all-time best. */
    val prComparison: List<SummaryPrEntry> = emptyList(),
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])
    val planId: String = checkNotNull(savedStateHandle["planId"])

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = authRepository.currentUserId.orEmpty()
            val plan = workoutRepository.getPlanById(planId)
            val profile = authRepository.observeProfile(userId).first()
            val summary = workoutRepository.observeSessionSummaries(userId).first()
                .firstOrNull { it.sessionId == sessionId }
            val sets = workoutRepository.observeSetsForSession(sessionId).first()
            val planExercises = workoutRepository.observePlanExercises(planId).first()

            // Volume per exercise: group this session's sets by exercise, name resolved via
            // the plan → exercise catalog, plus how many sets were logged per exercise.
            val exerciseIdToName = planExercises.associate { pe ->
                val name = workoutRepository.getExerciseById(pe.exerciseId)?.name
                pe.exerciseId to (name ?: "Esercizio")
            }
            val byExercise = sets.groupBy { it.exerciseId }
            val volumeByExercise = byExercise.map { (exerciseId, exerciseSets) ->
                SummaryExerciseVolume(
                    exerciseName = exerciseIdToName[exerciseId] ?: "Esercizio",
                    volumeKg = exerciseSets.sumOf { it.weightKg * it.reps },
                    setCount = exerciseSets.size,
                )
            }.sortedByDescending { it.volumeKg }

            // PR comparison: for every exercise trained today, this session's best e1RM
            // (Epley) against the best across all the user's OTHER sessions.
            val prComparison = byExercise.keys.mapNotNull { exerciseId ->
                val name = exerciseIdToName[exerciseId] ?: return@mapNotNull null
                val sessionE1rm = sets.filter { it.exerciseId == exerciseId }
                    .maxOf { it.weightKg * (1 + it.reps / 30.0) }
                val previousBest = workoutRepository.bestPreviousE1rm(sessionId, exerciseId)
                SummaryPrEntry(name, sessionE1rm, previousBest)
            }.sortedWith(compareByDescending<SummaryPrEntry> { it.deltaKg ?: Double.MIN_VALUE }.thenByDescending { it.sessionE1rmKg })

            _uiState.value = WorkoutSummaryUiState(
                planName = plan?.name ?: "Allenamento",
                durationMinutes = summary?.endedAtEpochMs?.let { ended -> (ended - summary.startedAtEpochMs) / 60_000 }
                    ?: ((sets.maxOfOrNull { it.completedAtEpochMs } ?: System.currentTimeMillis() - (summary?.startedAtEpochMs ?: System.currentTimeMillis())) / 60_000)
                        .takeIf { sets.isNotEmpty() && summary?.startedAtEpochMs != null },
                setCount = sets.size,
                totalVolumeKg = sets.sumOf { it.weightKg * it.reps },
                dateEpochMs = summary?.startedAtEpochMs ?: System.currentTimeMillis(),
                fullName = profile?.fullName.orEmpty(),
                volumeByExercise = volumeByExercise,
                prComparison = prComparison,
            )
        }
    }
}
