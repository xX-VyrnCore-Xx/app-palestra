package com.vyrncore.palestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.AllievoProfileRepository
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import com.vyrncore.palestra.data.repository.ThemeMode
import com.vyrncore.palestra.data.repository.ThemeRepository
import com.vyrncore.palestra.data.sync.RealtimeSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val chatRepository: ChatRepository,
    private val realtimeSyncManager: RealtimeSyncManager,
    private val allievoProfileRepository: AllievoProfileRepository,
) : ViewModel() {

    val startUserId: String? = authRepository.currentUserId

    private val userId = MutableStateFlow(authRepository.currentUserId)

    init {
        startUserId?.let {
            chatRepository.startListening(it)
            realtimeSyncManager.startListening(it)
            registerFcmToken(it)
        }
    }

    /** Sends this device's push token to the backend, so it knows where to deliver notifications. */
    private fun registerFcmToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch { authRepository.updateFcmToken(userId, token) }
        }
    }

    val role: StateFlow<UserRole?> = userId
        .flatMapLatest { id -> if (id != null) authRepository.observeProfile(id) else flowOf(null) }
        .map { it?.role }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    private val onboardingCompletedOverride = MutableStateFlow(false)

    /** Set right after a fresh sign-up: forces the Welcome wizard for a brand-new ALLIEVO,
     * even if their (still empty) profile row would evaluate as "no onboarding needed". */
    private val forceOnboarding = MutableStateFlow(false)

    /** True only while an ALLIEVO is logged in and hasn't finished the private Welcome
     * questionnaire yet. Never triggers for a PT - their role short-circuits the check - and
     * a regular login never sets [forceOnboarding], so returning users skip it entirely. */
    val needsOnboarding: StateFlow<Boolean> = combine(userId, role, onboardingCompletedOverride, forceOnboarding) { id, r, override, forced ->
        arrayOf(id, r, override, forced)
    }.flatMapLatest { parts ->
        val id = parts[0] as String?
        val r = parts[1] as UserRole?
        val override = parts[2] as Boolean
        val forced = parts[3] as Boolean
        when {
            (override && !forced) || id == null || r != UserRole.ALLIEVO -> flowOf(false)
            forced -> flowOf(true)
            else -> flow { emit(allievoProfileRepository.fetch(id)?.completedOnboarding != true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun markOnboardingComplete() {
        onboardingCompletedOverride.value = true
        forceOnboarding.value = false
    }

    /** Called after a fresh registration so the Welcome wizard shows before the dashboard. */
    fun requestOnboarding() {
        onboardingCompletedOverride.value = false
        forceOnboarding.value = true
    }

    fun setLoggedInUser(id: String) {
        userId.value = id
        chatRepository.startListening(id)
        realtimeSyncManager.startListening(id)
        registerFcmToken(id)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeRepository.setThemeMode(mode) }
    }
}
