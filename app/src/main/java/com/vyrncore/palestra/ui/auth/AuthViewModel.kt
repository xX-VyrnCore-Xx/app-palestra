package com.vyrncore.palestra.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.BuildConfig
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUserId: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(loggedInUserId = authRepository.currentUserId))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { authRepository.signIn(email, password) }
                .onSuccess {
                    syncScheduler.syncNow()
                    _uiState.value = AuthUiState(loggedInUserId = authRepository.currentUserId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = friendlyError(e))
                }
        }
    }

    fun signUp(email: String, password: String, fullName: String, role: UserRole, ptId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { authRepository.signUp(email, password, fullName, role, ptId?.takeIf { it.isNotBlank() }) }
                .onSuccess {
                    syncScheduler.syncNow()
                    _uiState.value = AuthUiState(loggedInUserId = authRepository.currentUserId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = friendlyError(e))
                }
        }
    }

    /** Turns a raw exception (often a low-level Ktor/network message) into something an end
     * user can actually act on, in Italian. */
    private fun friendlyError(e: Throwable): String {
        if (BuildConfig.SUPABASE_URL.isBlank()) {
            return "Questa build non è collegata al backend (credenziali Supabase mancanti). Contatta chi ha generato questo APK."
        }
        val message = e.message.orEmpty()
        return when {
            message.contains("Failed to connect", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ->
                "Impossibile connettersi al server. Controlla la connessione internet e riprova."
            message.contains("Invalid login credentials", ignoreCase = true) -> "Email o password non corretti."
            message.contains("already registered", ignoreCase = true) || message.contains("already exists", ignoreCase = true) ->
                "Esiste già un account con questa email."
            else -> "Si è verificato un problema, riprova tra poco."
        }
    }
}
