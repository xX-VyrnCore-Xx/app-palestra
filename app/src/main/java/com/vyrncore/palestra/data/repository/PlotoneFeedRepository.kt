package com.vyrncore.palestra.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PlotoneFeedPost(
    @SerialName("user_id") val userId: String,
    @SerialName("pt_id") val ptId: String,
    @SerialName("display_name") val displayName: String,
    val message: String,
    @SerialName("created_at") val createdAt: String? = null,
)

/**
 * Read-mostly activity feed shared by every allievo of the same PT (and the PT themself), backed
 * directly by Postgrest with no local Room cache - like [WorkoutRepository.fetchWeeklyRanking],
 * a live re-fetch on screen open/refresh is simple and fresh enough for this feature.
 */
@Singleton
class PlotoneFeedRepository @Inject constructor(
    private val postgrest: Postgrest,
) {
    suspend fun fetchFeed(ptId: String, limit: Long = 20): List<PlotoneFeedPost> =
        runCatching {
            postgrest.from("plotone_feed_posts")
                .select {
                    filter { eq("pt_id", ptId) }
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<PlotoneFeedPost>()
        }.getOrDefault(emptyList())

    suspend fun postWorkoutCompleted(userId: String, ptId: String, displayName: String, exerciseCount: Int) {
        runCatching {
            postgrest.from("plotone_feed_posts").insert(
                PlotoneFeedPost(
                    userId = userId,
                    ptId = ptId,
                    displayName = displayName.substringBefore(' '),
                    message = "ha completato una missione ($exerciseCount esercizi)",
                ),
            )
        }
    }
}
