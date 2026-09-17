package com.vyrncore.palestra.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.dao.MuscleGroupVolume
import com.vyrncore.palestra.data.local.dao.WeeklyVolume
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ExerciseProgressPoint(val epochMs: Long, val maxWeightKg: Double, val volumeKg: Double)

data class StatsUiState(
    val exerciseOptions: List<Pair<String, String>> = emptyList(), // id to name
    val selectedExerciseId: String? = null,
    val history: List<ExerciseProgressPoint> = emptyList(),
    val personalRecordKg: Double = 0.0,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserId.orEmpty()
    private val selectedExerciseId = MutableStateFlow<String?>(null)

    val advancedStats: StateFlow<Pair<List<MuscleGroupVolume>, List<WeeklyVolume>>> = combine(
        workoutRepository.observeVolumeByMuscleGroup(userId),
        workoutRepository.observeWeeklyVolume(userId),
    ) { byMuscle, weekly -> byMuscle to weekly }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<MuscleGroupVolume>() to emptyList())

    val uiState: StateFlow<StatsUiState> = combine(
        workoutRepository.observeExercises(),
        selectedExerciseId,
    ) { exercises, selectedId -> exercises to (selectedId ?: exercises.firstOrNull()?.id) }
        .flatMapLatest { (exercises, selectedId) ->
            val historyFlow = if (selectedId != null) {
                workoutRepository.observeHistoryForExercise(selectedId)
            } else {
                flowOf(emptyList())
            }
            historyFlow.combine(flowOf(exercises)) { sets, exs ->
                val points = sets
                    .groupBy { it.completedAtEpochMs / 86_400_000L } // bucket by day
                    .map { (_, daySets) ->
                        ExerciseProgressPoint(
                            epochMs = daySets.first().completedAtEpochMs,
                            maxWeightKg = daySets.maxOf { it.weightKg },
                            volumeKg = daySets.sumOf { it.weightKg * it.reps },
                        )
                    }
                    .sortedBy { it.epochMs }
                StatsUiState(
                    exerciseOptions = exs.map { it.id to it.name },
                    selectedExerciseId = selectedId,
                    history = points,
                    personalRecordKg = points.maxOfOrNull { it.maxWeightKg } ?: 0.0,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun selectExercise(exerciseId: String) {
        selectedExerciseId.value = exerciseId
    }
}
