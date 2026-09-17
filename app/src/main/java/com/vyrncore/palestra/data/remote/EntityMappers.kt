package com.vyrncore.palestra.data.remote

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.entity.BodyMetricEntity
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.local.entity.SetEntryEntity
import com.vyrncore.palestra.data.local.entity.UserProfileEntity
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.local.entity.WorkoutPlanEntity
import com.vyrncore.palestra.data.local.entity.WorkoutSessionEntity
import com.vyrncore.palestra.data.remote.dto.BodyMetricDto
import com.vyrncore.palestra.data.remote.dto.ExerciseDto
import com.vyrncore.palestra.data.remote.dto.PlanExerciseDto
import com.vyrncore.palestra.data.remote.dto.SetEntryDto
import com.vyrncore.palestra.data.remote.dto.UserProfileDto
import com.vyrncore.palestra.data.remote.dto.WorkoutPlanDto
import com.vyrncore.palestra.data.remote.dto.WorkoutSessionDto
import java.time.Instant

private fun Long.toIso(): String = Instant.ofEpochMilli(this).toString()
private fun String.toEpochMs(): Long = Instant.parse(this).toEpochMilli()

fun UserProfileEntity.toDto() = UserProfileDto(id, email, fullName, role.name, ptId)

fun ExerciseEntity.toDto() = ExerciseDto(id, name, muscleGroup, equipment, notes, createdByUserId, isCustom)

fun WorkoutPlanEntity.toDto() = WorkoutPlanDto(
    id, name, description, createdByPtId, assignedToUserId, createdAtEpochMs.toIso()
)

fun PlanExerciseEntity.toDto() = PlanExerciseDto(
    id, planId, exerciseId, orderIndex, targetSets, targetReps, targetWeightKg, restSeconds
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

fun UserProfileDto.toEntity() = UserProfileEntity(id, email, fullName, UserRole.valueOf(role), ptId, SyncStatus.SYNCED)

fun ExerciseDto.toEntity() = ExerciseEntity(id, name, muscleGroup, equipment, notes, createdByUserId, isCustom, SyncStatus.SYNCED)

fun WorkoutPlanDto.toEntity() = WorkoutPlanEntity(
    id, name, description, createdByPtId, assignedToUserId, createdAt.toEpochMs(), SyncStatus.SYNCED
)

fun PlanExerciseDto.toEntity() = PlanExerciseEntity(
    id, planId, exerciseId, orderIndex, targetSets, targetReps, targetWeightKg, restSeconds, SyncStatus.SYNCED
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
