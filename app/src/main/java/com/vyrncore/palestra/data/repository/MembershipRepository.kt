package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.remote.dto.MembershipDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

enum class MembershipStatus { NONE, ACTIVE, EXPIRING_SOON, EXPIRED }

data class MembershipInfo(
    val planLabel: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val notes: String?,
) {
    /** "Expiring soon" starts a week out - enough runway for the allievo to renew (or the PT to
     * nudge them) before it actually lapses, without crying wolf too early. */
    val status: MembershipStatus
        get() {
            val today = LocalDate.now()
            return when {
                endDate.isBefore(today) -> MembershipStatus.EXPIRED
                !endDate.isAfter(today.plusDays(7)) -> MembershipStatus.EXPIRING_SOON
                else -> MembershipStatus.ACTIVE
            }
        }

    val daysUntilEnd: Long get() = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate)
}

@Serializable
private data class MembershipUpsert(
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("plan_label") val planLabel: String?,
    @kotlinx.serialization.SerialName("start_date") val startDate: String,
    @kotlinx.serialization.SerialName("end_date") val endDate: String,
    val notes: String?,
)

/**
 * Membership/subscription tracking - remote-only (like the weekly ranking), since it's PT-set,
 * low-frequency data both sides just need to *see* rather than something that must work offline.
 * RLS already scopes reads/writes to "your own" (allievo) or "your own clients'" (PT); see the
 * memberships table migration for the policies.
 */
@Singleton
class MembershipRepository @Inject constructor(
    private val postgrest: Postgrest,
) {
    /** The most recent membership row for [userId], or null if the PT has never recorded one. */
    suspend fun getCurrentMembership(userId: String): MembershipInfo? {
        val rows = postgrest.from("memberships").select {
            filter { eq("user_id", userId) }
            order("end_date", Order.DESCENDING)
            limit(1)
        }.decodeList<MembershipDto>()
        val dto = rows.firstOrNull() ?: return null
        return MembershipInfo(
            planLabel = dto.planLabel,
            startDate = LocalDate.parse(dto.startDate),
            endDate = LocalDate.parse(dto.endDate),
            notes = dto.notes,
        )
    }

    /** A PT sets or replaces a client's membership window - always a fresh row rather than
     * editing history in place, so a renewal keeps a trail of past periods instead of overwriting
     * when the previous one ended. */
    suspend fun recordMembership(
        userId: String,
        planLabel: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        notes: String?,
    ) {
        postgrest.from("memberships").insert(
            MembershipUpsert(
                userId = userId,
                planLabel = planLabel?.takeIf { it.isNotBlank() },
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                notes = notes?.takeIf { it.isNotBlank() },
            ),
        )
    }
}
