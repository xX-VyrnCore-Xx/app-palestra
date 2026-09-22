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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
    private val scope = CoroutineScope(Dispatchers.IO)

    /** Peers sharing the same PT, ranked by workouts completed in the last 7 days. */
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

    // Plans (created by PT, assigned to a client)
    fun observePlansForUser(userId: String): Flow<List<WorkoutPlanEntity>> = workoutPlanDao.observeForUser(userId)

    fun observePlanExercises(planId: String): Flow<List<PlanExerciseEntity>> = planExerciseDao.observeForPlan(planId)

    suspend fun getPlanById(planId: String): WorkoutPlanEntity? = workoutPlanDao.getById(planId)

    suspend fun getExerciseById(exerciseId: String): ExerciseEntity? = exerciseDao.getById(exerciseId)

    fun observePlanExerciseCount(planId: String): Flow<Int> = planExerciseDao.observeExerciseCount(planId)

    // Structured multi-week programs (mesocicli)
    fun observeProgramsForUser(userId: String): Flow<List<ProgramEntity>> = programDao.observeForUser(userId)

    fun observePlansForProgram(programId: String): Flow<List<WorkoutPlanEntity>> = workoutPlanDao.observeForProgram(programId)

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
        setEntryDao.observePersonalRecords(userId).shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

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
        session.planId?.let { applyAutoProgression(it, session.id) }
    }

    /** Small, automatic progressive-overload nudge - the PT owns swapping/restructuring a scheda,
     * but bumping the load or reps a notch once an exercise stops being a real challenge shouldn't
     * need a PT to notice and go edit it by hand. Fires once per exercise per completed session:
     * only when every logged set for it met or beat both the prescribed reps and weight (a "clean"
     * session), the next session's target nudges up by one small, safe increment - never a full
     * plan change, and always something the PT can see (and override) synced onto the same row. */
    private suspend fun applyAutoProgression(planId: String, sessionId: String) {
        val planExercises = planExerciseDao.observeForPlan(planId).first()
        if (planExercises.isEmpty()) return
        val loggedSets = setEntryDao.observeForSession(sessionId).first()
        if (loggedSets.isEmpty()) return
        val setsByExercise = loggedSets.groupBy { it.exerciseId }

        for (planExercise in planExercises) {
            val sets = setsByExercise[planExercise.exerciseId] ?: continue
            if (sets.size < planExercise.targetSets) continue
            val metReps = sets.all { it.reps >= planExercise.targetReps }
            val targetWeight = planExercise.targetWeightKg
            val metWeight = targetWeight == null || sets.all { it.weightKg >= targetWeight }
            if (!metReps || !metWeight) continue

            val progressed = if (targetWeight != null) {
                planExercise.copy(targetWeightKg = targetWeight + 2.5, syncStatus = SyncStatus.PENDING_UPDATE)
            } else {
                planExercise.copy(targetReps = planExercise.targetReps + 1, syncStatus = SyncStatus.PENDING_UPDATE)
            }
            planExerciseDao.upsert(progressed)
        }
    }

    /** Best estimated 1RM for [exerciseId] across every session of this user EXCEPT [sessionId]
     * - the "previous personal best" baseline the workout summary compares against. */
    suspend fun bestPreviousE1rm(sessionId: String, exerciseId: String): Double? =
        setEntryDao.bestEstimatedOneRepMaxExcludingSession(sessionId, exerciseId)

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
