package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.PtNoteDao
import com.vyrncore.palestra.data.local.entity.PtNoteEntity
import com.vyrncore.palestra.data.remote.toDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Private PT notes about a client — one editable note per PT/client pair, visible only to the PT. */
@Singleton
class PtNotesRepository @Inject constructor(
    private val ptNoteDao: PtNoteDao,
    private val postgrest: Postgrest,
) {
    fun observeForClient(ptId: String, clientId: String): Flow<PtNoteEntity?> =
        ptNoteDao.observeForClient(ptId, clientId)

    suspend fun saveNote(ptId: String, clientId: String, content: String) {
        val existing = ptNoteDao.observeForClient(ptId, clientId).first()
        saveNote(ptId, clientId, content, existing?.reminderAtEpochMs)
    }

    /** The reminder is kept device-local (not synced to Supabase - see [PlanTemplateRepository]
     * for the same reasoning), so it never round-trips through [PtNoteDto]. */
    suspend fun saveNote(ptId: String, clientId: String, content: String, reminderAtEpochMs: Long?) {
        // Deterministic id from the (pt, client) pair mirrors the unique constraint on the
        // remote table, so re-saving always upserts the same row instead of creating duplicates.
        val id = UUID.nameUUIDFromBytes("$ptId:$clientId".toByteArray()).toString()
        val now = System.currentTimeMillis()
        val entity = PtNoteEntity(
            id = id,
            ptId = ptId,
            clientId = clientId,
            content = content,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            reminderAtEpochMs = reminderAtEpochMs,
            syncStatus = SyncStatus.PENDING_UPDATE,
        )
        ptNoteDao.upsert(entity)
        runCatching { postgrest.from("pt_notes").upsert(entity.toDto()) }
            .onSuccess { ptNoteDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED)) }
    }
}
