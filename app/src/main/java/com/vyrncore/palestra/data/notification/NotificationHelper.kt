package com.vyrncore.palestra.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
                )
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
                )
            )
            manager?.createNotificationChannel(
                NotificationChannel(
                    PT_REMINDER_CHANNEL_ID,
                    "Promemoria PT",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
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

    fun showWorkoutReminder(title: String, message: String) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    suspend fun showChatMessageNotification(conversationId: String, senderName: String, message: String) {
        if (!hasNotificationPermission() || !themeRepository.chatNotificationsEnabled.first()) return

        val notification = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(senderName)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(conversationId.hashCode(), notification)
    }

    suspend fun showPlanUpdateNotification(title: String, message: String) {
        if (!hasNotificationPermission() || !themeRepository.planNotificationsEnabled.first()) return

        val notification = NotificationCompat.Builder(context, PLAN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(PLAN_NOTIFICATION_ID, notification)
    }

    suspend fun showPersonalRecordNotification(exerciseName: String, estimatedOneRepMaxKg: Double) {
        if (!hasNotificationPermission() || !themeRepository.achievementNotificationsEnabled.first()) return

        val notification = NotificationCompat.Builder(context, ACHIEVEMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎖️ Nuovo record personale!")
            .setContentText("$exerciseName · 1RM stimato ${"%.1f".format(estimatedOneRepMaxKg)} kg")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(exerciseName.hashCode(), notification)
    }

    fun showPtNoteReminder(clientName: String, message: String) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, PT_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Promemoria: $clientName")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(clientName.hashCode(), notification)
    }

    /** Fires when a rest timer set via [com.vyrncore.palestra.data.notification.ReminderScheduler.scheduleRestTimerEnd]
     * elapses - covers the case where the allievo left the Rest Timer screen (or backgrounded the
     * app) before the countdown finished, so the in-screen vibrate-on-finish never got to run. */
    fun showRestTimerFinished(exerciseName: String?) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, REST_TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recupero terminato ⏱️")
            .setContentText(if (exerciseName != null) "Pronto per: $exerciseName" else "Si riparte!")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(REST_TIMER_NOTIFICATION_ID, notification)
    }

    fun showWeeklyDigest(title: String, message: String) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, DIGEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(DIGEST_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val REMINDER_CHANNEL_ID = "workout_reminders"
        const val REMINDER_NOTIFICATION_ID = 1001
        const val CHAT_CHANNEL_ID = "chat_messages"
        const val PLAN_CHANNEL_ID = "plan_updates"
        const val PLAN_NOTIFICATION_ID = 1002
        const val ACHIEVEMENT_CHANNEL_ID = "achievements"
        const val PT_REMINDER_CHANNEL_ID = "pt_note_reminders"
        const val DIGEST_CHANNEL_ID = "weekly_digest"
        const val DIGEST_NOTIFICATION_ID = 1003
        const val REST_TIMER_CHANNEL_ID = "rest_timer"
        const val REST_TIMER_NOTIFICATION_ID = 1004
    }
}
