package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import com.vyrncore.palestra.data.repository.PlotoneFeedPost
import com.vyrncore.palestra.data.repository.PlotoneFeedRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.data.sync.ConnectivityObserver
import com.vyrncore.palestra.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ClientRanking(val clientId: String, val fullName: String, val workoutsThisWeek: Int)

enum class ClientSortMode { LAST_ACTIVE, NAME }

/** One client's plan/program library, for the PT-side "Schede" tab. */
data class ClientPlanOverview(
    val clientId: String,
    val fullName: String,
    val planCount: Int,
    val activeProgramName: String?,
)

/** Everything the PT dashboard needs to show about one client at a glance, computed client-side
 * from data the PT is already authorized to see (their own roster + those clients' sessions). */
data class ClientOverview(
    val clientId: String,
    val fullName: String,
    val email: String,
    val injuries: String?,
    val workoutsThisWeek: Int,
    val totalWorkouts: Int,
    val lastActiveEpochMs: Long?,
    val unreadFromClient: Int,
)

@HiltViewModel
class PtDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val syncManager: SyncManager,
    connectivityObserver: ConnectivityObserver,
    private val workoutRepository: WorkoutRepository,
    private val plotoneFeedRepository: PlotoneFeedRepository,
) : ViewModel() {

    val ptId: String get() = authRepository.currentUserId.orEmpty()

    private val _feed = MutableStateFlow<List<PlotoneFeedPost>>(emptyList())
    val feed: StateFlow<List<PlotoneFeedPost>> = _feed.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(ClientSortMode.LAST_ACTIVE)
    val sortMode: StateFlow<ClientSortMode> = _sortMode.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: ClientSortMode) {
        _sortMode.value = mode
    }

    private val _inviteCode = MutableStateFlow<String?>(null)

    /** The PT's shareable code, generating one the first time it's needed - the dashboard header
     * used to show a truncated raw user id here, which stopped working the moment registration
     * switched to the 6-character invite code instead of a pasted UUID. */
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    init {
        viewModelScope.launch { _feed.value = plotoneFeedRepository.fetchFeed(ptId) }
        viewModelScope.launch { _inviteCode.value = authRepository.getOrCreateInviteCode(ptId) }
    }

    val clients = authRepository.observeClients(ptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount = chatRepository.observeUnreadCount(ptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isOnline = connectivityObserver.isOnline
    val isSyncing = syncManager.isSyncing

    /** Per-client detail (activity, workload, unread messages), recomputed live as sessions/chat
     * come in — the data source for both the roster list and the summary stat row above it. */
    private val clientOverviews: StateFlow<List<ClientOverview>> = clients
        .flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    list.map { client ->
                        combine(
                            workoutRepository.observeSessionsForUser(client.id),
                            chatRepository.observeUnreadCountFromSender(ptId, client.id),
                        ) { sessions, unread ->
                            val weekAgo = LocalDate.now(ZoneId.systemDefault()).minusDays(7)
                            val completed = sessions.mapNotNull { it.endedAtEpochMs }
                            val workoutsThisWeek = completed.count {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().isAfter(weekAgo)
                            }
                            ClientOverview(
                                clientId = client.id,
                                fullName = client.fullName,
                                email = client.email,
                                injuries = client.injuries,
                                workoutsThisWeek = workoutsThisWeek,
                                totalWorkouts = completed.size,
                                lastActiveEpochMs = completed.maxOrNull(),
                                unreadFromClient = unread,
                            )
                        }
                    },
                ) { it.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleClients: StateFlow<List<ClientOverview>> = combine(
        clientOverviews, _searchQuery, _sortMode,
    ) { overviews, query, sort ->
        overviews
            .filter { it.fullName.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) }
            .let { filtered ->
                when (sort) {
                    ClientSortMode.NAME -> filtered.sortedBy { it.fullName.lowercase() }
                    ClientSortMode.LAST_ACTIVE -> filtered.sortedWith(
                        compareByDescending<ClientOverview> { it.lastActiveEpochMs ?: -1 },
                    )
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Roster-wide counts shown as the dashboard's top stat row. */
    val activeThisWeekCount: StateFlow<Int> = clientOverviews
        .map { it.count { c -> c.workoutsThisWeek > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val inactiveCount: StateFlow<Int> = clientOverviews
        .map { overviews ->
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
            overviews.count { (it.lastActiveEpochMs ?: 0) < weekAgo }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Top clients by workouts completed in the last 7 days — a small motivational nudge for the
     * PT (who they may want to nudge) and a preview of what a future allievo-facing leaderboard
     * could look like, built entirely from data the PT is already authorized to see. */
    val weeklyRanking: StateFlow<List<ClientRanking>> = clientOverviews
        .map { overviews ->
            overviews.filter { it.workoutsThisWeek > 0 }
                .sortedByDescending { it.workoutsThisWeek }
                .map { ClientRanking(it.clientId, it.fullName, it.workoutsThisWeek) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Every client's plan/program library in one place, for the "Schede" tab - built the same way
     * as [clientOverviews], by combining a per-client flow the PT is already authorized to read. */
    val clientPlanOverviews: StateFlow<List<ClientPlanOverview>> = clients
        .flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    list.map { client ->
                        combine(
                            workoutRepository.observePlansForUser(client.id),
                            workoutRepository.observeProgramsForUser(client.id),
                        ) { plans, programs ->
                            ClientPlanOverview(
                                clientId = client.id,
                                fullName = client.fullName,
                                planCount = plans.size,
                                activeProgramName = programs.maxByOrNull { it.startEpochMs }?.name,
                            )
                        }
                    },
                ) { it.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        viewModelScope.launch {
            runCatching { syncManager.syncAll() }
            _feed.value = plotoneFeedRepository.fetchFeed(ptId)
        }
    }
}
