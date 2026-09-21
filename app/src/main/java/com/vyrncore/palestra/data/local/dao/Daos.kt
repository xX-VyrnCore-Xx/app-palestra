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
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanTemplateEntity
import com.vyrncore.palestra.data.local.entity.PlanTemplateExerciseEntity
import com.vyrncore.palestra.data.local.entity.ProgramEntity
import com.vyrncore.palestra.data.local.entity.PtNoteEntity
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

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?

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

    @Query("SELECT * FROM workout_plans WHERE programId = :programId ORDER BY weekIndex ASC")
    fun observeForProgram(programId: String): Flow<List<WorkoutPlanEntity>>

    @Delete
    suspend fun delete(plan: WorkoutPlanEntity)
}

@Dao
interface ProgramDao {
    @Upsert
    suspend fun upsert(program: ProgramEntity)

    @Query("SELECT * FROM programs WHERE assignedToUserId = :userId ORDER BY startEpochMs DESC")
    fun observeForUser(userId: String): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE createdByPtId = :ptId ORDER BY startEpochMs DESC")
    fun observeCreatedByPt(ptId: String): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<ProgramEntity>
}

@Dao
interface PlanExerciseDao {
    @Upsert
    suspend fun upsert(planExercise: PlanExerciseEntity)

    @Upsert
    suspend fun upsertAll(planExercises: List<PlanExerciseEntity>)

    @Query("SELECT * FROM plan_exercises WHERE planId = :planId ORDER BY orderIndex ASC")
    fun observeForPlan(planId: String): Flow<List<PlanExerciseEntity>>

    @Query("SELECT COUNT(*) FROM plan_exercises WHERE planId = :planId")
    fun observeExerciseCount(planId: String): Flow<Int>

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

    @Query(
        """
        SELECT e.muscleGroup AS muscleGroup, COALESCE(SUM(se.weightKg * se.reps), 0) AS totalVolumeKg
        FROM set_entries se
        JOIN workout_sessions ws ON ws.id = se.sessionId
        JOIN exercises e ON e.id = se.exerciseId
        WHERE ws.userId = :userId
        GROUP BY e.muscleGroup
        ORDER BY totalVolumeKg DESC
        """,
    )
    fun observeVolumeByMuscleGroup(userId: String): Flow<List<MuscleGroupVolume>>

    @Query(
        """
        SELECT (se.completedAtEpochMs / 604800000) AS weekBucket, COALESCE(SUM(se.weightKg * se.reps), 0) AS totalVolumeKg
        FROM set_entries se
        JOIN workout_sessions ws ON ws.id = se.sessionId
        WHERE ws.userId = :userId
        GROUP BY weekBucket
        ORDER BY weekBucket ASC
        """,
    )
    fun observeWeeklyVolume(userId: String): Flow<List<WeeklyVolume>>

    /** Best estimated 1RM (Epley formula: weight * (1 + reps/30)) ever logged per exercise. */
    @Query(
        """
        SELECT se.exerciseId AS exerciseId, e.name AS exerciseName,
               MAX(se.weightKg * (1 + se.reps / 30.0)) AS estimatedOneRepMaxKg
        FROM set_entries se
        JOIN workout_sessions ws ON ws.id = se.sessionId
        JOIN exercises e ON e.id = se.exerciseId
        WHERE ws.userId = :userId
        GROUP BY se.exerciseId
        ORDER BY estimatedOneRepMaxKg DESC
        """,
    )
    fun observePersonalRecords(userId: String): Flow<List<PersonalRecord>>

    /** The best estimated 1RM logged so far for this exercise by the session's owner, used to
     * detect whether a just-logged set is a new personal record. Null if none logged yet. */
    @Query(
        """
        SELECT MAX(se.weightKg * (1 + se.reps / 30.0))
        FROM set_entries se
        JOIN workout_sessions ws ON ws.id = se.sessionId
        WHERE ws.userId = (SELECT userId FROM workout_sessions WHERE id = :sessionId) AND se.exerciseId = :exerciseId
        """,
    )
    suspend fun bestEstimatedOneRepMax(sessionId: String, exerciseId: String): Double?

