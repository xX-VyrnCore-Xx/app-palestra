package com.vyrncore.palestra.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Built via [com.vyrncore.palestra.data.work.AppWorkerFactory] with a plain constructor — see
 * [com.vyrncore.palestra.data.notification.ReminderWorker] for why this avoids @HiltWorker.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        syncManager.syncAll()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}
