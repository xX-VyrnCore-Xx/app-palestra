package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.remote.dto.MembershipDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
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

/**
 * Membership/subscription tracking - read-only in the app and remote-only (like the weekly
 * ranking): the PT records it from the web management app, the allievo just needs to *see* it.
 * RLS scopes reads to the allievo's own rows; see the memberships table migration.
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
}
