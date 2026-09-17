package com.vyrncore.palestra.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs a single PT<->Allievo conversation thread. The peer is set imperatively via [setPeer]
 * rather than through SavedStateHandle nav args, so the same screen/VM can be embedded directly
 * as a bottom-nav tab (Allievo, single fixed peer = their PT) or pushed as a nav destination
 * (PT, one destination per client picked from a conversation list).
 */
@HiltViewModel
class ChatThreadViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val userId: String = authRepository.currentUserId.orEmpty()
    private val peerId = MutableStateFlow<String?>(null)

    fun setPeer(newPeerId: String) {
        if (peerId.value == newPeerId) return
        peerId.value = newPeerId
        chatRepository.setActiveConversation(newPeerId)
        viewModelScope.launch { chatRepository.markConversationRead(userId, newPeerId) }
    }

    val messages: StateFlow<List<ChatMessageEntity>> = peerId.filterNotNull()
        .flatMapLatest { peer -> chatRepository.observeConversation(userId, peer) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val peerName: StateFlow<String> = peerId.filterNotNull()
        .flatMapLatest { peer -> authRepository.observeProfile(peer) }
        .map { it?.fullName ?: "Chat" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Chat")

    fun sendMessage(content: String) {
        val peer = peerId.value ?: return
        if (content.isBlank()) return
        viewModelScope.launch { chatRepository.sendMessage(userId, peer, content) }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.setActiveConversation(null)
    }
}
