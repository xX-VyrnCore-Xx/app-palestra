package com.vyrncore.palestra.ui.chat

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.entity.ChatAttachmentType
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    @ApplicationContext private val context: android.content.Context,
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

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { chatRepository.deleteMessage(messageId, userId) }
    }

    fun sendAttachment(uri: Uri) {
        val peer = peerId.value ?: return
        viewModelScope.launch {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri).orEmpty()
            val fileName = queryFileName(resolver, uri) ?: "file"
            val bytes = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch
            val type = if (mimeType.startsWith("image/")) ChatAttachmentType.IMAGE else ChatAttachmentType.FILE
            chatRepository.sendAttachment(userId, peer, fileName, bytes, type)
        }
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.setActiveConversation(null)
    }
}
