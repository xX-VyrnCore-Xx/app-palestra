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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val userProfileDao: UserProfileDao,
) {
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
        }.onSuccess { dto ->
            userProfileDao.upsert(
                UserProfileEntity(
                    id = dto.id,
                    email = dto.email,
                    fullName = dto.fullName,
                    role = UserRole.valueOf(dto.role),
                    ptId = dto.ptId,
                    injuries = dto.injuries,
                    syncStatus = SyncStatus.SYNCED,
                )
            )
        }
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
}
