package com.vyrncore.palestra.ui.timer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.notification.ReminderScheduler
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
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val totalSeconds: Int = checkNotNull(savedStateHandle["seconds"])
    private val exerciseName: String? = savedStateHandle["exerciseName"]

    private val _uiState = MutableStateFlow(RestTimerUiState(totalSeconds, totalSeconds))
    val uiState: StateFlow<RestTimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        start()
    }

    fun start() {
        tickJob?.cancel()
        // Mirrors the in-app countdown with a background-safe worker, so leaving this screen (or
        // the app) before the timer finishes still delivers a notification + vibration at the
        // right time instead of silently missing the end of the rest period.
        reminderScheduler.scheduleRestTimerEnd(_uiState.value.remainingSeconds, exerciseName)
        tickJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(remainingSeconds = _uiState.value.remainingSeconds - 1)
            }
            _uiState.value = _uiState.value.copy(isRunning = false, isFinished = true)
            reminderScheduler.cancelRestTimerEnd()
        }
    }

    fun pause() {
        tickJob?.cancel()
        reminderScheduler.cancelRestTimerEnd()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    fun resume() {
        if (_uiState.value.remainingSeconds > 0 && !_uiState.value.isRunning) {
            _uiState.value = _uiState.value.copy(isRunning = true)
            start()
        }
    }

    /** Back to the original duration and running again - used by the "Ricomincia" button
     * shown once the timer has finished. */
    fun restart() {
        _uiState.value = RestTimerUiState(totalSeconds, totalSeconds)
        start()
    }

    fun addSeconds(delta: Int) {
        val newRemaining = (_uiState.value.remainingSeconds + delta).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(remainingSeconds = newRemaining, isFinished = newRemaining == 0)
        if (_uiState.value.isRunning && newRemaining > 0) {
            reminderScheduler.scheduleRestTimerEnd(newRemaining, exerciseName)
        } else {
            reminderScheduler.cancelRestTimerEnd()
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        // Deliberately NOT cancelling the scheduled notification here: leaving this screen (back
        // button, app backgrounded) while the countdown is still running is exactly the case it
        // exists to cover, so it must keep counting down independently of this ViewModel's lifecycle.
    }
}
