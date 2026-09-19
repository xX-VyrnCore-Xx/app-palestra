package com.vyrncore.palestra.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AllievoPrivateProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("experience_level") val experienceLevel: String? = null,
    @SerialName("training_days") val trainingDays: String? = null,
    @SerialName("activity_level") val activityLevel: String? = null,
    @SerialName("primary_goal") val primaryGoal: String? = null,
    @SerialName("pain_injuries") val painInjuries: String? = null,
    val nutrition: String? = null,
    val lifestyle: String? = null,
    val goals: String? = null,
    @SerialName("completed_onboarding") val completedOnboarding: Boolean = false,
)

/**
 * The allievo's self-assessment (experience, training days, goal, lifestyle, pain/injuries,
 * diet), collected once in the Welcome flow and editable later from Profile. RLS on
 * allievo_private_profiles lets the owner read/write it and their own PT read it (to tailor a
 * plan), but nothing else - least of all the AI assistant prompt in AiAssistantRepository - ever
 * reads this table. Keep it that way: never join or forward this data into the AI path.
 */
@Singleton
class AllievoProfileRepository @Inject constructor(
    private val postgrest: Postgrest,
) {
    /** [userId] is the allievo whose profile is being read - the caller may be that allievo or
     * their own PT (RLS enforces both, nobody else can read a row this way). */
    suspend fun fetch(userId: String): AllievoPrivateProfile? = runCatching {
        postgrest.from("allievo_private_profiles").select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<AllievoPrivateProfile>()
    }.getOrNull()

    suspend fun save(profile: AllievoPrivateProfile) {
        postgrest.from("allievo_private_profiles").upsert(profile)
    }
}
