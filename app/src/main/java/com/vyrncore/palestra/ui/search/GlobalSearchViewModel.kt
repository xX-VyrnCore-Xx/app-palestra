package com.vyrncore.palestra.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.WorkoutRepository
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

sealed interface SearchResult {
    data class Plan(val planId: String, val name: String, val subtitle: String) : SearchResult
    /** Carries the full entity so the UI can open the exercise detail sheet from a tap. */
    data class Exercise(val entity: ExerciseEntity) : SearchResult
    data class Client(val clientId: String, val fullName: String, val email: String) : SearchResult
    data class Contact(val peerId: String, val fullName: String) : SearchResult
}

/**
 * One search box across everything a person already has access to - schede, esercizi, e a
 * seconda del ruolo le reclute (PT) o il proprio PT (allievo). Nessuna nuova query lato server:
 * filtra solo i dati già osservati localmente, quindi funziona anche offline.
 */
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserId.orEmpty()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    private val role = authRepository.observeProfile(userId).map { it?.role }

    private val plans = role.flatMapLatest { r ->
        when (r) {
            UserRole.PT -> flowOf(emptyList())
            UserRole.ALLIEVO -> workoutRepository.observePlansForUser(userId)
            null -> flowOf(emptyList())
        }
    }

    private val clients = role.flatMapLatest { r ->
        if (r == UserRole.PT) authRepository.observeClients(userId) else flowOf(emptyList())
    }

    private val ptContact = role.flatMapLatest { r ->
        if (r == UserRole.ALLIEVO) {
            authRepository.observeProfile(userId).flatMapLatest { profile ->
                val ptId = profile?.ptId
                if (ptId == null) flowOf(null) else authRepository.observeProfile(ptId)
            }
        } else {
            flowOf(null)
        }
    }

    val results: StateFlow<List<SearchResult>> = combine(
        _query, plans, workoutRepository.observeExercises(), clients, ptContact,
    ) { query, plans, exercises, clients, pt ->
        if (query.isBlank()) {
            emptyList()
        } else {
            buildList {
                plans.filter { it.name.contains(query, ignoreCase = true) }.forEach {
                    add(SearchResult.Plan(it.id, it.name, it.category ?: "Scheda"))
                }
                clients.filter { it.fullName.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) }
                    .forEach { add(SearchResult.Client(it.id, it.fullName, it.email)) }
                if (pt != null && pt.fullName.contains(query, ignoreCase = true)) {
                    add(SearchResult.Contact(pt.id, pt.fullName))
                }
                exercises.filter { it.name.contains(query, ignoreCase = true) || it.muscleGroup.contains(query, ignoreCase = true) }
                    .take(20)
                    .forEach { add(SearchResult.Exercise(it)) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startWorkout(planId: String, onStarted: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            val sessionId = workoutRepository.startSession(userId, planId)
            onStarted(sessionId)
        }
    }
}
