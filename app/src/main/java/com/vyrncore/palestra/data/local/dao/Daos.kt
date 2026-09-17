package com.vyrncore.palestra.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.entity.BodyMetricEntity
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.local.entity.SetEntryEntity
import com.vyrncore.palestra.data.local.entity.UserProfileEntity
import com.vyrncore.palestra.data.local.entity.WorkoutPlanEntity
import com.vyrncore.palestra.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Upsert
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE ptId = :ptId")
    fun observeClientsOfPt(ptId: String): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<UserProfileEntity>
}

@Dao
interface ExerciseDao {
    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}

@Dao
interface WorkoutPlanDao {
    @Upsert
    suspend fun upsert(plan: WorkoutPlanEntity)

    @Query("SELECT * FROM workout_plans WHERE assignedToUserId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeForUser(userId: String): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans WHERE createdByPtId = :ptId ORDER BY createdAtEpochMs DESC")
    fun observeCreatedByPt(ptId: String): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkoutPlanEntity?

    @Query("SELECT * FROM workout_plans WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<WorkoutPlanEntity>

    @Delete
    suspend fun delete(plan: WorkoutPlanEntity)
}

@Dao
interface PlanExerciseDao {
    @Upsert
    suspend fun upsert(planExercise: PlanExerciseEntity)

    @Upsert
    suspend fun upsertAll(planExercises: List<PlanExerciseEntity>)

    @Query("SELECT * FROM plan_exercises WHERE planId = :planId ORDER BY orderIndex ASC")
    fun observeForPlan(planId: String): Flow<List<PlanExerciseEntity>>

    @Query("SELECT * FROM plan_exercises WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<PlanExerciseEntity>

    @Delete
    suspend fun delete(planExercise: PlanExerciseEntity)
}

@Dao
interface WorkoutSessionDao {
    @Upsert
    suspend fun upsert(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startedAtEpochMs DESC")
    fun observeForUser(userId: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<WorkoutSessionEntity>

    @Query(
        """
        SELECT ws.id AS sessionId, ws.planId AS planId, ws.startedAtEpochMs AS startedAtEpochMs,
               ws.endedAtEpochMs AS endedAtEpochMs, COUNT(se.id) AS setCount,
               COALESCE(SUM(se.weightKg * se.reps), 0) AS totalVolumeKg
        FROM workout_sessions ws
        LEFT JOIN set_entries se ON se.sessionId = ws.id
        WHERE ws.userId = :userId
        GROUP BY ws.id
        ORDER BY ws.startedAtEpochMs DESC
        """,
    )
    fun observeSessionSummaries(userId: String): Flow<List<SessionSummary>>
}

data class SessionSummary(
    val sessionId: String,
    val planId: String?,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val setCount: Int,
    val totalVolumeKg: Double,
)

@Dao
interface SetEntryDao {
    @Upsert
    suspend fun upsert(entry: SetEntryEntity)

    @Query("SELECT * FROM set_entries WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    fun observeForSession(sessionId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId ORDER BY completedAtEpochMs ASC")
    fun observeHistoryForExercise(exerciseId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<SetEntryEntity>
}

@Dao
interface BodyMetricDao {
    @Upsert
    suspend fun upsert(metric: BodyMetricEntity)

    @Query("SELECT * FROM body_metrics WHERE userId = :userId ORDER BY dateEpochMs DESC")
    fun observeForUser(userId: String): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<BodyMetricEntity>
}
