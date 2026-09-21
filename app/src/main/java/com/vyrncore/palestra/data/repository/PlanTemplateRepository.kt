package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.PlanTemplateDao
import com.vyrncore.palestra.data.local.dao.PlanTemplateExerciseDao
import com.vyrncore.palestra.data.local.entity.PlanTemplateEntity
import com.vyrncore.palestra.data.local.entity.PlanTemplateExerciseEntity
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A PT's reusable library of plan templates ("Push day", "Full body principianti", ...), so a
 * PT builds an exercise list once and reuses it across clients instead of retyping it every time.
 * Synced to Supabase like every other PT-owned entity - a template used to be device-local only,
 * which silently lost a PT's whole library on reinstall or a new device.
 */
@Singleton
class PlanTemplateRepository @Inject constructor(
    private val planTemplateDao: PlanTemplateDao,
    private val planTemplateExerciseDao: PlanTemplateExerciseDao,
    private val postgrest: Postgrest,
) {
    fun observeForPt(ptId: String): Flow<List<PlanTemplateEntity>> = planTemplateDao.observeForPt(ptId)

    fun observeExercisesForTemplate(templateId: String): Flow<List<PlanTemplateExerciseEntity>> =
        planTemplateExerciseDao.observeForTemplate(templateId)

    suspend fun saveTemplate(
        ptId: String,
        name: String,
        category: String?,
        exercises: List<PlanTemplateExerciseEntity>,
    ): String {
        val templateId = UUID.randomUUID().toString()
        planTemplateDao.upsert(
            PlanTemplateEntity(
                id = templateId,
                ptId = ptId,
                name = name,
                category = category,
                createdAtEpochMs = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_CREATE,
            ),
        )
        planTemplateExerciseDao.upsertAll(
            exercises.mapIndexed { index, exercise ->
                exercise.copy(
                    id = UUID.randomUUID().toString(),
                    templateId = templateId,
                    orderIndex = index,
                    syncStatus = SyncStatus.PENDING_CREATE,
                )
            },
        )
        return templateId
    }

    /** Deletes locally and, best-effort, remotely right away - a template has no other party who
     * needs to see the deletion, so there's no need for the tombstone/PENDING_DELETE dance a
     * shared entity like a chat message would need. */
    suspend fun deleteTemplate(template: PlanTemplateEntity) {
        planTemplateDao.delete(template)
        runCatching { postgrest.from("plan_templates").delete { filter { eq("id", template.id) } } }
    }
}
