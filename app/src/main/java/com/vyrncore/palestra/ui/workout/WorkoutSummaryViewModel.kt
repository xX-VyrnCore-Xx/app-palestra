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

data class WorkoutSummaryUiState(
    val planName: String = "Allenamento",
    val durationMinutes: Long? = null,
    val setCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val fullName: String = "",
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
            _uiState.value = WorkoutSummaryUiState(
                planName = plan?.name ?: "Allenamento",
                durationMinutes = summary?.endedAtEpochMs?.let { ended -> (ended - summary.startedAtEpochMs) / 60_000 },
                setCount = summary?.setCount ?: 0,
                totalVolumeKg = summary?.totalVolumeKg ?: 0.0,
                dateEpochMs = summary?.startedAtEpochMs ?: System.currentTimeMillis(),
                fullName = profile?.fullName.orEmpty(),
            )
        }
    }
}
