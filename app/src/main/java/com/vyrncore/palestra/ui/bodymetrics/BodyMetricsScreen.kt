package com.vyrncore.palestra.ui.bodymetrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.EmptyState
import com.vyrncore.palestra.ui.components.SimpleLineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BodyMetricsScreen(viewModel: BodyMetricsViewModel = hiltViewModel()) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ITALY) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "Dati corporei",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi misurazione")
            }
        },
    ) { padding ->
        if (metrics.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.MonitorWeight,
                message = "Nessuna misurazione registrata ancora. Tocca + per aggiungerne una.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            val weightTrend = metrics.mapNotNull { it.weightKg }.asReversed()
            if (weightTrend.size >= 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
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
            }
            items(metrics, key = { it.id }) { metric ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.MonitorWeight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                dateFormat.format(Date(metric.dateEpochMs)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            metric.weightKg?.let { Text("$it kg", style = MaterialTheme.typography.titleMedium) }
                            val details = listOfNotNull(
                                metric.bodyFatPercent?.let { "Massa grassa $it%" },
                                metric.waistCm?.let { "Vita $it cm" },
                            ).joinToString(" · ")
                            if (details.isNotEmpty()) {
                                Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        LogMetricDialog(
            onDismiss = { showDialog = false },
            onConfirm = { weight, fat, waist ->
                viewModel.logWeight(weight, fat, waist)
                showDialog = false
            },
        )
    }
}

@Composable
private fun LogMetricDialog(
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Double, bodyFatPercent: Double?, waistCm: Double?) -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    // Blank/non-numeric weight used to silently save as 0.0 kg instead of stopping the user -
    // now Salva is disabled until a real, plausible weight is entered.
    val weightError = weight.isNotBlank() && (weightValue == null || weightValue <= 0.0 || weightValue > 400.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova misurazione") },
        text = {
            Column {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg)") },
                    isError = weightError,
                    supportingText = if (weightError) {
                        { Text("Inserisci un peso valido") }
                    } else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Massa grassa % (opzionale)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = waist,
                    onValueChange = { waist = it },
                    label = { Text("Vita cm (opzionale)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(weightValue!!, fat.replace(',', '.').toDoubleOrNull(), waist.replace(',', '.').toDoubleOrNull())
                },
                enabled = weightValue != null && !weightError,
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
