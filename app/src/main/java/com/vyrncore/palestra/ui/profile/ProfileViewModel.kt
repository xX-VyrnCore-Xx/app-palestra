package com.vyrncore.palestra.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.DEFAULT_REMINDER_THRESHOLD_DAYS
import com.vyrncore.palestra.data.repository.ThemeMode
import com.vyrncore.palestra.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

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

    val chatNotificationsEnabled = themeRepository.chatNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val planNotificationsEnabled = themeRepository.planNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val achievementNotificationsEnabled = themeRepository.achievementNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setChatNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setChatNotificationsEnabled(enabled) }
    }

    fun setPlanNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setPlanNotificationsEnabled(enabled) }
    }

    fun setAchievementNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setAchievementNotificationsEnabled(enabled) }
    }

    fun updateFullName(fullName: String) {
        if (fullName.isBlank()) return
        viewModelScope.launch { authRepository.updateFullName(userId, fullName.trim()) }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch
            authRepository.updateAvatar(userId, bytes)
        }
    }

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
