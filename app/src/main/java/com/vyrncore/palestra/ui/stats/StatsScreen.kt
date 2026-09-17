package com.vyrncore.palestra.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.BarChartEntry
import com.vyrncore.palestra.ui.components.MetricCard
import com.vyrncore.palestra.ui.components.SimpleBarChart
import com.vyrncore.palestra.ui.components.SimpleLineChart

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val (volumeByMuscleGroup, weeklyVolume) = viewModel.advancedStats.collectAsState().value

    Scaffold(topBar = { TopAppBar(title = { Text("Statistiche") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                uiState.exerciseOptions.forEach { (id, name) ->
                    FilterChip(
                        selected = id == uiState.selectedExerciseId,
                        onClick = { viewModel.selectExercise(id) },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            MetricCard(
                icon = Icons.Filled.EmojiEvents,
                value = "${uiState.personalRecordKg} kg",
                label = "Record personale",
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            if (uiState.history.size < 2) {
                Text(
                    "Registra almeno due allenamenti per vedere il grafico di progressione.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SimpleLineChart(
                        values = uiState.history.map { it.maxWeightKg },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            if (volumeByMuscleGroup.isNotEmpty()) {
                Text(
                    "Volume per gruppo muscolare",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SimpleBarChart(
                        entries = volumeByMuscleGroup.map { BarChartEntry(it.muscleGroup, it.totalVolumeKg) },
                        modifier = Modifier.padding(16.dp),
                        barColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            if (weeklyVolume.size >= 2) {
                Text(
                    "Andamento volume settimanale",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SimpleLineChart(
                        values = weeklyVolume.map { it.totalVolumeKg },
                        modifier = Modifier.padding(16.dp),
                        lineColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
