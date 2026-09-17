package com.vyrncore.palestra.data.notification

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules the daily [ReminderWorker] check for an inactive Allievo. */
@Singleton
class ReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun scheduleDailyCheck() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "workout_reminder_daily"
    }
}
