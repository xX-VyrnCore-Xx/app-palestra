package com.vyrncore.palestra.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutPlansViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

    private val plansFlow = workoutRepository.observePlansForUser(userId)

    val plans = plansFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** planId -> number of exercises, so the plan list can show a quick "N esercizi" badge. */
    val exerciseCounts = plansFlow
        .flatMapLatest { plans ->
            if (plans.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(
                    plans.map { plan -> workoutRepository.observePlanExerciseCount(plan.id).map { plan.id to it } },
                ) { pairs -> pairs.toMap() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** The plan to highlight as "up next": the one right after whichever plan the allievo's most
     * recent completed session was for, cycling back to the first once the rotation is done. Null
     * until there's at least one completed session to react to, so a first-time user just sees the
     * plain list instead of an arbitrary pick. This is what makes the plan screen "reattivo": no
     * manual re-selection needed after finishing a workout, the next one in the rotation is already
     * front and center next time they open the tab. */
    val suggestedPlanId = combine(plansFlow, workoutRepository.observeSessionsForUser(userId)) { plans, sessions ->
        if (plans.size < 2) return@combine null
        val lastCompletedPlanId = sessions.filter { it.endedAtEpochMs != null }
            .maxByOrNull { it.endedAtEpochMs!! }
            ?.planId ?: return@combine null
        val orderedByAge = plans.sortedBy { it.createdAtEpochMs }
        val lastIndex = orderedByAge.indexOfFirst { it.id == lastCompletedPlanId }
        if (lastIndex == -1) null else orderedByAge[(lastIndex + 1) % orderedByAge.size].id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startSession(planId: String, onStarted: (String) -> Unit) {
        viewModelScope.launch {
            val sessionId = workoutRepository.startSession(userId, planId)
            onStarted(sessionId)
        }
    }
}
