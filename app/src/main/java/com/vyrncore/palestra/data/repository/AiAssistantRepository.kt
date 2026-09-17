package com.vyrncore.palestra.data.repository

import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AiMessage(
    val role: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
private data class AiChatRequest(val message: String)

@Serializable
private data class AiChatResponse(val reply: String? = null, val error: String? = null)

/**
 * Talks to the ai-chat Supabase Edge Function, which is the only place the NVIDIA NIM API key
 * lives — it never ships in the app. The Edge Function picks the PT or Allievo system prompt
 * itself based on the caller's own profile role, so this repository is role-agnostic.
 */
@Singleton
class AiAssistantRepository @Inject constructor(
    private val functions: Functions,
    private val postgrest: Postgrest,
) {
    suspend fun history(): List<AiMessage> =
        postgrest.from("ai_messages").select().decodeList<AiMessage>()
            .sortedBy { it.createdAt }

    /** Returns the assistant's reply, or throws with a user-facing message (e.g. rate limited). */
    suspend fun sendMessage(message: String): String {
        val response = functions.invoke("ai-chat", body = AiChatRequest(message))
        val decoded = response.body<AiChatResponse>()
        return decoded.reply ?: error(decoded.error ?: "Errore sconosciuto dell'assistente AI")
    }
}
