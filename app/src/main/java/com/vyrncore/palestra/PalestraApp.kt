package com.vyrncore.palestra

import android.app.Application
import androidx.work.Configuration
import com.vyrncore.palestra.data.notification.ReminderScheduler
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.data.sync.SyncScheduler
import com.vyrncore.palestra.data.work.AppWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PalestraApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: AppWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var workoutRepository: WorkoutRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        syncScheduler.schedulePeriodicSync()
        reminderScheduler.scheduleDailyCheck()
        reminderScheduler.scheduleWeeklyDigest()
        applicationScope.launch { workoutRepository.seedCatalogIfNeeded() }
    }
}
