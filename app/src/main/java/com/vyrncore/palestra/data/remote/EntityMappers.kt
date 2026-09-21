package com.vyrncore.palestra.data.remote

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.entity.BodyMetricEntity
import com.vyrncore.palestra.data.local.entity.ChatAttachmentType
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.local.entity.ProgramEntity
import com.vyrncore.palestra.data.local.entity.PtNoteEntity
import com.vyrncore.palestra.data.local.entity.SetEntryEntity
import com.vyrncore.palestra.data.local.entity.UserProfileEntity
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.local.entity.WorkoutPlanEntity
import com.vyrncore.palestra.data.local.entity.WorkoutSessionEntity
import com.vyrncore.palestra.data.remote.dto.BodyMetricDto
import com.vyrncore.palestra.data.remote.dto.ChatMessageDto
import com.vyrncore.palestra.data.remote.dto.ExerciseDto
import com.vyrncore.palestra.data.remote.dto.PlanExerciseDto
import com.vyrncore.palestra.data.remote.dto.ProgramDto
import com.vyrncore.palestra.data.remote.dto.PtNoteDto
import com.vyrncore.palestra.data.remote.dto.SetEntryDto
import com.vyrncore.palestra.data.remote.dto.UserProfileDto
import com.vyrncore.palestra.data.remote.dto.WorkoutPlanDto
import com.vyrncore.palestra.data.remote.dto.WorkoutSessionDto
import java.time.Instant

private fun Long.toIso(): String = Instant.ofEpochMilli(this).toString()
private fun String.toEpochMs(): Long = Instant.parse(this).toEpochMilli()

fun UserProfileEntity.toDto() = UserProfileDto(
    id = id, email = email, fullName = fullName, role = role.name, ptId = ptId, injuries = injuries,
    avatarUrl = avatarUrl, bio = bio, heightCm = heightCm, weightKg = weightKg, primaryGoal = primaryGoal,
    inviteCode = inviteCode,
)

fun ExerciseEntity.toDto() = ExerciseDto(id, name, muscleGroup, equipment, notes, createdByUserId, isCustom, imageUrl, difficulty)

fun WorkoutPlanEntity.toDto() = WorkoutPlanDto(
    id, name, description, createdByPtId, assignedToUserId, createdAtEpochMs.toIso(), category, estimatedMinutes,
    programId, weekIndex,
)

fun ProgramEntity.toDto() = ProgramDto(
    id, name, createdByPtId, assignedToUserId, totalWeeks, weeklyIncrementPercent, startEpochMs.toIso()
)

fun PlanExerciseEntity.toDto() = PlanExerciseDto(
    id, planId, exerciseId, orderIndex, targetSets, targetReps, targetWeightKg, restSeconds, notes
)

fun WorkoutSessionEntity.toDto() = WorkoutSessionDto(
    id, planId, userId, startedAtEpochMs.toIso(), endedAtEpochMs?.toIso(), notes
)

fun SetEntryEntity.toDto() = SetEntryDto(
    id, sessionId, exerciseId, setNumber, reps, weightKg, rpe, completedAtEpochMs.toIso()
)

fun BodyMetricEntity.toDto() = BodyMetricDto(
    id, userId, dateEpochMs.toIso(), weightKg, bodyFatPercent, chestCm, waistCm, hipsCm, armCm, thighCm, notes
)

fun UserProfileDto.toEntity() = UserProfileEntity(
    id = id, email = email, fullName = fullName, role = UserRole.valueOf(role), ptId = ptId,
    injuries = injuries, avatarUrl = avatarUrl, bio = bio, heightCm = heightCm, weightKg = weightKg,
    primaryGoal = primaryGoal, inviteCode = inviteCode, syncStatus = SyncStatus.SYNCED,
)

fun ExerciseDto.toEntity() = ExerciseEntity(
    id, name, muscleGroup, equipment, notes, createdByUserId, isCustom, imageUrl, difficulty, SyncStatus.SYNCED
)

fun WorkoutPlanDto.toEntity() = WorkoutPlanEntity(
    id, name, description, createdByPtId, assignedToUserId, createdAt.toEpochMs(), category, estimatedMinutes,
    programId, weekIndex, SyncStatus.SYNCED,
)

fun ProgramDto.toEntity() = ProgramEntity(
    id, name, createdByPtId, assignedToUserId, totalWeeks, weeklyIncrementPercent, startAt.toEpochMs(), SyncStatus.SYNCED
)

fun PlanExerciseDto.toEntity() = PlanExerciseEntity(
    id, planId, exerciseId, orderIndex, targetSets, targetReps, targetWeightKg, restSeconds, notes, SyncStatus.SYNCED
)

fun WorkoutSessionDto.toEntity() = WorkoutSessionEntity(
    id, planId, userId, startedAt.toEpochMs(), endedAt?.toEpochMs(), notes, SyncStatus.SYNCED
)

fun SetEntryDto.toEntity() = SetEntryEntity(
    id, sessionId, exerciseId, setNumber, reps, weightKg, rpe, completedAt.toEpochMs(), SyncStatus.SYNCED
)

fun BodyMetricDto.toEntity() = BodyMetricEntity(
    id, userId, date.toEpochMs(), weightKg, bodyFatPercent, chestCm, waistCm, hipsCm, armCm, thighCm, notes, SyncStatus.SYNCED
)

fun ChatMessageEntity.toDto() = ChatMessageDto(
    id, senderId, recipientId, content, createdAtEpochMs.toIso(), readAtEpochMs?.toIso(),
    attachmentUrl, attachmentName, attachmentType?.name, isDeleted,
)

fun ChatMessageDto.toEntity() = ChatMessageEntity(
    id, senderId, recipientId, content, createdAt.toEpochMs(), readAt?.toEpochMs(),
    attachmentUrl, attachmentName, attachmentType?.let { ChatAttachmentType.valueOf(it) }, isDeleted, SyncStatus.SYNCED
)

fun PtNoteEntity.toDto() = PtNoteDto(id, ptId, clientId, content, createdAtEpochMs.toIso(), updatedAtEpochMs.toIso())

fun PtNoteDto.toEntity() = PtNoteEntity(
    id = id, ptId = ptId, clientId = clientId, content = content,
    createdAtEpochMs = createdAt.toEpochMs(), updatedAtEpochMs = updatedAt.toEpochMs(), syncStatus = SyncStatus.SYNCED,
)
