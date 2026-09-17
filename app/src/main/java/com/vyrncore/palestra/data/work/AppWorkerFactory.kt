package com.vyrncore.palestra.data.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.vyrncore.palestra.data.notification.NotificationHelper
import com.vyrncore.palestra.data.notification.ReminderWorker
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ThemeRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.data.sync.SyncManager
import com.vyrncore.palestra.data.sync.SyncWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Looks up worker dependencies via a Hilt [EntryPoint] instead of the androidx.hilt-work
 * @HiltWorker/@AssistedInject codegen path (see [ReminderWorker] for why).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerDependenciesEntryPoint {
    fun authRepository(): AuthRepository
    fun workoutRepository(): WorkoutRepository
    fun themeRepository(): ThemeRepository
    fun notificationHelper(): NotificationHelper
    fun syncManager(): SyncManager
}

class AppWorkerFactory @Inject constructor() : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        val entryPoint = EntryPointAccessors.fromApplication(appContext, WorkerDependenciesEntryPoint::class.java)
        return when (workerClassName) {
            ReminderWorker::class.java.name -> ReminderWorker(
                appContext,
                workerParameters,
                entryPoint.authRepository(),
                entryPoint.workoutRepository(),
                entryPoint.themeRepository(),
                entryPoint.notificationHelper(),
            )
            SyncWorker::class.java.name -> SyncWorker(
                appContext,
                workerParameters,
                entryPoint.syncManager(),
            )
            else -> null
        }
    }
}
