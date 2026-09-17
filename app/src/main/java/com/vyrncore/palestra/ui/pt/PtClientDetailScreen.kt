package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.MetricCard
import com.vyrncore.palestra.ui.components.SimpleLineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PtClientDetailScreen(
    onCreatePlan: (clientId: String) -> Unit,
    viewModel: PtClientDetailViewModel = hiltViewModel(),
) {
    val plans by viewModel.plans.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val bodyMetrics by viewModel.bodyMetrics.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ITALY) }

    val completedSessions = sessions.count { it.endedAtEpochMs != null }
    val lastActive = sessions.mapNotNull { it.endedAtEpochMs }.maxOrNull()
    val weightTrend = bodyMetrics.mapNotNull { it.weightKg }.asReversed()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dettaglio allievo") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onCreatePlan(viewModel.clientId) }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuova scheda")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    icon = Icons.Filled.FitnessCenter,
                    value = "$completedSessions",
                    label = "ALLENAMENTI SVOLTI",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Filled.CalendarMonth,
                    value = lastActive?.let { dateFormat.format(Date(it)) } ?: "Mai",
                    label = "ULTIMO ALLENAMENTO",
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            if (weightTrend.size >= 2) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Andamento peso", style = MaterialTheme.typography.titleSmall)
                        SimpleLineChart(
                            values = weightTrend,
                            modifier = Modifier.padding(top = 8.dp),
                            lineColor = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            Text(
                "Schede assegnate",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            if (plans.isEmpty()) {
                Text(
                    "Nessuna scheda assegnata ancora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                plans.forEach { plan ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Text(plan.name, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
