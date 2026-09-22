package com.vyrncore.palestra.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.BodyMetricsRepository
import com.vyrncore.palestra.data.repository.DEFAULT_REMINDER_THRESHOLD_DAYS
import com.vyrncore.palestra.data.repository.MembershipInfo
import com.vyrncore.palestra.data.repository.MembershipRepository
import com.vyrncore.palestra.data.repository.ThemeMode
import com.vyrncore.palestra.data.repository.ThemeRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val workoutRepository: WorkoutRepository,
    private val bodyMetricsRepository: BodyMetricsRepository,
    private val membershipRepository: MembershipRepository,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

    val profile = authRepository.observeProfile(authRepository.currentUserId.orEmpty())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _membership = MutableStateFlow<MembershipInfo?>(null)
    val membership: StateFlow<MembershipInfo?> = _membership.asStateFlow()

    /** Called once the profile has loaded. */
    fun loadMembership() {
        viewModelScope.launch { _membership.value = runCatching { membershipRepository.getCurrentMembership(userId) }.getOrNull() }
    }

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

    /** Persists bio/height/weight/goal edits from the profile customization sheet. */
    fun updateProfileExtras(bio: String, heightCm: Int?, weightKg: Double?, primaryGoal: String?) {
        viewModelScope.launch {
            authRepository.updateProfileExtras(
                userId = userId,
                bio = bio.trim().takeIf { it.isNotBlank() },
                heightCm = heightCm,
                weightKg = weightKg,
                primaryGoal = primaryGoal,
            )
        }
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

    /** Every session, logged set, and body-metric entry the allievo has recorded, as one CSV -
     * the honest "take your data and leave" export, not just the workout summaries. */
    fun exportAllData() {
        viewModelScope.launch {
            val summaries = workoutRepository.observeSessionSummaries(userId).first()
            val plans = workoutRepository.observePlansForUser(userId).first()
            val history = summaries.filter { it.endedAtEpochMs != null }.map { summary ->
                com.vyrncore.palestra.ui.history.HistoryItemUi(
                    sessionId = summary.sessionId,
                    planName = plans.firstOrNull { it.id == summary.planId }?.name ?: "Allenamento libero",
                    startedAtEpochMs = summary.startedAtEpochMs,
                    durationMinutes = summary.endedAtEpochMs?.let { (it - summary.startedAtEpochMs) / 60_000 },
                    setCount = summary.setCount,
                    totalVolumeKg = summary.totalVolumeKg,
                )
            }
            val bodyMetrics = bodyMetricsRepository.observeForUser(userId).first()
            CsvExporter.shareFullExport(context, history, bodyMetrics)
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            val deviceToken = runCatching { currentFcmToken() }.getOrNull()
            authRepository.signOut(deviceToken)
            onSignedOut()
        }
    }

    /** Firebase's Task API has no suspend equivalent wired into this project (no
     * kotlinx-coroutines-play-services dependency), so this bridges the one-shot callback itself
     * rather than pull in a dependency for a single call site. */
    private suspend fun currentFcmToken(): String = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { cont.resume(it, null) }
            .addOnFailureListener { cont.cancel(it) }
    }

    private val _linkPtResult = MutableStateFlow<LinkPtResult?>(null)
    val linkPtResult: StateFlow<LinkPtResult?> = _linkPtResult.asStateFlow()

    /** Allievo-side "Collega il tuo PT" from the profile screen, for whoever skipped it (or
     * didn't have a code yet) at registration - same lookup, just later. */
    fun linkToPt(code: String) {
        viewModelScope.launch {
            val ptName = authRepository.linkToPtByInviteCode(userId, code)
            _linkPtResult.value = if (ptName != null) LinkPtResult.Success(ptName) else LinkPtResult.NotFound
        }
    }

    fun clearLinkPtResult() {
        _linkPtResult.value = null
    }
}

sealed interface LinkPtResult {
    data class Success(val ptName: String) : LinkPtResult
    data object NotFound : LinkPtResult
}
