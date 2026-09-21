package com.vyrncore.palestra.data.repository

import io.github.jan.supabase.functions.Functions
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class PlanBuilderRequest(val goal: String, val clientId: String)

@Serializable
data class AiPlanExerciseSuggestion(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
)

@Serializable
data class AiPlanSuggestion(
    val planName: String? = null,
    val category: String? = null,
    val exercises: List<AiPlanExerciseSuggestion> = emptyList(),
    val error: String? = null,
)

/**
 * Talks to the ai-plan-builder Edge Function: a PT describes a goal in plain Italian and gets
 * back a draft of exercises picked from the real catalog, to review and adjust in the Plan Editor
 * before saving - it never assigns anything to the client on its own.
 */
@Singleton
class AiPlanBuilderRepository @Inject constructor(
    private val functions: Functions,
) {
    suspend fun generatePlan(goal: String, clientId: String): AiPlanSuggestion {
        val response = functions.invoke("ai-plan-builder", body = PlanBuilderRequest(goal, clientId))
        val decoded = response.body<AiPlanSuggestion>()
        if (!response.status.isSuccess()) {
            error(decoded.error ?: "Errore sconosciuto dell'assistente AI")
        }
        return decoded
    }
}
