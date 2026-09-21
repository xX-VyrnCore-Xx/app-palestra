package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.UserProfileDao
import com.vyrncore.palestra.data.local.entity.UserProfileEntity
import com.vyrncore.palestra.data.local.entity.UserRole
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import com.vyrncore.palestra.data.remote.dto.UserProfileDto
import com.vyrncore.palestra.data.remote.toEntity
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class InviteCodeParams(val code: String)

@Serializable
data class PtInviteMatch(
    val id: String,
    @SerialName("full_name") val fullName: String,
)

@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val storage: Storage,
    private val userProfileDao: UserProfileDao,
) {
    private companion object {
        const val AVATARS_BUCKET = "avatars"
    }
    val currentUserId: String?
        get() = auth.currentUserOrNull()?.id

    /** [ptInviteCode] is the short code the allievo was given by their PT (see [resolvePtInviteCode])
     * - never a raw PT id, which nobody should be pasting by hand. Resolved after sign-in succeeds,
     * since the lookup function requires an authenticated caller; an unknown/blank code just leaves
     * the account unlinked rather than failing the whole registration. */
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        role: UserRole,
        ptInviteCode: String?,
        heightCm: Int?,
        weightKg: Double?,
        primaryGoal: String?,
    ) {
        if (password.length < 8) throw IllegalArgumentException("La password deve essere di almeno 8 caratteri.")

        runCatching {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }.onFailure { throw it }

        val userId = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Registrazione fallita: utente non trovato.")
        val resolvedPtId = ptInviteCode?.takeIf { it.isNotBlank() }?.let { resolvePtInviteCode(it)?.id }
        val profile = UserProfileEntity(
            id = userId,
            email = email,
            fullName = fullName,
            role = role,
            ptId = resolvedPtId,
            heightCm = heightCm,
            weightKg = weightKg,
            primaryGoal = primaryGoal,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
        userProfileDao.upsert(profile)
    }

    suspend fun signIn(email: String, password: String) {
        runCatching {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }.onFailure { throw it }

        val userId = auth.currentUserOrNull()?.id ?: return
        runCatching {
            postgrest.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingle<UserProfileDto>()
        }.onSuccess { dto -> userProfileDao.upsert(dto.toEntity()) }
    }

    /** Sends the Supabase Auth password-reset email - "Password dimenticata?" on the Login screen
     * used to call a default no-op callback that wasn't wired to anything. */
    suspend fun sendPasswordResetEmail(email: String) {
        auth.resetPasswordForEmail(email)
    }

    /** Clears this device's FCM token from the outgoing user's profile before signing out - without
     * this, a device shared between accounts (PT signs out, allievo signs in) keeps delivering push
     * notifications for BOTH accounts, since the token would otherwise stay registered on the old
     * profile row until it happens to be overwritten by a future login. */
    suspend fun signOut() {
        currentUserId?.let { userId ->
            runCatching {
                postgrest.from("profiles").update(mapOf("fcm_token" to null)) {
                    filter { eq("id", userId) }
                }
            }
        }
        auth.signOut()
    }

    fun observeProfile(userId: String): Flow<UserProfileEntity?> = userProfileDao.observeById(userId)

    fun observeClients(ptId: String): Flow<List<UserProfileEntity>> = userProfileDao.observeClientsOfPt(ptId)

    /** PT-only: records injuries/limitations for a client so they surface wherever the PT builds a plan. */
    suspend fun updateInjuries(clientId: String, injuries: String?) {
        val current = userProfileDao.observeById(clientId).firstOrNull() ?: return
        userProfileDao.upsert(
            current.copy(injuries = injuries?.takeIf { it.isNotBlank() }, syncStatus = SyncStatus.PENDING_UPDATE)
        )
    }

    /** Registers this device's FCM token so the backend can push notifications to it. */
    suspend fun updateFcmToken(userId: String, token: String) {
        runCatching {
            postgrest.from("profiles").update(mapOf("fcm_token" to token)) {
                filter { eq("id", userId) }
            }
        }
    }

    /** Resolves a PT's short invite code (as typed by an allievo) to their id/name, so linking
     * to a PT during registration - or later, from the profile screen - never requires pasting a
     * raw UUID. Returns null for an unknown/mistyped code. */
    suspend fun resolvePtInviteCode(code: String): PtInviteMatch? {
        if (code.isBlank()) return null
        return runCatching {
            postgrest.rpc("resolve_pt_invite_code", InviteCodeParams(code.trim()))
                .decodeSingleOrNull<PtInviteMatch>()
        }.getOrNull()
    }

    /** A PT's own shareable code, generating and persisting one the first time it's needed (e.g.
     * an existing PT who registered before this feature existed). Retries a handful of times on
     * the rare collision against another PT's code, since it's random rather than derived. */
    suspend fun getOrCreateInviteCode(userId: String): String? {
        val existing = userProfileDao.observeById(userId).firstOrNull()?.inviteCode
        if (!existing.isNullOrBlank()) return existing

        repeat(5) {
            val candidate = randomInviteCode()
            val result = runCatching {
                postgrest.from("profiles").update(mapOf("invite_code" to candidate)) {
                    filter { eq("id", userId) }
                }
            }
            if (result.isSuccess) {
                val current = userProfileDao.observeById(userId).firstOrNull()
                if (current != null) userProfileDao.upsert(current.copy(inviteCode = candidate))
                return candidate
            }
        }
        return null
    }

    private fun randomInviteCode(): String {
        // Excludes visually ambiguous characters (0/O, 1/I/L) since this gets read aloud/typed by hand.
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet.random() }.joinToString("")
    }

    /** Links an already-registered allievo to a PT after the fact (profile screen "Collega PT"),
     * resolving the same short invite code used at registration. Returns the linked PT's name on
     * success, or null if the code doesn't match any PT. */
    suspend fun linkToPtByInviteCode(userId: String, code: String): String? {
        val match = resolvePtInviteCode(code) ?: return null
        val current = userProfileDao.observeById(userId).firstOrNull() ?: return null
        userProfileDao.upsert(current.copy(ptId = match.id, syncStatus = SyncStatus.PENDING_UPDATE))
        runCatching {
            postgrest.from("profiles").update(mapOf("pt_id" to match.id)) { filter { eq("id", userId) } }
        }
        return match.fullName
    }

    /** Lets a user rename themself; visible immediately to their PT/allievi via the usual sync. */
    suspend fun updateFullName(userId: String, fullName: String) {
        val current = userProfileDao.observeById(userId).firstOrNull() ?: return
        userProfileDao.upsert(current.copy(fullName = fullName, syncStatus = SyncStatus.PENDING_UPDATE))
        runCatching {
            postgrest.from("profiles").update(mapOf("full_name" to fullName)) { filter { eq("id", userId) } }
        }
    }

    /** Persists the self-declared profile fields (bio, height, weight, goal) locally then remotely. */
    suspend fun updateProfileExtras(
        userId: String,
        bio: String?,
        heightCm: Int?,
        weightKg: Double?,
        primaryGoal: String?,
    ) {
        val current = userProfileDao.observeById(userId).firstOrNull() ?: return
        userProfileDao.upsert(
            current.copy(
                bio = bio?.takeIf { it.isNotBlank() },
                heightCm = heightCm,
                weightKg = weightKg,
                primaryGoal = primaryGoal?.takeIf { it.isNotBlank() },
                syncStatus = SyncStatus.PENDING_UPDATE,
            ),
        )
        runCatching {
            postgrest.from("profiles").update(
                mapOf(
                    "bio" to bio?.takeIf { it.isNotBlank() },
                    "height_cm" to heightCm,
                    "weight_kg" to weightKg,
                    "primary_goal" to primaryGoal?.takeIf { it.isNotBlank() },
                ),
            ) {
                filter { eq("id", userId) }
            }
        }
    }

    /** Uploads a new avatar image to the public "avatars" bucket and records its URL. */
    suspend fun updateAvatar(userId: String, bytes: ByteArray) {
        val path = "$userId/${UUID.randomUUID()}.jpg"
        runCatching {
            storage.from(AVATARS_BUCKET).upload(path, bytes)
            storage.from(AVATARS_BUCKET).publicUrl(path)
        }.onSuccess { url ->
            val current = userProfileDao.observeById(userId).firstOrNull() ?: return@onSuccess
            userProfileDao.upsert(current.copy(avatarUrl = url, syncStatus = SyncStatus.PENDING_UPDATE))
            runCatching {
                postgrest.from("profiles").update(mapOf("avatar_url" to url)) { filter { eq("id", userId) } }
            }
        }
    }
}
