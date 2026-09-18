package com.vyrncore.palestra.data.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vyrncore.palestra.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives server-pushed FCM messages so PT/Allievo get notified (new chat message, new/updated
 * plan) even when the app is killed - Supabase Realtime only updates data while the process is
 * alive. The backend sends a data-only payload with a "type" field so this can pick the right
 * notification style regardless of app state.
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository

    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val userId = authRepository.currentUserId ?: return
        scope.launch { authRepository.updateFcmToken(userId, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when (data["type"]) {
            "chat_message" -> notificationHelper.showChatMessageNotification(
                conversationId = data["conversationId"].orEmpty(),
                senderName = data["senderName"] ?: "Vibe Fitness",
                message = data["body"] ?: message.notification?.body.orEmpty(),
            )
            "plan_update" -> notificationHelper.showPlanUpdateNotification(
                title = data["title"] ?: "Scheda aggiornata",
                message = data["body"] ?: message.notification?.body.orEmpty(),
            )
        }
    }
}
