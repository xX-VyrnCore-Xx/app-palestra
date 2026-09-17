package com.vyrncore.palestra.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AiAssistantRepository
import com.vyrncore.palestra.data.repository.AiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiAssistantUiState(
    val messages: List<AiMessage> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val repository: AiAssistantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.history() }
                .onSuccess { history -> _uiState.value = _uiState.value.copy(messages = history) }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isSending) return

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + AiMessage(role = "user", content = trimmed),
            isSending = true,
            error = null,
        )
        viewModelScope.launch {
            runCatching { repository.sendMessage(trimmed) }
                .onSuccess { reply ->
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + AiMessage(role = "assistant", content = reply),
                        isSending = false,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = e.message ?: "Errore di connessione con l'assistente AI",
                    )
                }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
