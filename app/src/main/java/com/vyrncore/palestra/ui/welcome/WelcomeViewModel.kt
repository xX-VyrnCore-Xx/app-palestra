package com.vyrncore.palestra.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AllievoPrivateProfile
import com.vyrncore.palestra.data.repository.AllievoProfileRepository
import com.vyrncore.palestra.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelcomeUiState(
    val painInjuries: String = "",
    val nutrition: String = "",
    val lifestyle: String = "",
    val goals: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Backs the private onboarding questionnaire an allievo fills in once, right after their first
 * login. Everything written here is strictly owner-only (RLS on allievo_private_profiles) - it
 * is never shown to the PT and never forwarded to the AI assistant.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val allievoProfileRepository: AllievoProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun updatePainInjuries(value: String) {
        _uiState.value = _uiState.value.copy(painInjuries = value)
    }

    fun updateNutrition(value: String) {
        _uiState.value = _uiState.value.copy(nutrition = value)
    }

    fun updateLifestyle(value: String) {
        _uiState.value = _uiState.value.copy(lifestyle = value)
    }

    fun updateGoals(value: String) {
        _uiState.value = _uiState.value.copy(goals = value)
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            runCatching {
                allievoProfileRepository.save(
                    AllievoPrivateProfile(
                        userId = userId,
                        painInjuries = state.painInjuries.trim().takeIf { it.isNotBlank() },
                        nutrition = state.nutrition.trim().takeIf { it.isNotBlank() },
                        lifestyle = state.lifestyle.trim().takeIf { it.isNotBlank() },
                        goals = state.goals.trim().takeIf { it.isNotBlank() },
                        completedOnboarding = true,
                    ),
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            onSaved()
        }
    }

    fun skip(onSaved: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                allievoProfileRepository.save(AllievoPrivateProfile(userId = userId, completedOnboarding = true))
            }
            onSaved()
        }
    }
}
