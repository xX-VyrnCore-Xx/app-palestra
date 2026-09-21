package com.vyrncore.palestra.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Fires when a scheduled rest countdown elapses (see [ReminderWorker] for why this carries no
 * @HiltWorker/@AssistedInject codegen). Rescheduled/cancelled by [ReminderScheduler] every time
 * the Rest Timer screen's countdown starts, pauses, or changes, so it always reflects the time
 * actually left rather than the original duration. */
class RestTimerWorker(
    context: Context,
    params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        notificationHelper.showRestTimerFinished(inputData.getString(KEY_EXERCISE_NAME))
        return Result.success()
    }

    companion object {
        const val KEY_EXERCISE_NAME = "exerciseName"
    }
}
