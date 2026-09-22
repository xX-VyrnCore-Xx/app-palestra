package com.vyrncore.palestra.ui.pt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.notification.ReminderScheduler
import com.vyrncore.palestra.data.repository.AllievoPrivateProfile
import com.vyrncore.palestra.data.repository.AllievoProfileRepository
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.BodyMetricsRepository
import com.vyrncore.palestra.data.repository.MembershipInfo
import com.vyrncore.palestra.data.repository.MembershipRepository
import com.vyrncore.palestra.data.repository.PtNotesRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
import com.vyrncore.palestra.util.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PtClientDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    bodyMetricsRepository: BodyMetricsRepository,
    private val ptNotesRepository: PtNotesRepository,
    private val authRepository: AuthRepository,
    private val allievoProfileRepository: AllievoProfileRepository,
    private val reminderScheduler: ReminderScheduler,
    private val membershipRepository: MembershipRepository,
    @ApplicationContext private val context: android.content.Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])
    private val ptId: String = authRepository.currentUserId.orEmpty()

    private val _allievoProfile = MutableStateFlow<AllievoPrivateProfile?>(null)
    val allievoProfile: StateFlow<AllievoPrivateProfile?> = _allievoProfile.asStateFlow()

    private val _membership = MutableStateFlow<MembershipInfo?>(null)
    val membership: StateFlow<MembershipInfo?> = _membership.asStateFlow()

    init {
        viewModelScope.launch { _allievoProfile.value = allievoProfileRepository.fetch(clientId) }
        refreshMembership()
    }

    private fun refreshMembership() {
        viewModelScope.launch { _membership.value = runCatching { membershipRepository.getCurrentMembership(clientId) }.getOrNull() }
    }

    fun recordMembership(planLabel: String?, startDate: java.time.LocalDate, endDate: java.time.LocalDate, notes: String?) {
        viewModelScope.launch {
            membershipRepository.recordMembership(clientId, planLabel, startDate, endDate, notes)
            refreshMembership()
        }
    }

    val plans = workoutRepository.observePlansForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val programs = workoutRepository.observeProgramsForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions = workoutRepository.observeSessionsForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bodyMetrics = bodyMetricsRepository.observeForUser(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val note = ptNotesRepository.observeForClient(ptId, clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val injuries = authRepository.observeProfile(clientId).map { it?.injuries }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val clientName = authRepository.observeProfile(clientId).map { it?.fullName.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val exerciseCatalog = workoutRepository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Exercise list for a plan the PT tapped in the "Schede assegnate" list, so they can check
     * what's actually in it without leaving this screen to open the Plan Editor. */
    fun observePlanExercises(planId: String) = workoutRepository.observePlanExercises(planId)

    fun saveNote(content: String) {
        viewModelScope.launch { ptNotesRepository.saveNote(ptId, clientId, content) }
    }

    /** Saves the note with a reminder [daysFromNow] away, and schedules the local notification
     * that will fire it - replacing any reminder already pending for this client. */
    fun saveNoteWithReminder(content: String, daysFromNow: Int) {
        viewModelScope.launch {
            val reminderAtEpochMs = System.currentTimeMillis() + daysFromNow * 24L * 3600 * 1000
            ptNotesRepository.saveNote(ptId, clientId, content, reminderAtEpochMs)
            reminderScheduler.schedulePtNoteReminder(
                clientId = clientId,
                clientName = clientName.value,
                message = content.take(200).ifBlank { "Hai un promemoria per questo allievo." },
                delayMs = daysFromNow * 24L * 3600 * 1000,
            )
        }
    }

    fun cancelNoteReminder() {
        viewModelScope.launch {
            val content = note.value?.content.orEmpty()
            ptNotesRepository.saveNote(ptId, clientId, content, reminderAtEpochMs = null)
            reminderScheduler.cancelPtNoteReminder(clientId)
        }
    }

    fun saveInjuries(injuries: String) {
        viewModelScope.launch { authRepository.updateInjuries(clientId, injuries) }
    }

    fun exportPdfReport() {
        viewModelScope.launch {
            val summaries = workoutRepository.observeSessionSummaries(clientId).first()
            val records = workoutRepository.observePersonalRecords(clientId).first()
            PdfReportGenerator.shareClientReport(context, clientName.value, summaries, records)
        }
    }
}
