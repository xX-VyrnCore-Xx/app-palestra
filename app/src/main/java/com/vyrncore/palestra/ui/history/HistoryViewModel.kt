package com.vyrncore.palestra.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryItemUi(
    val sessionId: String,
    val planName: String,
    val startedAtEpochMs: Long,
    val durationMinutes: Long?,
    val setCount: Int,
    val totalVolumeKg: Double,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserId.orEmpty()

    val history = combine(
        workoutRepository.observeSessionSummaries(userId),
        workoutRepository.observePlansForUser(userId),
    ) { summaries, plans ->
        summaries.filter { it.endedAtEpochMs != null }.map { summary ->
            HistoryItemUi(
                sessionId = summary.sessionId,
                planName = plans.firstOrNull { it.id == summary.planId }?.name ?: "Allenamento libero",
                startedAtEpochMs = summary.startedAtEpochMs,
                durationMinutes = summary.endedAtEpochMs?.let { (it - summary.startedAtEpochMs) / 60_000 },
                setCount = summary.setCount,
                totalVolumeKg = summary.totalVolumeKg,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
