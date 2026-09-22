package com.vyrncore.palestra.data.sync

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.BodyMetricDao
import com.vyrncore.palestra.data.local.dao.ChatMessageDao
import com.vyrncore.palestra.data.local.dao.ExerciseDao
import com.vyrncore.palestra.data.local.dao.PlanExerciseDao
import com.vyrncore.palestra.data.local.dao.ProgramDao
import com.vyrncore.palestra.data.local.dao.SetEntryDao
import com.vyrncore.palestra.data.local.dao.UserProfileDao
import com.vyrncore.palestra.data.local.dao.WorkoutPlanDao
import com.vyrncore.palestra.data.local.dao.WorkoutSessionDao
import com.vyrncore.palestra.data.remote.dto.BodyMetricDto
import com.vyrncore.palestra.data.remote.dto.ChatMessageDto
import com.vyrncore.palestra.data.remote.dto.ExerciseDto
import com.vyrncore.palestra.data.remote.dto.PlanExerciseDto
import com.vyrncore.palestra.data.remote.dto.ProgramDto
import com.vyrncore.palestra.data.remote.dto.SetEntryDto
import com.vyrncore.palestra.data.remote.dto.UserProfileDto
import com.vyrncore.palestra.data.remote.dto.WorkoutPlanDto
import com.vyrncore.palestra.data.remote.dto.WorkoutSessionDto
import com.vyrncore.palestra.data.remote.toDto
import com.vyrncore.palestra.data.remote.toEntity
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the local Room database and Supabase in sync in both directions:
 * - [pushLocalChanges] sends every locally pending row (created/updated offline, e.g. mid-workout
 *   with no signal) up to Supabase. Each push is a plain upsert, so re-running a partially failed
 *   sync is always safe.
 * - [pullRemoteChanges] fetches what the signed-in allievo can see remotely and upserts it
 *   locally - this is how plans/programs a PT assigns from the web management app reach the
 *   allievo's device.
 *
 * [SyncWorker] runs both whenever connectivity returns.
 */
@Singleton
class SyncManager @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val userProfileDao: UserProfileDao,
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val setEntryDao: SetEntryDao,
    private val bodyMetricDao: BodyMetricDao,
    private val chatMessageDao: ChatMessageDao,
    private val programDao: ProgramDao,
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedAtEpochMs = MutableStateFlow<Long?>(null)
    val lastSyncedAtEpochMs: StateFlow<Long?> = _lastSyncedAtEpochMs.asStateFlow()

    suspend fun syncAll() {
        _isSyncing.value = true
        try {
            pushLocalChanges()
            auth.currentUserOrNull()?.id?.let { pullRemoteChanges(it) }
            _lastSyncedAtEpochMs.value = System.currentTimeMillis()
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun pushLocalChanges() {
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
        chatMessageDao.getPendingSync().forEach { entity ->
            postgrest.from("messages").upsert(entity.toDto())
            chatMessageDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
    }

    private suspend fun pullRemoteChanges(userId: String) {
        val ownProfile = runCatching {
            postgrest.from("profiles").select { filter { eq("id", userId) } }.decodeSingle<UserProfileDto>()
        }.getOrNull() ?: return
        userProfileDao.upsert(ownProfile.toEntity())

        // The allievo's own PT profile (name/avatar shown in chat and search).
        ownProfile.ptId?.let { ptId ->
            runCatching {
                postgrest.from("profiles").select { filter { eq("id", ptId) } }.decodeSingle<UserProfileDto>()
            }.getOrNull()?.let { userProfileDao.upsert(it.toEntity()) }
        }

        runCatching {
            postgrest.from("exercises").select().decodeList<ExerciseDto>()
        }.getOrDefault(emptyList()).forEach { exerciseDao.upsert(it.toEntity()) }

        val plans = runCatching {
            postgrest.from("workout_plans").select { filter { eq("assigned_to_user_id", userId) } }
                .decodeList<WorkoutPlanDto>()
        }.getOrDefault(emptyList())
        plans.forEach { workoutPlanDao.upsert(it.toEntity()) }

        val planIds = plans.map { it.id }
        if (planIds.isNotEmpty()) {
            runCatching {
                postgrest.from("plan_exercises").select { filter { isIn("plan_id", planIds) } }
                    .decodeList<PlanExerciseDto>()
            }.getOrDefault(emptyList()).forEach { planExerciseDao.upsert(it.toEntity()) }
        }

        val sessions = runCatching {
            postgrest.from("workout_sessions").select { filter { eq("user_id", userId) } }
                .decodeList<WorkoutSessionDto>()
        }.getOrDefault(emptyList())
        sessions.forEach { workoutSessionDao.upsert(it.toEntity()) }

        val sessionIds = sessions.map { it.id }
        if (sessionIds.isNotEmpty()) {
            runCatching {
                postgrest.from("set_entries").select { filter { isIn("session_id", sessionIds) } }
                    .decodeList<SetEntryDto>()
            }.getOrDefault(emptyList()).forEach { setEntryDao.upsert(it.toEntity()) }
        }

        runCatching {
            postgrest.from("body_metrics").select { filter { eq("user_id", userId) } }
                .decodeList<BodyMetricDto>()
        }.getOrDefault(emptyList()).forEach { bodyMetricDao.upsert(it.toEntity()) }

        runCatching {
            postgrest.from("messages").select {
                filter { or { eq("sender_id", userId); eq("recipient_id", userId) } }
            }.decodeList<ChatMessageDto>()
        }.getOrDefault(emptyList()).forEach { chatMessageDao.upsert(it.toEntity()) }

        runCatching {
            postgrest.from("programs").select { filter { eq("assigned_to_user_id", userId) } }
                .decodeList<ProgramDto>()
        }.getOrDefault(emptyList()).forEach { programDao.upsert(it.toEntity()) }
    }
}
