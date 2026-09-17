package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PtDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val ptId: String get() = authRepository.currentUserId.orEmpty()

    val clients = authRepository.observeClients(ptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
