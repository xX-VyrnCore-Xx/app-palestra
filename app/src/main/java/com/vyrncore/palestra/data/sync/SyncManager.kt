package com.vyrncore.palestra.data.sync

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.BodyMetricDao
import com.vyrncore.palestra.data.local.dao.ChatMessageDao
import com.vyrncore.palestra.data.local.dao.ExerciseDao
import com.vyrncore.palestra.data.local.dao.PlanExerciseDao
import com.vyrncore.palestra.data.local.dao.PlanTemplateDao
import com.vyrncore.palestra.data.local.dao.PlanTemplateExerciseDao
import com.vyrncore.palestra.data.local.dao.ProgramDao
import com.vyrncore.palestra.data.local.dao.PtNoteDao
import com.vyrncore.palestra.data.local.dao.SetEntryDao
import com.vyrncore.palestra.data.local.dao.UserProfileDao
import com.vyrncore.palestra.data.local.dao.WorkoutPlanDao
import com.vyrncore.palestra.data.local.dao.WorkoutSessionDao
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.remote.dto.BodyMetricDto
import com.vyrncore.palestra.data.remote.dto.ChatMessageDto
import com.vyrncore.palestra.data.remote.dto.ExerciseDto
import com.vyrncore.palestra.data.remote.dto.PlanExerciseDto
import com.vyrncore.palestra.data.remote.dto.PlanTemplateDto
import com.vyrncore.palestra.data.remote.dto.PlanTemplateExerciseDto
import com.vyrncore.palestra.data.remote.dto.ProgramDto
import com.vyrncore.palestra.data.remote.dto.PtNoteDto
import com.vyrncore.palestra.data.remote.dto.SetEntryDto
import com.vyrncore.palestra.data.remote.dto.UserProfileDto
import com.vyrncore.palestra.data.remote.dto.WorkoutPlanDto
import com.vyrncore.palestra.data.remote.dto.WorkoutSessionDto
import com.vyrncore.palestra.data.remote.toDto
import com.vyrncore.palestra.data.remote.toEntity
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SendPushRequest(
    val recipientId: String,
    val type: String,
    val title: String,
    val body: String,
)

/**
 * Keeps the local Room database and Supabase in sync in both directions:
 * - [pushLocalChanges] sends every locally pending row (created/updated offline, e.g. mid-workout
 *   with no signal) up to Supabase. Each push is a plain upsert, so re-running a partially failed
 *   sync is always safe.
 * - [pullRemoteChanges] fetches what the signed-in user (and, for a PT, their clients) can see
 *   remotely and upserts it locally. Without this, a plan a PT assigns from their own phone would
 *   never reach the client's device.
 *
 * [SyncWorker] runs both whenever connectivity returns.
 */
