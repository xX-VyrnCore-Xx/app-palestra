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

/** Streak lengths (in days) that unlock a badge. Unlocking is permanent: it's based on the
 * longest streak ever reached, not the current one, so a rest day never takes a badge away. */
val BADGE_MILESTONES = listOf(3, 7, 14, 30, 60, 100)

data class HomeUiState(
    val fullName: String = "",
    val streakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val workoutsThisWeek: Int = 0,
    val unlockedBadges: List<Int> = emptyList(),
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
            .toSortedSet()

        var streak = 0
        var day = LocalDate.now(zone)
        while (doneDates.contains(day)) {
            streak++
            day = day.minusDays(1)
        }

        var longestStreak = 0
        var runLength = 0
        var previousDay: LocalDate? = null
        for (date in doneDates) {
            runLength = if (previousDay != null && date == previousDay.plusDays(1)) runLength + 1 else 1
            longestStreak = maxOf(longestStreak, runLength)
            previousDay = date
        }

        val weekAgo = LocalDate.now(zone).minusDays(7)
        val workoutsThisWeek = doneDates.count { it.isAfter(weekAgo) }

        val nextPlan = plans.firstOrNull()
        HomeUiState(
            fullName = profile?.fullName.orEmpty(),
            streakDays = streak,
            longestStreakDays = longestStreak,
            workoutsThisWeek = workoutsThisWeek,
            unlockedBadges = BADGE_MILESTONES.filter { longestStreak >= it },
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
