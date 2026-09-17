package com.vyrncore.palestra.data.sync

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.BodyMetricDao
import com.vyrncore.palestra.data.local.dao.ExerciseDao
import com.vyrncore.palestra.data.local.dao.PlanExerciseDao
import com.vyrncore.palestra.data.local.dao.SetEntryDao
import com.vyrncore.palestra.data.local.dao.UserProfileDao
import com.vyrncore.palestra.data.local.dao.WorkoutPlanDao
import com.vyrncore.palestra.data.local.dao.WorkoutSessionDao
import com.vyrncore.palestra.data.remote.toDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes every locally pending row (created/updated offline, e.g. mid-workout with no signal)
 * to Supabase. Runs from [com.vyrncore.palestra.data.sync.SyncWorker] whenever connectivity
 * returns; each push is a plain upsert, so re-running a partially failed sync is always safe.
 */
@Singleton
class SyncManager @Inject constructor(
    private val postgrest: Postgrest,
    private val userProfileDao: UserProfileDao,
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val setEntryDao: SetEntryDao,
    private val bodyMetricDao: BodyMetricDao,
) {
    suspend fun syncAll() {
        userProfileDao.getPendingSync().forEach { entity ->
            postgrest.from("profiles").upsert(entity.toDto())
            userProfileDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        exerciseDao.getPendingSync().forEach { entity ->
            postgrest.from("exercises").upsert(entity.toDto())
            exerciseDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        workoutPlanDao.getPendingSync().forEach { entity ->
            postgrest.from("workout_plans").upsert(entity.toDto())
            workoutPlanDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        planExerciseDao.getPendingSync().forEach { entity ->
            postgrest.from("plan_exercises").upsert(entity.toDto())
            planExerciseDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        workoutSessionDao.getPendingSync().forEach { entity ->
            postgrest.from("workout_sessions").upsert(entity.toDto())
            workoutSessionDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        setEntryDao.getPendingSync().forEach { entity ->
            postgrest.from("set_entries").upsert(entity.toDto())
            setEntryDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        bodyMetricDao.getPendingSync().forEach { entity ->
            postgrest.from("body_metrics").upsert(entity.toDto())
            bodyMetricDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
    }
}
