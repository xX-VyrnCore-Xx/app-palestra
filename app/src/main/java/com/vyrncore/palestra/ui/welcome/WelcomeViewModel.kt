package com.vyrncore.palestra.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AllievoPrivateProfile
import com.vyrncore.palestra.data.repository.AllievoProfileRepository
import com.vyrncore.palestra.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.functions.Functions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class SendPushRequest(
    val recipientId: String,
    val type: String,
    val title: String,
    val body: String,
)

/** Single-select options shown as cards, in display order. */
val EXPERIENCE_LEVELS = listOf("Principiante", "Intermedio", "Avanzato")
val TRAINING_DAYS_OPTIONS = listOf("1-2 giorni", "3-4 giorni", "5+ giorni")
val PRIMARY_GOAL_OPTIONS = listOf("Perdere peso", "Aumentare massa", "Migliorare resistenza", "Tonificare", "Salute generale")
val ACTIVITY_LEVEL_OPTIONS = listOf("Lavoro sedentario", "Moderatamente attivo", "Molto attivo")

/** Optional body metrics asked during onboarding, pre-filled from registration when present. */
data class BodyMetricsDraft(val heightCm: Int? = null, val weightKg: Double? = null)

data class WelcomeUiState(
    val experienceLevel: String? = null,
    val trainingDays: String? = null,
    val primaryGoal: String? = null,
    val activityLevel: String? = null,
    val painInjuries: String = "",
    val nutrition: String = "",
    val goals: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Backs the onboarding questionnaire an allievo fills in once, right after their first login: a
 * short wizard mixing single-select questions (experience, training days, goal, lifestyle) with
 * open text ones (injuries, diet, other notes). RLS on allievo_private_profiles lets the owner
 * and their own PT read the answers (the PT uses them to tailor a plan); the AI assistant never
 * does, and this ViewModel never forwards them anywhere but that table.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val allievoProfileRepository: AllievoProfileRepository,
    private val authRepository: AuthRepository,
    private val functions: Functions,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    /** 4 select steps + 3 open-text ones + final recap. */
    val stepCount = 8

    fun selectExperienceLevel(value: String) {
        _uiState.value = _uiState.value.copy(experienceLevel = value)
    }

    fun selectTrainingDays(value: String) {
        _uiState.value = _uiState.value.copy(trainingDays = value)
    }

    fun selectPrimaryGoal(value: String) {
        _uiState.value = _uiState.value.copy(primaryGoal = value)
    }

    fun selectActivityLevel(value: String) {
        _uiState.value = _uiState.value.copy(activityLevel = value)
    }

    fun updatePainInjuries(value: String) {
        _uiState.value = _uiState.value.copy(painInjuries = value)
    }

    fun updateNutrition(value: String) {
        _uiState.value = _uiState.value.copy(nutrition = value)
    }

    fun updateGoals(value: String) {
        _uiState.value = _uiState.value.copy(goals = value)
    }

    fun nextStep() {
        if (_step.value < stepCount - 1) _step.value += 1
    }

    fun previousStep() {
        if (_step.value > 0) _step.value -= 1
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            runCatching {
                allievoProfileRepository.save(
                    AllievoPrivateProfile(
                        userId = userId,
                        experienceLevel = state.experienceLevel,
                        trainingDays = state.trainingDays,
                        primaryGoal = state.primaryGoal,
                        activityLevel = state.activityLevel,
                        painInjuries = state.painInjuries.trim().takeIf { it.isNotBlank() },
                        nutrition = state.nutrition.trim().takeIf { it.isNotBlank() },
                        goals = state.goals.trim().takeIf { it.isNotBlank() },
                        completedOnboarding = true,
                    ),
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            notifyPtProfileCompleted()
            onSaved()
        }
    }

    /** Best-effort nudge to the allievo's PT: their new recruit finished the questionnaire, so
     * there's now a profile worth building a plan around instead of starting from nothing. */
    private suspend fun notifyPtProfileCompleted() {
        val profile = authRepository.observeProfile(userId).first() ?: return
        val ptId = profile.ptId ?: return
        runCatching {
            functions.invoke(
                "send-push",
                body = SendPushRequest(
                    recipientId = ptId,
                    type = "plan_update",
                    title = "Nuova recluta pronta",
                    body = "${profile.fullName} ha completato il proprio profilo: dai un'occhiata prima di assegnare la scheda.",
                ),
            )
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
