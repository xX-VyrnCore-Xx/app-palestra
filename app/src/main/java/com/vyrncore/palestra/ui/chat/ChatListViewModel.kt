package com.vyrncore.palestra.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChatConversationUi(
    val peerId: String,
    val peerName: String,
    val lastMessage: String?,
    val unreadCount: Int,
)

/** One conversation row per client, for the PT's chat list. */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    chatRepository: ChatRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val ptId = authRepository.currentUserId.orEmpty()

    val conversations: StateFlow<List<ChatConversationUi>> = authRepository.observeClients(ptId)
        .flatMapLatest { clients ->
            if (clients.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    clients.map { client ->
                        combine(
                            chatRepository.observeLastMessage(ptId, client.id),
                            chatRepository.observeUnreadCountFromSender(ptId, client.id),
                        ) { last, unread ->
                            ChatConversationUi(client.id, client.fullName, last?.content, unread)
                        }
                    },
                ) { it.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
