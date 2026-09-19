package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.local.ExerciseCatalogSeed
import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.ExerciseDao
import com.vyrncore.palestra.data.local.dao.MuscleGroupVolume
import com.vyrncore.palestra.data.local.dao.PersonalRecord
import com.vyrncore.palestra.data.local.dao.PlanExerciseDao
import com.vyrncore.palestra.data.local.dao.ProgramDao
import com.vyrncore.palestra.data.local.dao.SessionSummary
import com.vyrncore.palestra.data.local.dao.SetEntryDao
import com.vyrncore.palestra.data.local.dao.WeeklyVolume
import com.vyrncore.palestra.data.local.dao.WorkoutPlanDao
import com.vyrncore.palestra.data.local.dao.WorkoutSessionDao
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.local.entity.ProgramEntity
import com.vyrncore.palestra.data.local.entity.SetEntryEntity
import com.vyrncore.palestra.data.local.entity.WorkoutPlanEntity
import com.vyrncore.palestra.data.local.entity.WorkoutSessionEntity
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Serializable
data class WeeklyRankingEntry(
    @SerialName("display_name") val displayName: String,
    @SerialName("workouts_this_week") val workoutsThisWeek: Int,
)

@Singleton
class WorkoutRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val setEntryDao: SetEntryDao,
    private val programDao: ProgramDao,
    private val postgrest: Postgrest,
) {
    /** Peers sharing the same PT, ranked by workouts completed in the last 7 days. Computed
     * server-side (a SECURITY DEFINER function) so an allievo never gets broad read access to
     * other users' rows - only first names and a count come back. */
    suspend fun fetchWeeklyRanking(): List<WeeklyRankingEntry> =
        runCatching { postgrest.rpc("get_weekly_ranking").decodeList<WeeklyRankingEntry>() }
            .getOrDefault(emptyList())
    // Exercise catalog
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    suspend fun seedCatalogIfNeeded() {
        if (exerciseDao.count() == 0) {
            exerciseDao.upsertAll(ExerciseCatalogSeed.exercises)
        }
    }

    suspend fun addCustomExercise(name: String, muscleGroup: String, createdByUserId: String, imageUrl: String? = null): String {
        val id = UUID.randomUUID().toString()
        exerciseDao.upsert(
            ExerciseEntity(
                id = id,
                name = name,
                muscleGroup = muscleGroup,
                createdByUserId = createdByUserId,
                isCustom = true,
                imageUrl = imageUrl,
                syncStatus = SyncStatus.PENDING_CREATE,
            )
        )
        return id
    }

    // Plans (created by PT, assigned to a client)
    fun observePlansForUser(userId: String): Flow<List<WorkoutPlanEntity>> = workoutPlanDao.observeForUser(userId)

    fun observePlansCreatedByPt(ptId: String): Flow<List<WorkoutPlanEntity>> = workoutPlanDao.observeCreatedByPt(ptId)

    fun observePlanExercises(planId: String): Flow<List<PlanExerciseEntity>> = planExerciseDao.observeForPlan(planId)

    fun observePlanExerciseCount(planId: String): Flow<Int> = planExerciseDao.observeExerciseCount(planId)

    suspend fun createPlan(
        name: String,
        description: String?,
        createdByPtId: String,
        assignedToUserId: String,
        exercises: List<PlanExerciseEntity>,
        category: String? = null,
        estimatedMinutes: Int? = null,
    ): String {
        val planId = UUID.randomUUID().toString()
        workoutPlanDao.upsert(
            WorkoutPlanEntity(
                id = planId,
                name = name,
                description = description,
                createdByPtId = createdByPtId,
                assignedToUserId = assignedToUserId,
                createdAtEpochMs = System.currentTimeMillis(),
                category = category,
                estimatedMinutes = estimatedMinutes,
                syncStatus = SyncStatus.PENDING_CREATE,
            )
        )
        exercises.forEachIndexed { index, exercise ->
            planExerciseDao.upsert(
                exercise.copy(
                    id = UUID.randomUUID().toString(),
                    planId = planId,
                    orderIndex = index,
                    syncStatus = SyncStatus.PENDING_CREATE,
                )
            )
        }
        return planId
    }

    // Structured multi-week programs (mesocicli)
    fun observeProgramsForUser(userId: String): Flow<List<ProgramEntity>> = programDao.observeForUser(userId)

    fun observeProgramsCreatedByPt(ptId: String): Flow<List<ProgramEntity>> = programDao.observeCreatedByPt(ptId)

    fun observePlansForProgram(programId: String): Flow<List<WorkoutPlanEntity>> = workoutPlanDao.observeForProgram(programId)

    /** Creates a program by generating one workout_plans row per week upfront, with each week's
     * target weights scaled by (1 + weeklyIncrementPercent/100)^(week-1) from the base exercises -
     * a simple linear-percentage progressive overload, applied once at creation rather than
     * computed lazily, so every week's plan is a completely ordinary plan the rest of the app
     * (sync, active workout, history) already knows how to handle. */
    suspend fun createProgram(
        name: String,
        createdByPtId: String,
        assignedToUserId: String,
        totalWeeks: Int,
        weeklyIncrementPercent: Double,
        baseExercises: List<PlanExerciseEntity>,
        category: String? = null,
    ): String {
        val programId = UUID.randomUUID().toString()
        programDao.upsert(
            ProgramEntity(
                id = programId,
                name = name,
                createdByPtId = createdByPtId,
                assignedToUserId = assignedToUserId,
                totalWeeks = totalWeeks,
                weeklyIncrementPercent = weeklyIncrementPercent,
                startEpochMs = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_CREATE,
            )
        )
        val estimatedMinutes = (baseExercises.sumOf { it.targetSets } * 3 / 2).takeIf { it > 0 }
        for (week in 1..totalWeeks) {
            val planId = UUID.randomUUID().toString()
            val factor = (1 + weeklyIncrementPercent / 100.0).pow(week - 1)
            workoutPlanDao.upsert(
                WorkoutPlanEntity(
                    id = planId,
                    name = "$name – Settimana $week",
                    createdByPtId = createdByPtId,
                    assignedToUserId = assignedToUserId,
                    createdAtEpochMs = System.currentTimeMillis(),
                    category = category,
                    estimatedMinutes = estimatedMinutes,
                    programId = programId,
                    weekIndex = week,
                    syncStatus = SyncStatus.PENDING_CREATE,
                )
            )
            baseExercises.forEachIndexed { index, exercise ->
                planExerciseDao.upsert(
                    exercise.copy(
                        id = UUID.randomUUID().toString(),
                        planId = planId,
                        orderIndex = index,
                        targetWeightKg = exercise.targetWeightKg?.let { it * factor },
                        syncStatus = SyncStatus.PENDING_CREATE,
                    )
                )
            }
        }
        return programId
    }

    // Active workout session tracking
    fun observeSession(sessionId: String): Flow<WorkoutSessionEntity?> = workoutSessionDao.observeById(sessionId)

    fun observeSessionsForUser(userId: String): Flow<List<WorkoutSessionEntity>> = workoutSessionDao.observeForUser(userId)

    fun observeSessionSummaries(userId: String): Flow<List<SessionSummary>> = workoutSessionDao.observeSessionSummaries(userId)

    fun observeSetsForSession(sessionId: String): Flow<List<SetEntryEntity>> = setEntryDao.observeForSession(sessionId)

    fun observeHistoryForExercise(exerciseId: String): Flow<List<SetEntryEntity>> =
        setEntryDao.observeHistoryForExercise(exerciseId)

    fun observeVolumeByMuscleGroup(userId: String): Flow<List<MuscleGroupVolume>> =
        setEntryDao.observeVolumeByMuscleGroup(userId)

    fun observeWeeklyVolume(userId: String): Flow<List<WeeklyVolume>> =
        setEntryDao.observeWeeklyVolume(userId)

    fun observePersonalRecords(userId: String): Flow<List<PersonalRecord>> =
        setEntryDao.observePersonalRecords(userId)

    suspend fun startSession(userId: String, planId: String?): String {
        val sessionId = UUID.randomUUID().toString()
        workoutSessionDao.upsert(
            WorkoutSessionEntity(
                id = sessionId,
                planId = planId,
                userId = userId,
                startedAtEpochMs = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_CREATE,
            )
        )
        return sessionId
    }

    suspend fun endSessionById(sessionId: String, notes: String?) {
        val session = workoutSessionDao.observeById(sessionId).first() ?: return
        endSession(session, notes)
    }

    suspend fun endSession(session: WorkoutSessionEntity, notes: String?) {
        workoutSessionDao.upsert(
            session.copy(
                endedAtEpochMs = System.currentTimeMillis(),
                notes = notes,
                syncStatus = SyncStatus.PENDING_UPDATE,
            )
        )
    }

    /** Logs a set and returns true if it beats every previous set logged for this exercise - an
     * estimated 1RM (Epley) new personal record, used to trigger a celebratory notification. */
    suspend fun logSet(sessionId: String, exerciseId: String, setNumber: Int, reps: Int, weightKg: Double, rpe: Double?): Boolean {
        val previousBest = setEntryDao.bestEstimatedOneRepMax(sessionId, exerciseId)
        setEntryDao.upsert(
            SetEntryEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                exerciseId = exerciseId,
                setNumber = setNumber,
                reps = reps,
                weightKg = weightKg,
                rpe = rpe,
                completedAtEpochMs = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_CREATE,
            )
        )
        val newEstimatedOneRepMax = weightKg * (1 + reps / 30.0)
        return previousBest != null && newEstimatedOneRepMax > previousBest
    }
}
