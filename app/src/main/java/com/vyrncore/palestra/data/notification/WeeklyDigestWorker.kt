package com.vyrncore.palestra.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Once a week, sums up the allievo's own activity into one glanceable notification - workouts
 * completed, kg lifted, current streak - instead of making them open the app to piece it
 * together from Home. Built with a plain constructor for the same kapt/K2 reason as
 * [ReminderWorker].
 */
class WeeklyDigestWorker(
    context: Context,
    params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = authRepository.currentUserId ?: return Result.success()
        val profile = authRepository.observeProfile(userId).first()
        if (profile?.role != UserRole.ALLIEVO) return Result.success()

        val zone = ZoneId.systemDefault()
        val weekAgo = LocalDate.now(zone).minusDays(7)
        val sessions = workoutRepository.observeSessionsForUser(userId).first()
        val doneDates = sessions.mapNotNull { it.endedAtEpochMs }
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toSortedSet()
        val workoutsThisWeek = doneDates.count { it.isAfter(weekAgo) }

        if (workoutsThisWeek == 0) {
            notificationHelper.showWeeklyDigest(
                "Riepilogo della settimana",
                "Nessun allenamento questa settimana. Ripartiamo da qui? 💪",
            )
            return Result.success()
        }

        val weeklyVolume = workoutRepository.observeWeeklyVolume(userId).first()
        val totalVolumeKg = weeklyVolume.lastOrNull()?.totalVolumeKg?.toInt() ?: 0

        var streak = 0
        var day = LocalDate.now(zone)
        while (doneDates.contains(day)) {
            streak++
            day = day.minusDays(1)
        }

        notificationHelper.showWeeklyDigest(
            "Riepilogo della settimana 🎖️",
            "$workoutsThisWeek allenamenti · $totalVolumeKg kg totali sollevati · streak di $streak giorni. Continua così!",
        )
        return Result.success()
    }
}
