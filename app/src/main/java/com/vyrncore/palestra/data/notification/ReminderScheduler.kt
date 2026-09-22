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
 * digest and the rest-timer notification. */
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
