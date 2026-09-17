package com.vyrncore.palestra.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AllievoDashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    chatRepository: ChatRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserId.orEmpty()

    val ptId = authRepository.observeProfile(userId).map { it?.ptId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val unreadCount = chatRepository.observeUnreadCount(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
