package com.vyrncore.palestra.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.DEFAULT_REMINDER_THRESHOLD_DAYS
import com.vyrncore.palestra.data.repository.ThemeMode
import com.vyrncore.palestra.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val profile = authRepository.observeProfile(authRepository.currentUserId.orEmpty())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode = themeRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val remindersEnabled = themeRepository.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderThresholdDays = themeRepository.reminderThresholdDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_REMINDER_THRESHOLD_DAYS)

    val reminderCustomMessage = themeRepository.reminderCustomMessage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeRepository.setThemeMode(mode) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setRemindersEnabled(enabled) }
    }

    fun setReminderThresholdDays(days: Int) {
        viewModelScope.launch { themeRepository.setReminderThresholdDays(days) }
    }

    fun setReminderCustomMessage(message: String) {
        viewModelScope.launch { themeRepository.setReminderCustomMessage(message) }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }
}
