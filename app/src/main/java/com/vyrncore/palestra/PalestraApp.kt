package com.vyrncore.palestra

import android.app.Application
import android.os.Build
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
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
class PalestraApp : Application(), Configuration.Provider, ImageLoaderFactory {

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
        applicationScope.launch { workoutRepository.seedCatalogIfNeeded() }
    }

    /** Single shared Coil loader for the whole app: a 100 MB on-disk image cache means chat
     * attachments, avatars and exercise GIFs load instantly after the first fetch and don't
     * re-download on every screen. respectCacheHeaders(false) stops Supabase Storage's
     * revalidation headers from evicting cached entries; crossfade smooths image appearance. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            // Animated-GIF support for exercise demo URLs (hardware decoder on API 28+).
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
}
