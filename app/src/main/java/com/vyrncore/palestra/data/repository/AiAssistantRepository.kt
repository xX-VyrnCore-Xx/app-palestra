package com.vyrncore.palestra.data.repository

import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

@Serializable
private data class AiChatStreamChunk(val content: String)

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

    /**
     * Streams the assistant's reply as it's typed out server-side (the Edge Function resolves
     * any tool calls and drip-feeds the finished answer over SSE, rather than the app waiting for
     * one big JSON blob), invoking [onChunk] with each piece of text as it arrives. Returns the
     * full reply, or throws with a user-facing message (e.g. rate limited).
     */
    suspend fun sendMessage(message: String, onChunk: (String) -> Unit): String {
        val response = functions.invoke("ai-chat", body = AiChatRequest(message))
        if (!response.status.isSuccess()) {
            val decoded = runCatching { response.body<AiChatResponse>() }.getOrNull()
            error(decoded?.error ?: "Errore sconosciuto dell'assistente AI")
        }

        val channel = response.bodyAsChannel()
        val full = StringBuilder()
        while (true) {
            val line = channel.readUTF8Line() ?: break
            if (!line.startsWith("data:")) continue
            val data = line.removePrefix("data:").trim()
            if (data.isEmpty() || data == "[DONE]") continue
            val chunk = runCatching { Json.decodeFromString<AiChatStreamChunk>(data) }.getOrNull() ?: continue
            full.append(chunk.content)
            onChunk(chunk.content)
        }
        if (full.isEmpty()) error("Risposta AI vuota")
        return full.toString()
    }
}
