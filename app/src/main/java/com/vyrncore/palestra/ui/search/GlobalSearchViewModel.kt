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
    data class Contact(val peerId: String, val fullName: String) : SearchResult
}

/**
 * One search box across everything the allievo already has access to - schede, esercizi e il
 * proprio PT. Nessuna nuova query lato server: filtra solo i dati già osservati localmente,
 * quindi funziona anche offline. Ogni categoria è ordinata per rilevanza.
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
        _query.value = value.trimStart()
    }

    /** Selected muscle-group chip, or null for "all groups" - lets an allievo browse the exercise
     * list by body part even with an empty search box, not just refine an existing text query. */
    private val _muscleGroupFilter = MutableStateFlow<String?>(null)
    val muscleGroupFilter: StateFlow<String?> = _muscleGroupFilter.asStateFlow()

    fun setMuscleGroupFilter(group: String?) {
        _muscleGroupFilter.value = if (_muscleGroupFilter.value == group) null else group
    }

    val availableMuscleGroups: StateFlow<List<String>> = workoutRepository.observeExercises()
        .map { exercises -> exercises.map { it.muscleGroup }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val role = authRepository.observeProfile(userId).map { it?.role }

    private val plans = role.flatMapLatest { r ->
        when (r) {
            UserRole.ALLIEVO -> workoutRepository.observePlansForUser(userId)
            else -> flowOf(emptyList())
        }
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
        _query, plans, workoutRepository.observeExercises(), ptContact, _muscleGroupFilter,
    ) { query, plans, exercises, pt, muscleGroupFilter ->
        val groupFiltered = if (muscleGroupFilter == null) exercises else exercises.filter { it.muscleGroup == muscleGroupFilter }
        if (query.isBlank()) {
            // No text typed: a muscle-group chip alone puts the screen in "browse" mode instead
            // of staying empty until something is typed.
            if (muscleGroupFilter == null) {
                emptyList()
            } else {
                groupFiltered.sortedBy { it.name }.map { SearchResult.Exercise(it) }
            }
        } else {
            val trimmed = query.trim()
            buildList {
                addAll(
                    plans.mapNotNull { plan -> relevance(plan.name, trimmed)?.let { it to SearchResult.Plan(plan.id, plan.name, plan.category ?: "Scheda") } }
                        .sortedBy { it.first }
                        .map { it.second },
                )
                if (pt != null) {
                    relevance(pt.fullName, trimmed)?.let { add(SearchResult.Contact(pt.id, pt.fullName)) }
                }
                addAll(
                    groupFiltered.mapNotNull { ex ->
                        val score = relevance(ex.name, trimmed) ?: relevance(ex.muscleGroup, trimmed)?.plus(1)
                        score?.let { it to SearchResult.Exercise(ex) }
                    }.sortedBy { it.first }.take(30).map { it.second },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Lower is more relevant: 0 = starts with the query, 1 = a word inside it starts with the
     * query, 2 = the query just appears somewhere. Null = no match at all. */
    private fun relevance(text: String, query: String): Int? {
        if (query.isBlank()) return null
        val haystack = text.trim()
        return when {
            haystack.startsWith(query, ignoreCase = true) -> 0
            haystack.split(' ').any { it.startsWith(query, ignoreCase = true) } -> 1
            haystack.contains(query, ignoreCase = true) -> 2
            else -> null
        }
    }

    fun startWorkout(planId: String, onStarted: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            val sessionId = workoutRepository.startSession(userId, planId)
            onStarted(sessionId)
        }
    }
}