    /** Best estimated 1RM for this exercise across ALL the user's other sessions (the workout
     * being summarized excluded), so the summary can compare "this session vs everything
     * before it" and show a positive delta when a record was just set. */
    @Query(
        """
        SELECT MAX(se.weightKg * (1 + se.reps / 30.0))
        FROM set_entries se
        JOIN workout_sessions ws ON ws.id = se.sessionId
        WHERE ws.userId = (SELECT userId FROM workout_sessions WHERE id = :sessionId)
          AND se.exerciseId = :exerciseId
          AND ws.id != :sessionId
        """,
    )
    suspend fun bestEstimatedOneRepMaxExcludingSession(sessionId: String, exerciseId: String): Double?
}

data class MuscleGroupVolume(val muscleGroup: String, val totalVolumeKg: Double)

data class WeeklyVolume(val weekBucket: Long, val totalVolumeKg: Double)

data class PersonalRecord(val exerciseId: String, val exerciseName: String, val estimatedOneRepMaxKg: Double)

@Dao
interface BodyMetricDao {
    @Upsert
    suspend fun upsert(metric: BodyMetricEntity)

    @Query("SELECT * FROM body_metrics WHERE userId = :userId ORDER BY dateEpochMs DESC")
    fun observeForUser(userId: String): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<BodyMetricEntity>
}

@Dao
interface ChatMessageDao {
    @Upsert
    suspend fun upsert(message: ChatMessageEntity)

    @Query(
        """
        SELECT * FROM chat_messages
        WHERE (senderId = :userId AND recipientId = :otherUserId) OR (senderId = :otherUserId AND recipientId = :userId)
        ORDER BY createdAtEpochMs ASC
        """,
    )
    fun observeConversation(userId: String, otherUserId: String): Flow<List<ChatMessageEntity>>

    @Query(
        """
        SELECT * FROM chat_messages
        WHERE (senderId = :userId AND recipientId = :otherUserId) OR (senderId = :otherUserId AND recipientId = :userId)
        ORDER BY createdAtEpochMs DESC LIMIT 1
        """,
    )
    fun observeLastMessage(userId: String, otherUserId: String): Flow<ChatMessageEntity?>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE recipientId = :userId AND readAtEpochMs IS NULL")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE recipientId = :userId AND senderId = :otherUserId AND readAtEpochMs IS NULL")
    fun observeUnreadCountFromSender(userId: String, otherUserId: String): Flow<Int>

    @Query("UPDATE chat_messages SET readAtEpochMs = :now WHERE recipientId = :userId AND senderId = :otherUserId AND readAtEpochMs IS NULL")
    suspend fun markRead(userId: String, otherUserId: String, now: Long)

    @Query("SELECT * FROM chat_messages WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChatMessageEntity?
}

@Dao
interface PtNoteDao {
    @Upsert
    suspend fun upsert(note: PtNoteEntity)

    @Query("SELECT * FROM pt_notes WHERE ptId = :ptId AND clientId = :clientId LIMIT 1")
    fun observeForClient(ptId: String, clientId: String): Flow<PtNoteEntity?>

    @Query("SELECT * FROM pt_notes WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<PtNoteEntity>

    @Query("SELECT * FROM pt_notes WHERE reminderAtEpochMs IS NOT NULL")
    suspend fun getAllWithReminder(): List<PtNoteEntity>
}

@Dao
interface PlanTemplateDao {
    @Upsert
    suspend fun upsert(template: PlanTemplateEntity)

    @Query("SELECT * FROM plan_templates WHERE ptId = :ptId ORDER BY createdAtEpochMs DESC")
    fun observeForPt(ptId: String): Flow<List<PlanTemplateEntity>>

    @Query("SELECT * FROM plan_templates WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<PlanTemplateEntity>

    @Delete
    suspend fun delete(template: PlanTemplateEntity)
}

@Dao
interface PlanTemplateExerciseDao {
    @Upsert
    suspend fun upsertAll(exercises: List<PlanTemplateExerciseEntity>)

    @Upsert
    suspend fun upsert(exercise: PlanTemplateExerciseEntity)

    @Query("SELECT * FROM plan_template_exercises WHERE templateId = :templateId ORDER BY orderIndex ASC")
    fun observeForTemplate(templateId: String): Flow<List<PlanTemplateExerciseEntity>>

    @Query("SELECT * FROM plan_template_exercises WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<PlanTemplateExerciseEntity>
}
