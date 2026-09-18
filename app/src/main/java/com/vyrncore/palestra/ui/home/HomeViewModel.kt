package com.vyrncore.palestra.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WeeklyRankingEntry
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.data.sync.ConnectivityObserver
import com.vyrncore.palestra.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** Total completed workouts that unlock a separate milestone badge, independent of streaks. */
val WORKOUT_COUNT_MILESTONES = listOf(5, 10, 25, 50, 100, 250)

/** How many workouts count as a "full" week for the weekly goal ring. */
const val WEEKLY_GOAL = 3

/** XP awarded per completed workout and per badge unlocked; levels are 100 XP apart. */
private const val XP_PER_SESSION = 30
private const val XP_PER_BADGE = 20
private const val XP_PER_LEVEL = 100

/** Total kg lifted (sum of weight*reps across every set) that unlock a badge. */
val VOLUME_MILESTONES_KG = listOf(1_000, 5_000, 10_000, 25_000, 50_000, 100_000)

/** Cosmetic title shown next to the level number, purely for flavor. */
fun levelTitle(level: Int): String = when {
    level < 3 -> "Novizio"
    level < 6 -> "Allievo"
    level < 11 -> "Atleta"
    level < 21 -> "Veterano"
    else -> "Leggenda"
}

data class HomeUiState(
    val fullName: String = "",
    val streakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val workoutsThisWeek: Int = 0,
    val totalWorkouts: Int = 0,
    val unlockedBadges: List<Int> = emptyList(),
    val unlockedWorkoutCountBadges: List<Int> = emptyList(),
    val unlockedVolumeBadges: List<Int> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val xp: Int = 0,
    val level: Int = 1,
    val levelTitle: String = "Novizio",
    val xpIntoLevel: Int = 0,
    val nextPlanId: String? = null,
    val nextPlanName: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

    val isOnline = connectivityObserver.isOnline
    val isSyncing = syncManager.isSyncing

    private val _weeklyRanking = MutableStateFlow<List<WeeklyRankingEntry>>(emptyList())
    val weeklyRanking: StateFlow<List<WeeklyRankingEntry>> = _weeklyRanking.asStateFlow()

    init {
        viewModelScope.launch { _weeklyRanking.value = workoutRepository.fetchWeeklyRanking() }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { syncManager.syncAll() }
            _weeklyRanking.value = workoutRepository.fetchWeeklyRanking()
        }
    }

    val uiState = combine(
        workoutRepository.observeSessionsForUser(userId),
        workoutRepository.observePlansForUser(userId),
        authRepository.observeProfile(userId),
        workoutRepository.observeVolumeByMuscleGroup(userId),
    ) { sessions, plans, profile, volumeByMuscle ->
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
        val totalWorkouts = sessions.count { it.endedAtEpochMs != null }

        val totalVolumeKg = volumeByMuscle.sumOf { it.totalVolumeKg }

        val unlockedBadges = BADGE_MILESTONES.filter { longestStreak >= it }
        val unlockedWorkoutCountBadges = WORKOUT_COUNT_MILESTONES.filter { totalWorkouts >= it }
        val unlockedVolumeBadges = VOLUME_MILESTONES_KG.filter { totalVolumeKg >= it }
        val badgeCount = unlockedBadges.size + unlockedWorkoutCountBadges.size + unlockedVolumeBadges.size
        val xp = totalWorkouts * XP_PER_SESSION + badgeCount * XP_PER_BADGE
        val level = 1 + xp / XP_PER_LEVEL

        val nextPlan = plans.firstOrNull()
        HomeUiState(
            fullName = profile?.fullName.orEmpty(),
            streakDays = streak,
            longestStreakDays = longestStreak,
            workoutsThisWeek = workoutsThisWeek,
            totalWorkouts = totalWorkouts,
            unlockedBadges = unlockedBadges,
            unlockedWorkoutCountBadges = unlockedWorkoutCountBadges,
            unlockedVolumeBadges = unlockedVolumeBadges,
            totalVolumeKg = totalVolumeKg,
            xp = xp,
            level = level,
            levelTitle = levelTitle(level),
            xpIntoLevel = xp % XP_PER_LEVEL,
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
