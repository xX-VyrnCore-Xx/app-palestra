package com.vyrncore.palestra.data.sync

import com.vyrncore.palestra.data.local.dao.BodyMetricDao
import com.vyrncore.palestra.data.local.dao.PlanExerciseDao
import com.vyrncore.palestra.data.local.dao.WorkoutPlanDao
import com.vyrncore.palestra.data.remote.dto.BodyMetricDto
import com.vyrncore.palestra.data.remote.dto.PlanExerciseDto
import com.vyrncore.palestra.data.remote.dto.WorkoutPlanDto
import com.vyrncore.palestra.data.remote.toEntity
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live-updates workout plans/exercises and body metrics via Supabase Realtime, the same way
 * [ChatRepository] already does for messages - so a plan a PT just assigned (or edited) shows up
 * on the allievo's device immediately, instead of waiting for [SyncManager]'s periodic pull.
 * RLS already scopes which rows Realtime delivers to each connected user, so no extra
 * server-side filtering is needed here beyond what postgres_changes already enforces.
 *
 * The underlying websocket does not always resume its subscriptions on its own after the phone's
 * network drops and comes back (screen off overnight, a tunnel, switching wifi/data) - without
 * [observeConnectivity], the app would silently stop receiving live updates until the next app
 * restart even though [ConnectivityObserver] reports it's back online, so a PT's edit would sit
 * unseen until [SyncManager]'s next periodic pull instead of arriving immediately as intended.
 */
@Singleton
class RealtimeSyncManager @Inject constructor(
    private val realtime: Realtime,
    private val workoutPlanDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao,
    private val bodyMetricDao: BodyMetricDao,
    private val connectivityObserver: ConnectivityObserver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var channel: RealtimeChannel? = null
    private var listeningUserId: String? = null

    init {
        observeConnectivity()
    }

    /** Whenever connectivity comes back and the realtime client isn't actually connected, force a
     * fresh connect + resubscribe of the current channel instead of waiting for it to notice on
     * its own. [drop]\(1\) skips the observer's initial replayed value so this only reacts to real
     * transitions, not the state at startup (already handled by [startListening]). */
    private fun observeConnectivity() {
        scope.launch {
            connectivityObserver.isOnline
                .distinctUntilChanged()
                .drop(1)
                .filter { online -> online }
                .collect {
                    if (realtime.status.value != Realtime.Status.CONNECTED) {
                        runCatching { realtime.connect() }
                    }
                    runCatching { channel?.subscribe() }
                }
        }
    }

    fun startListening(userId: String) {
        if (listeningUserId == userId) return
        listeningUserId = userId
        scope.launch {
            runCatching {
                val ch = realtime.channel("realtime-sync-$userId")
                channel = ch

                val planInserts = ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "workout_plans" }
                val planUpdates = ch.postgresChangeFlow<PostgresAction.Update>(schema = "public") { table = "workout_plans" }
                val exerciseInserts = ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "plan_exercises" }
                val exerciseUpdates = ch.postgresChangeFlow<PostgresAction.Update>(schema = "public") { table = "plan_exercises" }
                val metricInserts = ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "body_metrics" }
                val metricUpdates = ch.postgresChangeFlow<PostgresAction.Update>(schema = "public") { table = "body_metrics" }

                ch.subscribe()

                launch {
                    planInserts.collect { action ->
                        runCatching { action.decodeRecord<WorkoutPlanDto>() }.getOrNull()?.let { workoutPlanDao.upsert(it.toEntity()) }
                    }
                }
                launch {
                    planUpdates.collect { action ->
                        runCatching { action.decodeRecord<WorkoutPlanDto>() }.getOrNull()?.let { workoutPlanDao.upsert(it.toEntity()) }
                    }
                }
                launch {
                    exerciseInserts.collect { action ->
                        runCatching { action.decodeRecord<PlanExerciseDto>() }.getOrNull()?.let { planExerciseDao.upsert(it.toEntity()) }
                    }
                }
                launch {
                    exerciseUpdates.collect { action ->
                        runCatching { action.decodeRecord<PlanExerciseDto>() }.getOrNull()?.let { planExerciseDao.upsert(it.toEntity()) }
                    }
                }
                launch {
                    metricInserts.collect { action ->
                        runCatching { action.decodeRecord<BodyMetricDto>() }.getOrNull()?.let { bodyMetricDao.upsert(it.toEntity()) }
                    }
                }
                launch {
                    metricUpdates.collect { action ->
                        runCatching { action.decodeRecord<BodyMetricDto>() }.getOrNull()?.let { bodyMetricDao.upsert(it.toEntity()) }
                    }
                }
            }
        }
    }
}
