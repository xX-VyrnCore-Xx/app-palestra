package com.vyrncore.palestra.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.dao.MuscleGroupVolume
import com.vyrncore.palestra.data.local.dao.PersonalRecord
import com.vyrncore.palestra.data.local.dao.WeeklyVolume
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.PlotoneFeedPost
import com.vyrncore.palestra.data.repository.PlotoneFeedRepository
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
import kotlinx.coroutines.flow.first
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

/** Real Italian Army rank hierarchy (Esercito Italiano), one promotion per level: truppa ->
 * graduati -> sottufficiali -> ufficiali inferiori -> ufficiali superiori -> ufficiali generali.
 * The app's gamification is framed as a career of service; each level climbed is a real promotion. */
/** Gym-themed progression (Principiante -> Intermedio -> Avanzato -> Elite), each macro-tier
 * split into a few steps so leveling still feels granular over a long training history. */
private val GYM_LEVELS = listOf(
    "Principiante", // 1
    "Principiante Assiduo", // 2
    "Allievo", // 3
    "Allievo Costante", // 4
    "Atleta in Erba", // 5
    "Atleta", // 6
    "Atleta Esperto", // 7
    "Intermedio", // 8
    "Intermedio Solido", // 9
    "Intermedio Avanzato", // 10
    "Veterano", // 11
    "Veterano Esperto", // 12
    "Specialista", // 13
    "Specialista Esperto", // 14
    "Avanzato", // 15
    "Avanzato Esperto", // 16
    "Elite Junior", // 17
    "Elite", // 18
    "Elite Esperto", // 19
    "Campione", // 20
    "Campione Esperto", // 21
    "Maestro", // 22
    "Maestro Esperto", // 23
    "Leggenda", // 24
    "Leggenda Assoluta", // 25+
)

fun levelTitle(level: Int): String = GYM_LEVELS.getOrElse(level - 1) { GYM_LEVELS.last() }

/** Number of stars on the level badge — climbs with tier, caps at 5 for Leggenda. */
fun rankStars(level: Int): Int = when {
    level < 3 -> 0 // Principiante
    level < 7 -> 1 // Allievo/Atleta
    level < 16 -> 2 // Intermedio/Veterano/Specialista
    level < 19 -> 3 // Avanzato/Elite Junior
    level < 22 -> 4 // Elite/Campione
    else -> 5 // Maestro/Leggenda
}

/** One tappable "ordine del giorno" on the Home screen: a context-aware nudge computed from the
 * allievo's real data (streak, weekly goal, next badge, next promotion) paired with the action to
 * run when the card is tapped. The UI renders at most [MAX_HOME_SUGGESTIONS] of these. */
data class HomeSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val action: HomeSuggestionAction,
)

enum class HomeSuggestionAction { StartWorkout, Assistant, ChatPt, History }

/** Cap on the tappable suggestion chips shown in Home's "Ordini del giorno" section. */
const val MAX_HOME_SUGGESTIONS = 3

/** Ranking of all possible suggestions, most pressing first; the builder walks it and stops at the cap.
 * Each suggestion is only produced when the underlying condition (computed from real data) holds. */
