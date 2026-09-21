package com.vyrncore.palestra.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Fires the one-off reminder a PT set on a client's private note (see [ReminderWorker] for why
 * this carries no @HiltWorker/@AssistedInject codegen). */
class PtNoteReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val clientName = inputData.getString(KEY_CLIENT_NAME) ?: return Result.success()
        val message = inputData.getString(KEY_MESSAGE) ?: return Result.success()
        notificationHelper.showPtNoteReminder(clientName, message)
        return Result.success()
    }

    companion object {
        const val KEY_CLIENT_NAME = "clientName"
        const val KEY_MESSAGE = "message"
    }
}
