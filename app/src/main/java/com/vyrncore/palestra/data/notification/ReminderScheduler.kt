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

    private companion object {
        const val WORK_NAME = "workout_reminder_daily"
        const val DIGEST_WORK_NAME = "weekly_digest"
    }
}
