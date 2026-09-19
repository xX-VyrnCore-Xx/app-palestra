package com.vyrncore.palestra.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
    @SerialName("pt_id") val ptId: String? = null,
    val injuries: String? = null,
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    @SerialName("muscle_group") val muscleGroup: String,
    val equipment: String? = null,
    val notes: String? = null,
    @SerialName("created_by_user_id") val createdByUserId: String? = null,
    @SerialName("is_custom") val isCustom: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class WorkoutPlanDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("created_by_pt_id") val createdByPtId: String,
    @SerialName("assigned_to_user_id") val assignedToUserId: String,
    @SerialName("created_at") val createdAt: String,
    val category: String? = null,
    @SerialName("estimated_minutes") val estimatedMinutes: Int? = null,
    @SerialName("program_id") val programId: String? = null,
    @SerialName("week_index") val weekIndex: Int? = null,
)

@Serializable
data class ProgramDto(
    val id: String,
    val name: String,
    @SerialName("created_by_pt_id") val createdByPtId: String,
    @SerialName("assigned_to_user_id") val assignedToUserId: String,
    @SerialName("total_weeks") val totalWeeks: Int,
    @SerialName("weekly_increment_percent") val weeklyIncrementPercent: Double,
    @SerialName("start_at") val startAt: String,
)

@Serializable
data class PlanExerciseDto(
    val id: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("target_sets") val targetSets: Int,
    @SerialName("target_reps") val targetReps: Int,
    @SerialName("target_weight_kg") val targetWeightKg: Double? = null,
    @SerialName("rest_seconds") val restSeconds: Int = 90,
)

@Serializable
data class WorkoutSessionDto(
    val id: String,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    val notes: String? = null,
)

@Serializable
data class SetEntryDto(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("set_number") val setNumber: Int,
    val reps: Int,
    @SerialName("weight_kg") val weightKg: Double,
    val rpe: Double? = null,
    @SerialName("completed_at") val completedAt: String,
)

@Serializable
data class BodyMetricDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("body_fat_percent") val bodyFatPercent: Double? = null,
    @SerialName("chest_cm") val chestCm: Double? = null,
    @SerialName("waist_cm") val waistCm: Double? = null,
    @SerialName("hips_cm") val hipsCm: Double? = null,
    @SerialName("arm_cm") val armCm: Double? = null,
    @SerialName("thigh_cm") val thighCm: Double? = null,
    val notes: String? = null,
)

@Serializable
data class ChatMessageDto(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("attachment_name") val attachmentName: String? = null,
    @SerialName("attachment_type") val attachmentType: String? = null,
)

@Serializable
data class PtNoteDto(
    val id: String,
    @SerialName("pt_id") val ptId: String,
    @SerialName("client_id") val clientId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
