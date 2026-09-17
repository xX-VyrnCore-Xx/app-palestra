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

    fun startSession(planId: String, onStarted: (String) -> Unit) {
        viewModelScope.launch {
            val sessionId = workoutRepository.startSession(userId, planId)
            onStarted(sessionId)
        }
    }
}
