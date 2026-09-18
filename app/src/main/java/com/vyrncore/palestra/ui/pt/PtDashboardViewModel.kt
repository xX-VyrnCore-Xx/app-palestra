package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import com.vyrncore.palestra.data.sync.ConnectivityObserver
import com.vyrncore.palestra.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PtDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    chatRepository: ChatRepository,
    private val syncManager: SyncManager,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val ptId: String get() = authRepository.currentUserId.orEmpty()

    val clients = authRepository.observeClients(ptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount = chatRepository.observeUnreadCount(ptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isOnline = connectivityObserver.isOnline
    val isSyncing = syncManager.isSyncing

    fun refresh() {
        viewModelScope.launch { runCatching { syncManager.syncAll() } }
    }
}
