package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.local.ExerciseCatalogSeed
import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.ExerciseDao
import com.vyrncore.palestra.data.local.dao.MuscleGroupVolume
import com.vyrncore.palestra.data.local.dao.PlanExerciseDao
import com.vyrncore.palestra.data.local.dao.SessionSummary
import com.vyrncore.palestra.data.local.dao.SetEntryDao
import com.vyrncore.palestra.data.local.dao.WeeklyVolume
import com.vyrncore.palestra.data.local.dao.WorkoutPlanDao
import com.vyrncore.palestra.data.local.dao.WorkoutSessionDao
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.local.entity.SetEntryEntity
import com.vyrncore.palestra.data.local.entity.WorkoutPlanEntity
import com.vyrncore.palestra.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val setEntryDao: SetEntryDao,
) {
    // Exercise catalog
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    suspend fun seedCatalogIfNeeded() {
        if (exerciseDao.count() == 0) {
            exerciseDao.upsertAll(ExerciseCatalogSeed.exercises)
        }
    }

    suspend fun addCustomExercise(name: String, muscleGroup: String, createdByUserId: String) {
        exerciseDao.upsert(
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                muscleGroup = muscleGroup,
                createdByUserId = createdByUserId,
                isCustom = true,
                syncStatus = SyncStatus.PENDING_CREATE,
            )
        )
    }

    // Plans (created by PT, assigned to a client)
    fun observePlansForUser(userId: String): Flow<List<WorkoutPlanEntity>> = workoutPlanDao.observeForUser(userId)

    fun observePlansCreatedByPt(ptId: String): Flow<List<WorkoutPlanEntity>> = workoutPlanDao.observeCreatedByPt(ptId)

    fun observePlanExercises(planId: String): Flow<List<PlanExerciseEntity>> = planExerciseDao.observeForPlan(planId)

    suspend fun createPlan(
        name: String,
        description: String?,
        createdByPtId: String,
        assignedToUserId: String,
        exercises: List<PlanExerciseEntity>,
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

    suspend fun logSet(sessionId: String, exerciseId: String, setNumber: Int, reps: Int, weightKg: Double, rpe: Double?) {
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
    }
}
