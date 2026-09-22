package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.BuildConfig
import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.UserProfileDao
import com.vyrncore.palestra.data.local.entity.UserProfileEntity
import com.vyrncore.palestra.data.local.entity.UserRole
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import com.vyrncore.palestra.data.remote.dto.UserProfileDto
import com.vyrncore.palestra.data.remote.toEntity
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PtInviteMatch(
    val id: String,
    @SerialName("full_name") val fullName: String,
)

@Serializable
private data class WelcomeEmailRequest(val fullName: String, val role: String)

@Serializable
private data class PasswordResetRequest(val email: String)

@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val storage: Storage,
    private val userProfileDao: UserProfileDao,
    private val functions: Functions,
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

        // Best-effort branded welcome email via Resend - registration must never fail on this,
        // Supabase Auth's own built-in email already covers verification/reset.
        runCatching {
            functions.invoke("send-welcome-email", body = WelcomeEmailRequest(fullName = fullName, role = role.name))
        }
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

    /** Sends the password-reset email - "Password dimenticata?" on the Login screen. Routed
     * through the request-password-reset edge function rather than calling
     * [io.github.jan.supabase.auth.Auth.resetPasswordForEmail] directly: the function mints the
     * recovery link itself and delivers it through the branded Resend template (falling back to
     * Supabase's own plain email only if Resend isn't configured), and never reveals whether
     * [email] is actually registered. */
    suspend fun sendPasswordResetEmail(email: String) {
        functions.invoke("request-password-reset", body = PasswordResetRequest(email = email))
    }

    /** Sets a new password using the short-lived recovery access token from the
     * "vibefitness://reset-password" deep link (see MainActivity/RootViewModel/ResetPasswordScreen).
     * A plain REST call to Supabase Auth's own `PUT /auth/v1/user` endpoint - the exact request the
     * official SDKs make for updateUser() - rather than importing the token as a supabase-kt
     * session, since it's never persisted locally until this call actually succeeds.
     * @return the user's id, so the caller can sign them in for real afterwards.
     */
    suspend fun updatePasswordWithRecoveryToken(accessToken: String, newPassword: String): String = withContext(Dispatchers.IO) {
        if (newPassword.length < 8) throw IllegalArgumentException("La password deve essere di almeno 8 caratteri.")
        val connection = (java.net.URL("${BuildConfig.SUPABASE_URL}/auth/v1/user").openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        try {
            connection.outputStream.use {
                it.write("{\"password\":${JSONObject.quote(newPassword)}}".toByteArray(Charsets.UTF_8))
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream.bufferedReader().use { it.readText() }
            if (responseCode !in 200..299) {
                throw IllegalStateException("Il link non è più valido: richiedine uno nuovo dalla schermata di accesso.")
            }
            JSONObject(responseBody).getString("id")
        } finally {
            connection.disconnect()
        }
    }

    /** Clears this device's push token(s) from the outgoing user before signing out - without this,
     * a device shared between accounts (PT signs out, allievo signs in) keeps delivering push
     * notifications for BOTH accounts, since the token would otherwise stay registered until a
     * future login happens to overwrite it. Every device this user is signed into keeps its own row
     * in device_tokens, so only this device's is removed here. */
    suspend fun signOut(deviceToken: String? = null) {
        currentUserId?.let { userId ->
            runCatching {
                if (deviceToken != null) {
                    postgrest.from("device_tokens").delete {
                        filter { eq("user_id", userId); eq("fcm_token", deviceToken) }
                    }
                }
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

    /** Registers this device's FCM token so the backend can push notifications to it. Kept in
     * device_tokens (one row per user+device, so every installed device gets pushes, not just the
     * last one to register) and mirrored onto profiles.fcm_token for older RPCs/functions that still
     * read the single-device column. If this token was previously another user's device (shared
     * device, different account signed in), that stale row is dropped first. */
    suspend fun updateFcmToken(userId: String, token: String) {
        runCatching {
            postgrest.from("device_tokens").delete {
                filter { eq("fcm_token", token); neq("user_id", userId) }
            }
            postgrest.from("device_tokens").upsert(
                mapOf("user_id" to userId, "fcm_token" to token, "platform" to "android"),
            )
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
            val params = buildJsonObject { put("code", code.trim()) }
            postgrest.rpc("resolve_pt_invite_code", params).decodeSingleOrNull<PtInviteMatch>()
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