@Singleton
class SyncManager @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val functions: Functions,
    private val userProfileDao: UserProfileDao,
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val setEntryDao: SetEntryDao,
    private val bodyMetricDao: BodyMetricDao,
    private val chatMessageDao: ChatMessageDao,
    private val ptNoteDao: PtNoteDao,
    private val programDao: ProgramDao,
    private val planTemplateDao: PlanTemplateDao,
    private val planTemplateExerciseDao: PlanTemplateExerciseDao,
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
            if (entity.syncStatus == SyncStatus.PENDING_CREATE) notifyPlanAssigned(entity.assignedToUserId, entity.name)
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
        ptNoteDao.getPendingSync().forEach { entity ->
            postgrest.from("pt_notes").upsert(entity.toDto())
            ptNoteDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        programDao.getPendingSync().forEach { entity ->
            postgrest.from("programs").upsert(entity.toDto())
            programDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        planTemplateDao.getPendingSync().forEach { entity ->
            postgrest.from("plan_templates").upsert(entity.toDto())
            planTemplateDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
        planTemplateExerciseDao.getPendingSync().forEach { entity ->
            postgrest.from("plan_template_exercises").upsert(entity.toDto())
            planTemplateExerciseDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
        }
    }

    /** Best-effort server push so a newly assigned plan reaches the client even if the app isn't running. */
    private suspend fun notifyPlanAssigned(assignedToUserId: String, planName: String) {
        runCatching {
            functions.invoke(
                "send-push",
                body = SendPushRequest(
                    recipientId = assignedToUserId,
                    type = "plan_update",
                    title = "Nuova scheda assegnata",
                    body = planName,
                ),
            )
        }
    }

    private suspend fun pullRemoteChanges(userId: String) {
        val ownProfile = runCatching {
            postgrest.from("profiles").select { filter { eq("id", userId) } }.decodeSingle<UserProfileDto>()
        }.getOrNull() ?: return
        userProfileDao.upsert(ownProfile.toEntity())

        val clients = if (ownProfile.role == UserRole.PT.name) {
            runCatching {
                postgrest.from("profiles").select { filter { eq("pt_id", userId) } }.decodeList<UserProfileDto>()
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        clients.forEach { userProfileDao.upsert(it.toEntity()) }
        val relevantUserIds = listOf(userId) + clients.map { it.id }

        runCatching {
            postgrest.from("exercises").select().decodeList<ExerciseDto>()
        }.getOrDefault(emptyList()).forEach { exerciseDao.upsert(it.toEntity()) }

        val plansAssigned = runCatching {
            postgrest.from("workout_plans").select { filter { isIn("assigned_to_user_id", relevantUserIds) } }
                .decodeList<WorkoutPlanDto>()
        }.getOrDefault(emptyList())
        val plansCreated = runCatching {
            postgrest.from("workout_plans").select { filter { eq("created_by_pt_id", userId) } }
                .decodeList<WorkoutPlanDto>()
        }.getOrDefault(emptyList())
        val plans = (plansAssigned + plansCreated).distinctBy { it.id }
        plans.forEach { workoutPlanDao.upsert(it.toEntity()) }

        val planIds = plans.map { it.id }
        if (planIds.isNotEmpty()) {
            runCatching {
                postgrest.from("plan_exercises").select { filter { isIn("plan_id", planIds) } }
                    .decodeList<PlanExerciseDto>()
            }.getOrDefault(emptyList()).forEach { planExerciseDao.upsert(it.toEntity()) }
        }

        val sessions = runCatching {
            postgrest.from("workout_sessions").select { filter { isIn("user_id", relevantUserIds) } }
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
            postgrest.from("body_metrics").select { filter { isIn("user_id", relevantUserIds) } }
                .decodeList<BodyMetricDto>()
        }.getOrDefault(emptyList()).forEach { bodyMetricDao.upsert(it.toEntity()) }

        runCatching {
            postgrest.from("messages").select {
                filter { or { eq("sender_id", userId); eq("recipient_id", userId) } }
            }.decodeList<ChatMessageDto>()
        }.getOrDefault(emptyList()).forEach { chatMessageDao.upsert(it.toEntity()) }

        if (ownProfile.role == UserRole.PT.name) {
            runCatching {
                postgrest.from("pt_notes").select { filter { eq("pt_id", userId) } }
                    .decodeList<PtNoteDto>()
            }.getOrDefault(emptyList()).forEach { ptNoteDao.upsert(it.toEntity()) }
        }

        val programsAssigned = runCatching {
            postgrest.from("programs").select { filter { isIn("assigned_to_user_id", relevantUserIds) } }
                .decodeList<ProgramDto>()
        }.getOrDefault(emptyList())
        val programsCreated = runCatching {
            postgrest.from("programs").select { filter { eq("created_by_pt_id", userId) } }
                .decodeList<ProgramDto>()
        }.getOrDefault(emptyList())
        (programsAssigned + programsCreated).distinctBy { it.id }.forEach { programDao.upsert(it.toEntity()) }

        if (ownProfile.role == UserRole.PT.name) {
            val templates = runCatching {
                postgrest.from("plan_templates").select { filter { eq("pt_id", userId) } }
                    .decodeList<PlanTemplateDto>()
            }.getOrDefault(emptyList())
            templates.forEach { planTemplateDao.upsert(it.toEntity()) }

            val templateIds = templates.map { it.id }
            if (templateIds.isNotEmpty()) {
                runCatching {
                    postgrest.from("plan_template_exercises").select { filter { isIn("template_id", templateIds) } }
                        .decodeList<PlanTemplateExerciseDto>()
                }.getOrDefault(emptyList()).forEach { planTemplateExerciseDao.upsert(it.toEntity()) }
            }
        }
    }
}
