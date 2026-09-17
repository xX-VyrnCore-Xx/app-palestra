package com.vyrncore.palestra.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val fullName: String = "",
    val streakDays: Int = 0,
    val workoutsThisWeek: Int = 0,
    val nextPlanId: String? = null,
    val nextPlanName: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

    val uiState = combine(
        workoutRepository.observeSessionsForUser(userId),
        workoutRepository.observePlansForUser(userId),
        authRepository.observeProfile(userId),
    ) { sessions, plans, profile ->
        val zone = ZoneId.systemDefault()
        val doneDates = sessions.mapNotNull { it.endedAtEpochMs }
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toSet()

        var streak = 0
        var day = LocalDate.now(zone)
        while (doneDates.contains(day)) {
            streak++
            day = day.minusDays(1)
        }

        val weekAgo = LocalDate.now(zone).minusDays(7)
        val workoutsThisWeek = doneDates.count { it.isAfter(weekAgo) }

        val nextPlan = plans.firstOrNull()
        HomeUiState(
            fullName = profile?.fullName.orEmpty(),
            streakDays = streak,
            workoutsThisWeek = workoutsThisWeek,
            nextPlanId = nextPlan?.id,
            nextPlanName = nextPlan?.name,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun startWorkout(planId: String, onStarted: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            val sessionId = workoutRepository.startSession(userId, planId)
            onStarted(sessionId)
        }
    }
}
