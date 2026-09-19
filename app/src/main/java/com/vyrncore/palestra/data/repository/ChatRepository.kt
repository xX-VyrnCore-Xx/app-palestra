package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.ChatMessageDao
import com.vyrncore.palestra.data.local.dao.UserProfileDao
import com.vyrncore.palestra.data.local.entity.ChatAttachmentType
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.data.notification.NotificationHelper
import com.vyrncore.palestra.data.remote.dto.ChatMessageDto
import com.vyrncore.palestra.data.remote.toDto
import com.vyrncore.palestra.data.remote.toEntity
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SendPushRequest(
    val recipientId: String,
    val type: String,
    val title: String,
    val body: String,
)

/**
 * Realtime PT<->Allievo messaging. Sent/received messages are always mirrored into Room so the
 * conversation is readable offline; new messages arrive live via a Supabase Realtime channel
 * rather than waiting for [SyncManager]'s poll cycle.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val realtime: Realtime,
    private val postgrest: Postgrest,
    private val storage: Storage,
    private val functions: Functions,
    private val chatMessageDao: ChatMessageDao,
    private val userProfileDao: UserProfileDao,
    private val notificationHelper: NotificationHelper,
) {
    private companion object {
        const val ATTACHMENTS_BUCKET = "chat-attachments"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var channel: RealtimeChannel? = null
    private var listeningUserId: String? = null

    /** The peer whose thread is currently on screen — suppresses its own notifications. */
    @Volatile
    private var activeConversationPeerId: String? = null

    fun setActiveConversation(peerId: String?) {
        activeConversationPeerId = peerId
    }

    fun observeConversation(userId: String, otherUserId: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao.observeConversation(userId, otherUserId)

    fun observeUnreadCount(userId: String): Flow<Int> = chatMessageDao.observeUnreadCount(userId)

    fun observeUnreadCountFromSender(userId: String, otherUserId: String): Flow<Int> =
        chatMessageDao.observeUnreadCountFromSender(userId, otherUserId)

    fun observeLastMessage(userId: String, otherUserId: String): Flow<ChatMessageEntity?> =
        chatMessageDao.observeLastMessage(userId, otherUserId)

    /** Starts (once per signed-in user) listening for inserts on the messages table. */
    fun startListening(userId: String) {
        if (listeningUserId == userId) return
        listeningUserId = userId
        scope.launch {
            runCatching {
                val ch = realtime.channel("messages-$userId")
                channel = ch
                val inserts = ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                }
                ch.subscribe()
                inserts.collect { action ->
                    val dto = runCatching { action.decodeRecord<ChatMessageDto>() }.getOrNull() ?: return@collect
                    if (dto.senderId != userId && dto.recipientId != userId) return@collect
                    chatMessageDao.upsert(dto.toEntity())
                    if (dto.senderId != userId && dto.senderId != activeConversationPeerId) {
                        val senderName = userProfileDao.observeById(dto.senderId).first()?.fullName ?: "Nuovo messaggio"
                        notificationHelper.showChatMessageNotification(dto.senderId, senderName, dto.content)
                    }
                }
            }
        }
    }

    suspend fun sendMessage(senderId: String, recipientId: String, content: String) {
        val entity = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            recipientId = recipientId,
            content = content,
            createdAtEpochMs = System.currentTimeMillis(),
            readAtEpochMs = null,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
        chatMessageDao.upsert(entity)
        runCatching { postgrest.from("messages").upsert(entity.toDto()) }
            .onSuccess {
                chatMessageDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
                notifyPeerOfMessage(senderId, recipientId, content)
            }
    }

    /** Best-effort server push so the peer is notified even if the app isn't running. */
    private suspend fun notifyPeerOfMessage(senderId: String, recipientId: String, content: String) {
        val senderName = userProfileDao.observeById(senderId).first()?.fullName ?: "Vibe Fitness"
        runCatching {
            functions.invoke(
                "send-push",
                body = SendPushRequest(recipientId = recipientId, type = "chat_message", title = senderName, body = content),
            )
        }
    }

    /** Uploads [bytes] to the chat-attachments bucket and sends it as a message with an attachment. */
    suspend fun sendAttachment(
        senderId: String,
        recipientId: String,
        fileName: String,
        bytes: ByteArray,
        type: ChatAttachmentType,
    ) {
        val path = "$senderId/${UUID.randomUUID()}-$fileName"
        storage.from(ATTACHMENTS_BUCKET).upload(path, bytes)
        val url = storage.from(ATTACHMENTS_BUCKET).publicUrl(path)

        val entity = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            recipientId = recipientId,
            content = if (type == ChatAttachmentType.IMAGE) "📷 Immagine" else "📎 $fileName",
            createdAtEpochMs = System.currentTimeMillis(),
            readAtEpochMs = null,
            attachmentUrl = url,
            attachmentName = fileName,
            attachmentType = type,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
        chatMessageDao.upsert(entity)
        runCatching { postgrest.from("messages").upsert(entity.toDto()) }
            .onSuccess {
                chatMessageDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
                notifyPeerOfMessage(senderId, recipientId, entity.content)
            }
    }

    /** Soft-deletes a message the caller sent: clears its content/attachment locally and remotely
     * but keeps the row so the peer sees a "messaggio eliminato" tombstone instead of a gap. */
    suspend fun deleteMessage(messageId: String, requestedBySenderId: String) {
        val message = chatMessageDao.getById(messageId) ?: return
        if (message.senderId != requestedBySenderId) return
        val updated = message.copy(
            content = "",
            attachmentUrl = null,
            attachmentName = null,
            attachmentType = null,
            isDeleted = true,
            syncStatus = SyncStatus.PENDING_UPDATE,
        )
        chatMessageDao.upsert(updated)
        runCatching { postgrest.from("messages").upsert(updated.toDto()) }
            .onSuccess { chatMessageDao.upsert(updated.copy(syncStatus = SyncStatus.SYNCED)) }
    }

    suspend fun markConversationRead(userId: String, otherUserId: String) {
        val now = System.currentTimeMillis()
        chatMessageDao.markRead(userId, otherUserId, now)
        runCatching {
            postgrest.from("messages").update({
                set("read_at", Instant.ofEpochMilli(now).toString())
            }) {
                filter {
                    eq("recipient_id", userId)
                    eq("sender_id", otherUserId)
                }
            }
        }
    }
}
