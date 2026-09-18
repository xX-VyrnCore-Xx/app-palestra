package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.data.sync.ConnectivityObserver
import com.vyrncore.palestra.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
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

@HiltViewModel
class PtDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    chatRepository: ChatRepository,
    private val syncManager: SyncManager,
    connectivityObserver: ConnectivityObserver,
    workoutRepository: WorkoutRepository,
) : ViewModel() {

    val ptId: String get() = authRepository.currentUserId.orEmpty()

    val clients = authRepository.observeClients(ptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount = chatRepository.observeUnreadCount(ptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isOnline = connectivityObserver.isOnline
    val isSyncing = syncManager.isSyncing

    /** Top clients by workouts completed in the last 7 days — a small motivational nudge for the
     * PT (who they may want to nudge) and a preview of what a future allievo-facing leaderboard
     * could look like, built entirely from data the PT is already authorized to see. */
    val weeklyRanking = clients
        .flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    list.map { client ->
                        workoutRepository.observeSessionsForUser(client.id).map { sessionList ->
                            val weekAgo = LocalDate.now(ZoneId.systemDefault()).minusDays(7)
                            val count = sessionList.count { session ->
                                val endedAt = session.endedAtEpochMs ?: return@count false
                                Instant.ofEpochMilli(endedAt).atZone(ZoneId.systemDefault()).toLocalDate().isAfter(weekAgo)
                            }
                            ClientRanking(client.id, client.fullName, count)
                        }
                    },
                ) { rankings -> rankings.toList().filter { it.workoutsThisWeek > 0 }.sortedByDescending { it.workoutsThisWeek } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        viewModelScope.launch { runCatching { syncManager.syncAll() } }
    }
}
