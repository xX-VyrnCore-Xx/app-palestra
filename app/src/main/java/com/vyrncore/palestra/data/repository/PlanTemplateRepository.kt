package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.PlanTemplateDao
import com.vyrncore.palestra.data.local.dao.PlanTemplateExerciseDao
import com.vyrncore.palestra.data.local.entity.PlanTemplateEntity
import com.vyrncore.palestra.data.local.entity.PlanTemplateExerciseEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A PT's reusable library of plan templates ("Push day", "Full body principianti", ...), so a
 * PT builds an exercise list once and reuses it across clients instead of retyping it every
 * time. Kept device-local for now (not synced to Supabase) - a PT works from one device/tablet,
 * and this avoids widening the sync/RLS surface for a first version of the feature.
 */
@Singleton
class PlanTemplateRepository @Inject constructor(
    private val planTemplateDao: PlanTemplateDao,
    private val planTemplateExerciseDao: PlanTemplateExerciseDao,
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
            ),
        )
        planTemplateExerciseDao.upsertAll(
            exercises.mapIndexed { index, exercise ->
                exercise.copy(id = UUID.randomUUID().toString(), templateId = templateId, orderIndex = index)
            },
        )
        return templateId
    }

    suspend fun deleteTemplate(template: PlanTemplateEntity) {
        planTemplateDao.delete(template)
    }
}
