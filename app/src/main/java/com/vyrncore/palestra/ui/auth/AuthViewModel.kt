package com.vyrncore.palestra.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    /** True right after a fresh sign-up: drives the Welcome onboarding wizard for ALLIEVI. */
    val justRegistered: Boolean = false,
    /** Per-field validation messages, shown inline under the offending input. */
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null,
)

private fun isValidEmail(email: String): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

/**
 * Login is a two-field form and never asks anything else on top; Register collects the basics
 * (name, email, password) plus optional body metrics up front, and after a successful
 * sign-up flags [AuthUiState.justRegistered] so the nav graph routes ALLIEVI through the
 * Welcome wizard before the dashboard. Existing users logging in skip onboarding entirely:
 * either they completed it once (persisted server-side) or they explicitly skipped it.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(loggedInUserId = authRepository.currentUserId))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        val emailError = when {
            email.isBlank() -> "Inserisci la tua email"
            !isValidEmail(email) -> "Formato email non valido"
            else -> null
        }
        val passwordError = if (password.isBlank()) "Inserisci la password" else null
        if (emailError != null || passwordError != null) {
            _uiState.value = _uiState.value.copy(emailError = emailError, passwordError = passwordError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, emailError = null, passwordError = null)
            runCatching { authRepository.signIn(email.trim(), password) }
                .onSuccess {
                    syncScheduler.syncNow()
                    _uiState.value = AuthUiState(loggedInUserId = authRepository.currentUserId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = friendlyError(e))
                }
        }
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        ptInviteCode: String?,
        heightCm: Int?,
        weightKg: Double?,
        primaryGoal: String?,
    ) {
        val nameError = if (fullName.isBlank()) "Come ti chiami?" else null
        val emailError = when {
            email.isBlank() -> "Inserisci la tua email"
            !isValidEmail(email) -> "Formato email non valido"
            else -> null
        }
        val passwordError = when {
            password.isBlank() -> "Scegli una password"
            password.length < 8 -> "Almeno 8 caratteri"
            else -> null
        }
        if (nameError != null || emailError != null || passwordError != null) {
            _uiState.value = _uiState.value.copy(nameError = nameError, emailError = emailError, passwordError = passwordError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, nameError = null, emailError = null, passwordError = null)
            runCatching {
                authRepository.signUp(
                    email = email.trim(),
                    password = password,
                    fullName = fullName.trim(),
                    ptInviteCode = ptInviteCode?.takeIf { it.isNotBlank() },
                    heightCm = heightCm,
                    weightKg = weightKg,
                    primaryGoal = primaryGoal,
                )
            }
                .onSuccess {
                    syncScheduler.syncNow()
                    _uiState.value = AuthUiState(
                        loggedInUserId = authRepository.currentUserId,
                        justRegistered = true,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = friendlyError(e))
                }
        }
    }

    fun clearErrors() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null, emailError = null, passwordError = null, nameError = null,
        )
    }

    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState.asStateFlow()

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank() || !isValidEmail(email)) {
            _passwordResetState.value = PasswordResetState.Error("Inserisci un'email valida")
            return
        }
        _passwordResetState.value = PasswordResetState.Sending
        viewModelScope.launch {
            runCatching { authRepository.sendPasswordResetEmail(email.trim()) }
                .onSuccess { _passwordResetState.value = PasswordResetState.Sent }
                .onFailure { e -> _passwordResetState.value = PasswordResetState.Error(friendlyError(e)) }
        }
    }

    fun resetPasswordResetState() {
        _passwordResetState.value = PasswordResetState.Idle
    }

    private val _setNewPasswordState = MutableStateFlow<SetNewPasswordState>(SetNewPasswordState.Idle)
    val setNewPasswordState: StateFlow<SetNewPasswordState> = _setNewPasswordState.asStateFlow()

    /** Final step of the password-reset deep link flow (see ResetPasswordScreen): [accessToken]
     * is the short-lived recovery credential from the link, never a normal session. */
    fun setNewPassword(accessToken: String, newPassword: String, confirmPassword: String) {
        val error = when {
            newPassword.length < 8 -> "La password deve avere almeno 8 caratteri"
            newPassword != confirmPassword -> "Le due password non coincidono"
            else -> null
        }
        if (error != null) {
            _setNewPasswordState.value = SetNewPasswordState.Error(error)
            return
        }
        _setNewPasswordState.value = SetNewPasswordState.Saving
        viewModelScope.launch {
            runCatching { authRepository.updatePasswordWithRecoveryToken(accessToken, newPassword) }
                .onSuccess { userId -> _setNewPasswordState.value = SetNewPasswordState.Done(userId) }
                .onFailure { e -> _setNewPasswordState.value = SetNewPasswordState.Error(friendlyError(e)) }
        }
    }
}

sealed interface PasswordResetState {
    data object Idle : PasswordResetState
    data object Sending : PasswordResetState
    data object Sent : PasswordResetState
    data class Error(val message: String) : PasswordResetState
}

sealed interface SetNewPasswordState {
    data object Idle : SetNewPasswordState
    data object Saving : SetNewPasswordState
    data class Done(val userId: String) : SetNewPasswordState
    data class Error(val message: String) : SetNewPasswordState
}
