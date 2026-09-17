package com.vyrncore.palestra.ui.bodymetrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BodyMetricsScreen(viewModel: BodyMetricsViewModel = hiltViewModel()) {
    val metrics by viewModel.metrics.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ITALY) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dati corporei") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi misurazione")
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(metrics, key = { it.id }) { metric ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(dateFormat.format(Date(metric.dateEpochMs)), style = MaterialTheme.typography.bodyMedium)
                        metric.weightKg?.let { Text("Peso: $it kg", style = MaterialTheme.typography.titleMedium) }
                        metric.bodyFatPercent?.let { Text("Massa grassa: $it%") }
                        metric.waistCm?.let { Text("Vita: $it cm") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova misurazione") },
        text = {
            Column {
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso (kg)") })
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Massa grassa % (opzionale)") },
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = waist,
                    onValueChange = { waist = it },
                    label = { Text("Vita cm (opzionale)") },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(weight.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull(), waist.toDoubleOrNull())
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
