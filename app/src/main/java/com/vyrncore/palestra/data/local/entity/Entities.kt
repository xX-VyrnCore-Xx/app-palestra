package com.vyrncore.palestra.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vyrncore.palestra.data.local.SyncStatus

enum class UserRole { PT, ALLIEVO }

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val ptId: String? = null,
    /** Infortuni/limitazioni fisiche riportate: solo il PT le imposta, visibili solo a lui. */
    val injuries: String? = null,
    /** Foto profilo scelta dall'utente, caricata nel bucket pubblico "avatars". */
    val avatarUrl: String? = null,
    /** Breve biografia mostrata in testa al profilo (max 200 caratteri). */
    val bio: String? = null,
    /** Altezza in centimetri, usata per BMI e statistiche. */
    val heightCm: Int? = null,
    /** Peso corporeo in kg, precompila la registrazione misure corporee. */
    val weightKg: Double? = null,
    /** Obiettivo dichiarato dall'utente (es. "Perdere peso"). */
    val primaryGoal: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String? = null,
    val notes: String? = null,
    val createdByUserId: String? = null,
    val isCustom: Boolean = false,
    /** Optional URL of a demonstrative image/GIF, shown wherever the exercise appears. */
    val imageUrl: String? = null,
    /** Coarse difficulty tier ([ExerciseDifficulty] name), null for legacy/custom rows. */
    val difficulty: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(
    tableName = "workout_plans",
    indices = [Index("createdByPtId"), Index("assignedToUserId"), Index("programId")],
)
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val createdByPtId: String,
    val assignedToUserId: String,
    val createdAtEpochMs: Long,
    val category: String? = null,
    val estimatedMinutes: Int? = null,
    /** Set when this plan is one week of a multi-week [ProgramEntity], null for a standalone plan. */
    val programId: String? = null,
    /** 1-based week number within the program, null for a standalone plan. */
    val weekIndex: Int? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(
    tableName = "programs",
    indices = [Index("createdByPtId"), Index("assignedToUserId")],
)
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdByPtId: String,
    val assignedToUserId: String,
    val totalWeeks: Int,
    /** Percent by which target weight is scaled up on each successive week's plan (e.g. 2.5 = +2.5%/week). */
    val weeklyIncrementPercent: Double,
    val startEpochMs: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(
    tableName = "plan_exercises",
    indices = [Index("planId"), Index("exerciseId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlanExerciseEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Double? = null,
    val restSeconds: Int = 90,
    /** Quick freeform note from the PT (technique cue, tempo, substitution) shown to the allievo
     * right on the exercise card during the workout - no need to open chat to relay it. */
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(
    tableName = "workout_sessions",
    indices = [Index("userId"), Index("planId")],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val planId: String? = null,
    val userId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(
    tableName = "set_entries",
    indices = [Index("sessionId"), Index("exerciseId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SetEntryEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double? = null,
    val completedAtEpochMs: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(tableName = "body_metrics", indices = [Index("userId")])
data class BodyMetricEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateEpochMs: Long,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

enum class ChatAttachmentType { IMAGE, FILE }

@Entity(tableName = "chat_messages", indices = [Index("senderId"), Index("recipientId")])
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val createdAtEpochMs: Long,
    val readAtEpochMs: Long? = null,
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val attachmentType: ChatAttachmentType? = null,
    /** Soft-delete flag: content/attachment are cleared but the row stays so the peer sees a
     * "message deleted" tombstone instead of a confusing gap in the conversation. */
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

@Entity(tableName = "pt_notes", indices = [Index("ptId"), Index("clientId")])
data class PtNoteEntity(
    @PrimaryKey val id: String,
    val ptId: String,
    val clientId: String,
    val content: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
