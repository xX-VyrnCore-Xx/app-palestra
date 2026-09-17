package com.vyrncore.palestra.data.repository

import com.vyrncore.palestra.data.local.SyncStatus
import com.vyrncore.palestra.data.local.dao.BodyMetricDao
import com.vyrncore.palestra.data.local.entity.BodyMetricEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyMetricsRepository @Inject constructor(
    private val bodyMetricDao: BodyMetricDao,
) {
    fun observeForUser(userId: String): Flow<List<BodyMetricEntity>> = bodyMetricDao.observeForUser(userId)

    suspend fun logMetric(
        userId: String,
        weightKg: Double?,
        bodyFatPercent: Double?,
        chestCm: Double?,
        waistCm: Double?,
        hipsCm: Double?,
        armCm: Double?,
        thighCm: Double?,
        notes: String?,
    ) {
        bodyMetricDao.upsert(
            BodyMetricEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                dateEpochMs = System.currentTimeMillis(),
                weightKg = weightKg,
                bodyFatPercent = bodyFatPercent,
                chestCm = chestCm,
                waistCm = waistCm,
                hipsCm = hipsCm,
                armCm = armCm,
                thighCm = thighCm,
                notes = notes,
                syncStatus = SyncStatus.PENDING_CREATE,
            )
        )
    }
}
