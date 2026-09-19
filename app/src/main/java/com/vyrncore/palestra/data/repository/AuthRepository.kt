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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun signUp(email: String, password: String, fullName: String, role: UserRole, ptId: String?) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = auth.currentUserOrNull()?.id ?: error("Sign up did not return a user")
        val profile = UserProfileEntity(
            id = userId,
            email = email,
            fullName = fullName,
            role = role,
            ptId = ptId,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
        userProfileDao.upsert(profile)
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = auth.currentUserOrNull()?.id ?: return
        runCatching {
            postgrest.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingle<UserProfileDto>()
        }.onSuccess { dto -> userProfileDao.upsert(dto.toEntity()) }
    }

    suspend fun signOut() {
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

    /** Lets a user rename themself; visible immediately to their PT/allievi via the usual sync. */
    suspend fun updateFullName(userId: String, fullName: String) {
        val current = userProfileDao.observeById(userId).firstOrNull() ?: return
        userProfileDao.upsert(current.copy(fullName = fullName, syncStatus = SyncStatus.PENDING_UPDATE))
        runCatching {
            postgrest.from("profiles").update(mapOf("full_name" to fullName)) { filter { eq("id", userId) } }
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
