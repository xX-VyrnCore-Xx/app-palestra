package com.vyrncore.palestra.data.local

import com.vyrncore.palestra.data.local.entity.ExerciseEntity

/**
 * Built-in exercise catalog. IDs are fixed on purpose: the same rows are inserted server-side
 * by the `seed_exercise_catalog` Supabase migration, so a device that seeds its local Room
 * catalog and later syncs never creates duplicates (the push is a plain upsert by id).
 */
object ExerciseCatalogSeed {

    val exercises: List<ExerciseEntity> = listOf(
        Triple("a10c9b1e-1111-4a11-8000-000000000001", "Panca piana" to "Petto", "Bilanciere"),
        Triple("a10c9b1e-1111-4a11-8000-000000000002", "Panca inclinata" to "Petto", "Bilanciere"),
        Triple("a10c9b1e-1111-4a11-8000-000000000003", "Croci ai cavi" to "Petto", "Cavi"),
        Triple("a10c9b1e-1111-4a11-8000-000000000004", "Piegamenti" to "Petto", null),
        Triple("a10c9b1e-1111-4a11-8000-000000000005", "Trazioni alla lat machine" to "Dorso", "Lat machine"),
        Triple("a10c9b1e-1111-4a11-8000-000000000006", "Rematore con bilanciere" to "Dorso", "Bilanciere"),
        Triple("a10c9b1e-1111-4a11-8000-000000000007", "Trazioni alla sbarra" to "Dorso", "Sbarra"),
        Triple("a10c9b1e-1111-4a11-8000-000000000008", "Stacco da terra" to "Dorso", "Bilanciere"),
        Triple("a10c9b1e-1111-4a11-8000-000000000009", "Squat" to "Gambe", "Bilanciere"),
        Triple("a10c9b1e-1111-4a11-8000-00000000000a", "Leg press" to "Gambe", "Macchina"),
        Triple("a10c9b1e-1111-4a11-8000-00000000000b", "Affondi" to "Gambe", "Manubri"),
        Triple("a10c9b1e-1111-4a11-8000-00000000000c", "Leg curl" to "Gambe", "Macchina"),
        Triple("a10c9b1e-1111-4a11-8000-00000000000d", "Leg extension" to "Gambe", "Macchina"),
        Triple("a10c9b1e-1111-4a11-8000-00000000000e", "Polpacci in piedi" to "Gambe", "Macchina"),
        Triple("a10c9b1e-1111-4a11-8000-00000000000f", "Military press" to "Spalle", "Bilanciere"),
        Triple("a10c9b1e-1111-4a11-8000-000000000010", "Alzate laterali" to "Spalle", "Manubri"),
        Triple("a10c9b1e-1111-4a11-8000-000000000011", "Alzate posteriori" to "Spalle", "Manubri"),
        Triple("a10c9b1e-1111-4a11-8000-000000000012", "Curl bicipiti" to "Braccia", "Manubri"),
        Triple("a10c9b1e-1111-4a11-8000-000000000013", "Curl a martello" to "Braccia", "Manubri"),
        Triple("a10c9b1e-1111-4a11-8000-000000000014", "Push down ai cavi" to "Braccia", "Cavi"),
        Triple("a10c9b1e-1111-4a11-8000-000000000015", "French press" to "Braccia", "Bilanciere"),
        Triple("a10c9b1e-1111-4a11-8000-000000000016", "Plank" to "Core", null),
        Triple("a10c9b1e-1111-4a11-8000-000000000017", "Crunch" to "Core", null),
        Triple("a10c9b1e-1111-4a11-8000-000000000018", "Russian twist" to "Core", "Disco"),
    ).map { (id, nameGroup, equipment) ->
        ExerciseEntity(
            id = id,
            name = nameGroup.first,
            muscleGroup = nameGroup.second,
            equipment = equipment,
            isCustom = false,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
    }
}
