package com.vyrncore.palestra.ui.bodymetrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BodyMetricsViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val userId get() = authRepository.currentUserId.orEmpty()

    val metrics = bodyMetricsRepository.observeForUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logWeight(weightKg: Double, bodyFatPercent: Double?, waistCm: Double?) {
        viewModelScope.launch {
            bodyMetricsRepository.logMetric(
                userId = userId,
                weightKg = weightKg,
                bodyFatPercent = bodyFatPercent,
                chestCm = null,
                waistCm = waistCm,
                hipsCm = null,
                armCm = null,
                thighCm = null,
                notes = null,
            )
        }
    }
}
