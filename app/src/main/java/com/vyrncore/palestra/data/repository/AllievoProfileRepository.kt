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
    @SerialName("pain_injuries") val painInjuries: String? = null,
    val nutrition: String? = null,
    val lifestyle: String? = null,
    val goals: String? = null,
    @SerialName("completed_onboarding") val completedOnboarding: Boolean = false,
)

/**
 * The allievo's private self-assessment (pain/injuries, diet, lifestyle, goals), collected once
 * in the Welcome flow and editable later from Profile. Strictly owner-only: RLS on
 * allievo_private_profiles has no PT exception (unlike profiles.injuries), and nothing in this
 * app - least of all the AI assistant prompt in AiAssistantRepository - ever reads this table.
 * Keep it that way: never join or forward this data anywhere else.
 */
@Singleton
class AllievoProfileRepository @Inject constructor(
    private val postgrest: Postgrest,
) {
    suspend fun fetch(userId: String): AllievoPrivateProfile? = runCatching {
        postgrest.from("allievo_private_profiles").select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<AllievoPrivateProfile>()
    }.getOrNull()

    suspend fun save(profile: AllievoPrivateProfile) {
        postgrest.from("allievo_private_profiles").upsert(profile)
    }
}
