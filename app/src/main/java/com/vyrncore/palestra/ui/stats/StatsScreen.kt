package com.vyrncore.palestra.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.BarChartEntry
import com.vyrncore.palestra.ui.components.MetricCard
import com.vyrncore.palestra.ui.components.PersonalRecordsCard
import com.vyrncore.palestra.ui.components.SimpleBarChart
import com.vyrncore.palestra.ui.components.SimpleLineChart
import com.vyrncore.palestra.ui.components.WeekOverWeekCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val (volumeByMuscleGroup, weeklyVolume) = viewModel.advancedStats.collectAsState().value
    val personalRecords by viewModel.personalRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STATISTICHE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                uiState.exerciseOptions.forEach { (id, name) ->
                    FilterChip(
                        selected = id == uiState.selectedExerciseId,
                        onClick = { viewModel.selectExercise(id) },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            MetricCard(
                icon = Icons.Filled.EmojiEvents,
                value = "${uiState.personalRecordKg} kg",
                label = "Record personale",
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.history.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                        .padding(24.dp)
                ) {
                    Text(
                        "Registra almeno due allenamenti per sbloccare i grafici di progressione.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "PROGRESSIONE CARICO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    letterSpacing = 1.sp
                )
                PremiumChartCard {
                    SimpleLineChart(
                        values = uiState.history.map { it.maxWeightKg },
                        modifier = Modifier.padding(16.dp),
                        lineColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (volumeByMuscleGroup.isNotEmpty()) {
                Text(
                    "VOLUME PER GRUPPO MUSCOLARE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    letterSpacing = 1.sp
                )
                PremiumChartCard {
                    SimpleBarChart(
                        entries = volumeByMuscleGroup.map { BarChartEntry(it.muscleGroup, it.totalVolumeKg) },
                        modifier = Modifier.padding(16.dp),
                        barColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            if (personalRecords.isNotEmpty()) {
                Text(
                    "RECORD PERSONALI (1RM STIMATO)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    letterSpacing = 1.sp
                )
                PersonalRecordsCard(records = personalRecords, modifier = Modifier.fillMaxWidth())
            }

            if (weeklyVolume.size >= 2) {
                val (previous, current) = weeklyVolume.takeLast(2)
                WeekOverWeekCard(
                    previousVolumeKg = previous.totalVolumeKg,
                    currentVolumeKg = current.totalVolumeKg,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )

                Text(
                    "ANDAMENTO VOLUME SETTIMANALE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    letterSpacing = 1.sp
                )
                PremiumChartCard {
                    SimpleLineChart(
                        values = weeklyVolume.map { it.totalVolumeKg },
                        modifier = Modifier.padding(16.dp),
                        lineColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(modifier = Modifier.padding(bottom = 32.dp))
        }
    }
}

@Composable
private fun PremiumChartCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.large
                )
        ) {
            content()
        }
    }
}