private fun buildHomeSuggestions(state: HomeUiState): List<HomeSuggestion> = buildList {
    // 1. Just go train: always the top priority whenever there is anything to run.
    if (state.nextPlanId != null) {
        add(
            HomeSuggestion(
                id = "start_workout",
                title = "Devi ancora allenarti questa settimana",
                description = "La scheda “${state.nextPlanName.orEmpty()}” ti aspetta: esercizi e carichi sono già pronti.",
                action = HomeSuggestionAction.StartWorkout,
            ),
        )
    }
    // 2. Weekly goal reached: celebrate and warn about the streak you'd be risking.
    if (state.workoutsThisWeek >= WEEKLY_GOAL) {
        add(
            HomeSuggestion(
                id = "goal_reached",
                title = "Obiettivo settimanale centrato!",
                description = "${state.workoutsThisWeek} allenamenti completati: ogni giorno extra vale XP e tiene viva la streak di ${state.streakDays} giorni.",
                action = HomeSuggestionAction.History,
            ),
        )
    }
    // 3. Quiet week: a nudge, not a scolding — the PT chat is the fastest way to get unblocked.
    if (state.workoutsThisWeek < WEEKLY_GOAL && state.nextPlanId != null) {
        add(
            HomeSuggestion(
                id = "behind_weekly_goal",
                title = "La settimana è ancora in sospeso",
                description = "Ti mancano ${WEEKLY_GOAL - state.workoutsThisWeek} allenamenti all'obiettivo: anche una sessione breve vale più di zero.",
                action = HomeSuggestionAction.StartWorkout,
            ),
        )
    }
    // 4. One workout away from the next streak badge: the closest win on the board.
    BADGE_MILESTONES.firstOrNull { it > state.longestStreakDays }?.let { next ->
        add(
            HomeSuggestion(
                id = "next_streak_badge",
                title = "A $next giorni di streak apre la prossima medaglia",
                description = "Sei a ${next - state.longestStreakDays} giorni di distanza: aggiorna la scheda dal profilo per includere oggi.",
                action = HomeSuggestionAction.History,
            ),
        )
    }
    // 5. Total volume closing in on the next milestone badge.
    VOLUME_MILESTONES_KG.firstOrNull { it > state.totalVolumeKg }?.let { next ->
        add(
            HomeSuggestion(
                id = "next_volume_badge",
                title = "Mancano ${formatKgToNextMilestone(next - state.totalVolumeKg)} alla prossima medaglia",
                description = "Hai sollevato ${state.totalVolumeKg.toInt()} kg in totale: la soglia successiva è $next kg.",
                action = HomeSuggestionAction.History,
            ),
        )
    }
    // 6. Promotion in sight: XP from one more session is usually enough to level up.
    val xpToPromotion = XP_PER_LEVEL - state.xpIntoLevel
    if (xpToPromotion <= XP_PER_SESSION) {
        add(
            HomeSuggestion(
                id = "promotion_in_sight",
                title = "A un passo dal livello successivo",
                description = "Ti mancano $xpToPromotion XP per il livello ${levelTitle(state.level + 1)}: un allenamento e ci sei.",
                action = HomeSuggestionAction.StartWorkout,
            ),
        )
    }
    // 7. Nothing assigned at all: point at the two people who can fix that.
    if (state.nextPlanId == null) {
        add(
            HomeSuggestion(
                id = "no_plan",
                title = "Nessuna scheda assegnata",
                description = "Scrivi al tuo PT per farti assegnare una scheda, o chiedi all'assistente come strutturarti intanto.",
                action = HomeSuggestionAction.ChatPt,
            ),
        )
    }
    // 8. Pure idle fall-back so the section is never an empty void.
    if (isEmpty()) {
        add(
            HomeSuggestion(
                id = "assistant_generic",
                title = "Non sai da dove ripartire?",
                description = "L'assistente analizza i tuoi progressi e ti propone la mossa successiva.",
                action = HomeSuggestionAction.Assistant,
            ),
        )
    }
}

/** Renders a kg delta for the volume-badge suggestion without drowning the label in decimals:
 * sub-1kg deltas still read as "meno di 1 kg". */
private fun formatKgToNextMilestone(kg: Double): String {
    val rounded = kotlin.math.ceil(kg).toInt()
    return if (rounded < 1) "meno di 1 kg" else "$rounded kg"
}

