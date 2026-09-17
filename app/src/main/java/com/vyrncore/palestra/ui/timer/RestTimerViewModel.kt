package com.vyrncore.palestra.ui.timer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestTimerUiState(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean = true,
    val isFinished: Boolean = false,
)

@HiltViewModel
class RestTimerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val totalSeconds: Int = checkNotNull(savedStateHandle["seconds"])

    private val _uiState = MutableStateFlow(RestTimerUiState(totalSeconds, totalSeconds))
    val uiState: StateFlow<RestTimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        start()
    }

    fun start() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(remainingSeconds = _uiState.value.remainingSeconds - 1)
            }
            _uiState.value = _uiState.value.copy(isRunning = false, isFinished = true)
        }
    }

    fun pause() {
        tickJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    fun resume() {
        if (_uiState.value.remainingSeconds > 0 && !_uiState.value.isRunning) {
            _uiState.value = _uiState.value.copy(isRunning = true)
            start()
        }
    }

    fun addSeconds(delta: Int) {
        val newRemaining = (_uiState.value.remainingSeconds + delta).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(remainingSeconds = newRemaining, isFinished = newRemaining == 0)
    }

    override fun onCleared() {
        tickJob?.cancel()
    }
}
