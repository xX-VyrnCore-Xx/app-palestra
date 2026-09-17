package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.BodyMetricsRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PtClientDetailViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    bodyMetricsRepository: BodyMetricsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])

    val plans = workoutRepository.observePlansForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions = workoutRepository.observeSessionsForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bodyMetrics = bodyMetricsRepository.observeForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
