package com.vyrncore.palestra.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.sync.SyncScheduler
import com.vyrncore.palestra.util.friendlyError
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
}
