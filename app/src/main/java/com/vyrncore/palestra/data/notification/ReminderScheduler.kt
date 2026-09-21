package com.vyrncore.palestra.data.notification

import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules the daily [ReminderWorker] check for an inactive Allievo, the weekly activity
 * digest, and one-off [PtNoteReminderWorker] reminders a PT sets on a client's note. */
@Singleton
class ReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun scheduleDailyCheck() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleWeeklyDigest() {
        val request = PeriodicWorkRequestBuilder<WeeklyDigestWorker>(7, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(DIGEST_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Replaces any pending reminder for this client's note - saving a new reminder date always
     * supersedes the old one instead of stacking notifications. */
    fun schedulePtNoteReminder(clientId: String, clientName: String, message: String, delayMs: Long) {
        val request = OneTimeWorkRequestBuilder<PtNoteReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(PtNoteReminderWorker.KEY_CLIENT_NAME, clientName)
                    .putString(PtNoteReminderWorker.KEY_MESSAGE, message)
                    .build(),
            )
            .build()
        workManager.enqueueUniqueWork("pt_note_reminder_$clientId", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelPtNoteReminder(clientId: String) {
        workManager.cancelUniqueWork("pt_note_reminder_$clientId")
    }

    /** (Re)schedules the "recupero terminato" notification for [remainingSeconds] from now,
     * replacing any previous one - called every time the Rest Timer countdown starts, resumes, or
     * has its remaining time adjusted, so the backgrounded/killed-app case still gets notified even
     * though the in-screen vibrate-on-finish only runs while the composable is alive. */
    fun scheduleRestTimerEnd(remainingSeconds: Int, exerciseName: String?) {
        val request = OneTimeWorkRequestBuilder<RestTimerWorker>()
            .setInitialDelay(remainingSeconds.toLong(), TimeUnit.SECONDS)
            .setInputData(
                Data.Builder().putString(RestTimerWorker.KEY_EXERCISE_NAME, exerciseName).build(),
            )
            .build()
        workManager.enqueueUniqueWork(REST_TIMER_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Cancels a pending rest-timer notification - call on pause, or once the countdown finishes
     * while the screen is still open, so it isn't duplicated a moment later by the worker. */
    fun cancelRestTimerEnd() {
        workManager.cancelUniqueWork(REST_TIMER_WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "workout_reminder_daily"
        const val DIGEST_WORK_NAME = "weekly_digest"
        const val REST_TIMER_WORK_NAME = "rest_timer_end"
    }
}
