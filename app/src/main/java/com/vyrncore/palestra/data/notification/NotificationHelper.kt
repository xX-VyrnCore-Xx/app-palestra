package com.vyrncore.palestra.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vyrncore.palestra.MainActivity
import com.vyrncore.palestra.R
import com.vyrncore.palestra.data.repository.ThemeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeRepository: ThemeRepository,
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Promemoria allenamento",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHAT_CHANNEL_ID,
                    "Messaggi chat",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableLights(true)
                    lightColor = BRAND_COLOR
                }
            )
            manager?.createNotificationChannel(
                NotificationChannel(
                    PLAN_CHANNEL_ID,
                    "Aggiornamenti scheda",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
            manager?.createNotificationChannel(
                NotificationChannel(
                    ACHIEVEMENT_CHANNEL_ID,
                    "Record e traguardi",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableLights(true)
                    lightColor = BRAND_COLOR
                }
            )
            manager?.createNotificationChannel(
                NotificationChannel(
                    DIGEST_CHANNEL_ID,
                    "Riepilogo settimanale",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
            manager?.createNotificationChannel(
                NotificationChannel(
                    REST_TIMER_CHANNEL_ID,
                    "Timer di recupero",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    // Vibration lives on the channel itself, not just the notification, so the
                    // buzz still fires when this is delivered by the background worker while the
                    // Compose screen (and its own foreground-only vibrate() call) isn't running.
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                }
            )
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Every notification this class posts used to have no tap action at all - tapping one did
     * nothing. [chatPeerId] set deep-links a chat notification straight into that thread instead
     * of just opening the app to wherever it was left. */
    private fun openAppPendingIntent(requestCode: Int, chatPeerId: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (chatPeerId != null) putExtra(MainActivity.EXTRA_OPEN_CHAT_PEER_ID, chatPeerId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    /** Shared defaults every notification this app posts should carry: the real small icon (see
     * [R.drawable.ic_notification] - the previous [R.drawable.ic_launcher_foreground] is a thin,
     * mostly-transparent wordmark meant for an adaptive-icon mask and rendered as an illegible
     * blob in the status bar), the brand color tint Android applies behind it on 5.0+, and
     * [NotificationCompat.Builder.setWhen] left at post time so notifications sort correctly
     * alongside the rest of the shade. */
    private fun baseBuilder(channelId: String, category: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_COLOR)
            .setCategory(category)
            .setAutoCancel(true)

    fun showWorkoutReminder(title: String, message: String) {
        if (!hasNotificationPermission()) return

        val notification = baseBuilder(REMINDER_CHANNEL_ID, NotificationCompat.CATEGORY_REMINDER)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppPendingIntent(REMINDER_NOTIFICATION_ID))
            .build()
        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    suspend fun showChatMessageNotification(conversationId: String, senderName: String, message: String) {
        if (!hasNotificationPermission() || !themeRepository.chatNotificationsEnabled.first()) return

        val notification = baseBuilder(CHAT_CHANNEL_ID, NotificationCompat.CATEGORY_MESSAGE)
            .setContentTitle(senderName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent(conversationId.hashCode(), chatPeerId = conversationId))
            .build()
        NotificationManagerCompat.from(context).notify(conversationId.hashCode(), notification)
    }

    suspend fun showPlanUpdateNotification(title: String, message: String) {
        if (!hasNotificationPermission() || !themeRepository.planNotificationsEnabled.first()) return

        val notification = baseBuilder(PLAN_CHANNEL_ID, NotificationCompat.CATEGORY_EVENT)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppPendingIntent(PLAN_NOTIFICATION_ID))
            .build()
        NotificationManagerCompat.from(context).notify(PLAN_NOTIFICATION_ID, notification)
    }

    suspend fun showPersonalRecordNotification(exerciseName: String, estimatedOneRepMaxKg: Double) {
        if (!hasNotificationPermission() || !themeRepository.achievementNotificationsEnabled.first()) return

        val notification = baseBuilder(ACHIEVEMENT_CHANNEL_ID, NotificationCompat.CATEGORY_STATUS)
            .setContentTitle("🎖️ Nuovo record personale!")
            .setContentText("$exerciseName · 1RM stimato ${"%.1f".format(estimatedOneRepMaxKg)} kg")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent(exerciseName.hashCode()))
            .build()
        NotificationManagerCompat.from(context).notify(exerciseName.hashCode(), notification)
    }

    /** Fires when a rest timer set via [com.vyrncore.palestra.data.notification.ReminderScheduler.scheduleRestTimerEnd]
     * elapses - covers the case where the allievo left the Rest Timer screen (or backgrounded the
     * app) before the countdown finished, so the in-screen vibrate-on-finish never got to run. */
    fun showRestTimerFinished(exerciseName: String?) {
        if (!hasNotificationPermission()) return

        val notification = baseBuilder(REST_TIMER_CHANNEL_ID, NotificationCompat.CATEGORY_ALARM)
            .setContentTitle("Recupero terminato ⏱️")
            .setContentText(if (exerciseName != null) "Pronto per: $exerciseName" else "Si riparte!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent(REST_TIMER_NOTIFICATION_ID))
            .build()
        NotificationManagerCompat.from(context).notify(REST_TIMER_NOTIFICATION_ID, notification)
    }

    fun showWeeklyDigest(title: String, message: String) {
        if (!hasNotificationPermission()) return

        val notification = baseBuilder(DIGEST_CHANNEL_ID, NotificationCompat.CATEGORY_SOCIAL)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppPendingIntent(DIGEST_NOTIFICATION_ID))
            .build()
        NotificationManagerCompat.from(context).notify(DIGEST_NOTIFICATION_ID, notification)
    }

    private companion object {
        // Same brand orange as the app's Material color scheme (ui/theme/Color.kt Orange50) -
        // Android tints the small icon's background circle with this on 5.0+.
        val BRAND_COLOR = Color.parseColor("#F76B15")

        const val REMINDER_CHANNEL_ID = "workout_reminders"
        const val REMINDER_NOTIFICATION_ID = 1001
        const val CHAT_CHANNEL_ID = "chat_messages"
        const val PLAN_CHANNEL_ID = "plan_updates"
        const val PLAN_NOTIFICATION_ID = 1002
        const val ACHIEVEMENT_CHANNEL_ID = "achievements"
        const val DIGEST_CHANNEL_ID = "weekly_digest"
        const val DIGEST_NOTIFICATION_ID = 1003
        const val REST_TIMER_CHANNEL_ID = "rest_timer"
        const val REST_TIMER_NOTIFICATION_ID = 1004
    }
}