data class HomeUiState(
    val fullName: String = "",
    val streakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val workoutsThisWeek: Int = 0,
    val workoutsLastWeek: Int = 0,
    val totalWorkouts: Int = 0,
    val unlockedBadges: List<Int> = emptyList(),
    val unlockedWorkoutCountBadges: List<Int> = emptyList(),
    val unlockedVolumeBadges: List<Int> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val xp: Int = 0,
    val level: Int = 1,
    val levelTitle: String = "Principiante",
    val xpIntoLevel: Int = 0,
    val nextPlanId: String? = null,
    val nextPlanName: String? = null,
    val activeProgramName: String? = null,
    val activeProgramCurrentWeek: Int? = null,
    val activeProgramTotalWeeks: Int? = null,
    /** Ids of plans whose latest session is still open, keyed for "riprendi l'allenamento" actions. */
    val sessionsWithPending: Map<String, String> = emptyMap(),
    /** Context-aware tappable nudges, computed from the same stats the rest of Home displays. */
    val suggestions: List<HomeSuggestion> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val plotoneFeedRepository: PlotoneFeedRepository,
    private val syncManager: SyncManager,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

    val isOnline = connectivityObserver.isOnline
    val isSyncing = syncManager.isSyncing

    private val _weeklyRanking = MutableStateFlow<List<WeeklyRankingEntry>>(emptyList())
    val weeklyRanking: StateFlow<List<WeeklyRankingEntry>> = _weeklyRanking.asStateFlow()

    private val _feed = MutableStateFlow<List<PlotoneFeedPost>>(emptyList())
    val feed: StateFlow<List<PlotoneFeedPost>> = _feed.asStateFlow()

    /** Advanced progress charts, folded directly into Home instead of a separate tab. */
    val weeklyVolume: StateFlow<List<WeeklyVolume>> = workoutRepository.observeWeeklyVolume(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val volumeByMuscleGroup: StateFlow<List<MuscleGroupVolume>> = workoutRepository.observeVolumeByMuscleGroup(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalRecords: StateFlow<List<PersonalRecord>> = workoutRepository.observePersonalRecords(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { _weeklyRanking.value = workoutRepository.fetchWeeklyRanking() }
        viewModelScope.launch { refreshFeed() }
    }

    private suspend fun refreshFeed() {
        val ptId = authRepository.observeProfile(userId).first()?.ptId ?: return
        _feed.value = plotoneFeedRepository.fetchFeed(ptId)
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { syncManager.syncAll() }
            _weeklyRanking.value = workoutRepository.fetchWeeklyRanking()
            refreshFeed()
        }
    }

    val uiState = combine(
        workoutRepository.observeSessionsForUser(userId),
        workoutRepository.observePlansForUser(userId),
        authRepository.observeProfile(userId),
        workoutRepository.observeVolumeByMuscleGroup(userId),
        workoutRepository.observeProgramsForUser(userId),
    ) { sessions, plans, profile, volumeByMuscle, programs ->
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
        val twoWeeksAgo = LocalDate.now(zone).minusDays(14)
        val workoutsThisWeek = doneDates.count { it.isAfter(weekAgo) }
        val workoutsLastWeek = doneDates.count { it.isAfter(twoWeeksAgo) && !it.isAfter(weekAgo) }
        val totalWorkouts = sessions.count { it.endedAtEpochMs != null }

        val totalVolumeKg = volumeByMuscle.sumOf { it.totalVolumeKg }

        val unlockedBadges = BADGE_MILESTONES.filter { longestStreak >= it }
        val unlockedWorkoutCountBadges = WORKOUT_COUNT_MILESTONES.filter { totalWorkouts >= it }
        val unlockedVolumeBadges = VOLUME_MILESTONES_KG.filter { totalVolumeKg >= it }
        val badgeCount = unlockedBadges.size + unlockedWorkoutCountBadges.size + unlockedVolumeBadges.size
        val xp = totalWorkouts * XP_PER_SESSION + badgeCount * XP_PER_BADGE
        val level = 1 + xp / XP_PER_LEVEL

        val activeProgram = programs.maxByOrNull { it.startEpochMs }
        val activeProgramCurrentWeek = activeProgram?.let {
            val elapsedWeeks = (System.currentTimeMillis() - it.startEpochMs) / (7L * 24 * 3600 * 1000)
            (elapsedWeeks.toInt() + 1).coerceIn(1, it.totalWeeks)
        }
        val programPlan = activeProgram?.let { program ->
            plans.firstOrNull { it.programId == program.id && it.weekIndex == activeProgramCurrentWeek }
        }
        val nextPlan = programPlan ?: plans.firstOrNull { it.programId == null }

        val sessionsWithPending = sessions.filter { it.endedAtEpochMs == null && it.planId != null }
            .associate { it.planId!! to it.id }

        val baseState = HomeUiState(
            fullName = profile?.fullName.orEmpty(),
            streakDays = streak,
            longestStreakDays = longestStreak,
            workoutsThisWeek = workoutsThisWeek,
            workoutsLastWeek = workoutsLastWeek,
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
            activeProgramName = activeProgram?.name,
            activeProgramCurrentWeek = activeProgramCurrentWeek,
            activeProgramTotalWeeks = activeProgram?.totalWeeks,
        )
        baseState.copy(
            sessionsWithPending = sessionsWithPending,
            suggestions = buildHomeSuggestions(baseState).take(MAX_HOME_SUGGESTIONS),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun startWorkout(planId: String, onStarted: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            val sessionId = workoutRepository.startSession(userId, planId)
            onStarted(sessionId)
        }
    }
}
