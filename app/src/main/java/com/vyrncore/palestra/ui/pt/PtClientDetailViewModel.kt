package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.BodyMetricsRepository
import com.vyrncore.palestra.data.repository.PtNotesRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PtClientDetailViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    bodyMetricsRepository: BodyMetricsRepository,
    private val ptNotesRepository: PtNotesRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])
    private val ptId: String = authRepository.currentUserId.orEmpty()

    val plans = workoutRepository.observePlansForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions = workoutRepository.observeSessionsForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bodyMetrics = bodyMetricsRepository.observeForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val note = ptNotesRepository.observeForClient(ptId, clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val injuries = authRepository.observeProfile(clientId).map { it?.injuries }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val clientName = authRepository.observeProfile(clientId).map { it?.fullName.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveNote(content: String) {
        viewModelScope.launch { ptNotesRepository.saveNote(ptId, clientId, content) }
    }

    fun saveInjuries(injuries: String) {
        viewModelScope.launch { authRepository.updateInjuries(clientId, injuries) }
    }
}
