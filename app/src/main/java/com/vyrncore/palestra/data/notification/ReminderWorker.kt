package com.vyrncore.palestra.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ThemeRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Nudges an inactive Allievo once a day: "you haven't trained in N days" if it's been 2+.
 *
 * Built via [com.vyrncore.palestra.data.work.AppWorkerFactory] with a plain constructor rather
 * than @HiltWorker/@AssistedInject: kapt's stub generator can't read the Kotlin metadata K2
 * emits for @AssistedInject constructors on CoroutineWorker subclasses (a known, unresolved
 * Kotlin 2.1.x/Hilt kapt incompatibility), so this class carries no annotation-processed codegen.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    private val themeRepository: ThemeRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = authRepository.currentUserId ?: return Result.success()
        if (!themeRepository.remindersEnabled.first()) return Result.success()

        val profile = authRepository.observeProfile(userId).first()
        if (profile?.role != UserRole.ALLIEVO) return Result.success()

        val thresholdDays = themeRepository.reminderThresholdDays.first()
        val customMessage = themeRepository.reminderCustomMessage.first()

        val lastSessionEpochMs = workoutRepository.observeSessionsForUser(userId).first()
            .mapNotNull { it.endedAtEpochMs }
            .maxOrNull()

        val daysSinceLastWorkout = lastSessionEpochMs?.let {
            ChronoUnit.DAYS.between(Instant.ofEpochMilli(it), Instant.now())
        }

        if (daysSinceLastWorkout == null || daysSinceLastWorkout >= thresholdDays) {
            notificationHelper.showWorkoutReminder(
                title = "Il tuo Vibe ti aspetta 🔥",
                message = customMessage?.takeIf { it.isNotBlank() } ?: if (daysSinceLastWorkout == null) {
                    "Non hai ancora registrato un allenamento. Si parte!"
                } else {
                    "Non ti alleni da $daysSinceLastWorkout giorni. Torna in palestra!"
                },
            )
        }
        return Result.success()
    }
}
