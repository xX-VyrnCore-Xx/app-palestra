package com.vyrncore.palestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import com.vyrncore.palestra.data.repository.ThemeMode
import com.vyrncore.palestra.data.repository.ThemeRepository
import com.vyrncore.palestra.data.sync.RealtimeSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    /** True only while an ALLIEVO who just registered in this app install hasn't finished the
     * private Welcome questionnaire yet - never re-derived from a network fetch, so a returning
     * user is never nagged again just because a completion check failed to load (e.g. offline).
     * Never triggers for a PT - their role short-circuits the check. */
    val needsOnboarding: StateFlow<Boolean> = combine(
        userId, role, onboardingCompletedOverride, themeRepository.pendingOnboardingUserId,
    ) { id, r, override, pendingId ->
        !override && id != null && r == UserRole.ALLIEVO && pendingId == id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun markOnboardingComplete() {
        onboardingCompletedOverride.value = true
        viewModelScope.launch { themeRepository.setPendingOnboardingUserId(null) }
    }

    fun setLoggedInUser(id: String) {
        userId.value = id
        chatRepository.startListening(id)
        realtimeSyncManager.startListening(id)
        registerFcmToken(id)
    }

    /** Same as [setLoggedInUser], plus marks this account as owing the Welcome questionnaire -
     * call this only right after a successful sign-up, never on an ordinary login. */
    fun setNewlyRegisteredUser(id: String) {
        setLoggedInUser(id)
        viewModelScope.launch { themeRepository.setPendingOnboardingUserId(id) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeRepository.setThemeMode(mode) }
    }

    private val _pendingChatPeerId = MutableStateFlow<String?>(null)

    /** Set when a chat-message notification is tapped (cold or warm start) - the nav graph
     * navigates to this peer's thread once it's non-null, then clears it via [consumeChatDeepLink]. */
    val pendingChatPeerId: StateFlow<String?> = _pendingChatPeerId.asStateFlow()

    fun requestOpenChat(peerId: String) {
        _pendingChatPeerId.value = peerId
    }

    fun consumeChatDeepLink() {
        _pendingChatPeerId.value = null
    }
}
