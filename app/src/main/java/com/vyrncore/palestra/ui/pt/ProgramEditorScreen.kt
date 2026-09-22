package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.EmptyState

@Composable
fun ProgramEditorScreen(
    onSaved: () -> Unit,
    viewModel: ProgramEditorViewModel = hiltViewModel(),
) {
    val catalog by viewModel.exerciseCatalog.collectAsStateWithLifecycle()
    val draft by viewModel.draftExercises.collectAsStateWithLifecycle()
    var programName by remember { mutableStateOf("") }
    var totalWeeks by remember { mutableStateOf(4) }
    var weeklyIncrementPercent by remember { mutableStateOf(2.5) }
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Nuovo programma") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "Genera automaticamente una scheda per ogni settimana, con il carico che aumenta progressivamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            OutlinedTextField(
                value = programName,
                onValueChange = { programName = it },
                label = { Text("Nome programma") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Stepper(
                    label = "Settimane",
                    value = totalWeeks,
                    onChange = { totalWeeks = it.coerceIn(2, 16) },
                    modifier = Modifier.weight(1f),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Incremento carico/settimana", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = weeklyIncrementPercent.toString(),
                        onValueChange = { weeklyIncrementPercent = it.toDoubleOrNull() ?: weeklyIncrementPercent },
                        suffix = { Text("%") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Button(
                onClick = { showPicker = true },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Aggiungi esercizio", modifier = Modifier.padding(start = 4.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                if (draft.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.CalendarViewWeek,
                            message = "Aggiungi gli esercizi che si ripeteranno ogni settimana: il carico crescerà da solo secondo l'incremento impostato sopra.",
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                }
                itemsIndexed(draft, key = { _, item -> item.exerciseId }) { index, exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Text(
                                    exercise.exerciseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                                )
                                IconButton(onClick = { viewModel.removeExercise(exercise.exerciseId) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Rimuovi", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                OutlinedTextField(
                                    value = exercise.targetSets.toString(),
                                    onValueChange = {
                                        viewModel.updateExercise(
                                            exercise.exerciseId,
                                            it.toIntOrNull() ?: exercise.targetSets,
                                            exercise.targetReps,
                                            exercise.targetWeightKg,
                                            exercise.restSeconds,
                                        )
                                    },
                                    label = { Text("Serie") },
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = exercise.targetReps.toString(),
                                    onValueChange = {
                                        viewModel.updateExercise(
                                            exercise.exerciseId,
                                            exercise.targetSets,
                                            it.toIntOrNull() ?: exercise.targetReps,
                                            exercise.targetWeightKg,
                                            exercise.restSeconds,
                                        )
                                    },
                                    label = { Text("Ripetizioni") },
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                )
                                OutlinedTextField(
                                    value = exercise.targetWeightKg?.toString() ?: "",
                                    onValueChange = {
                                        viewModel.updateExercise(
                                            exercise.exerciseId,
                                            exercise.targetSets,
                                            exercise.targetReps,
                                            it.toDoubleOrNull(),
                                            exercise.restSeconds,
                                        )
                                    },
                                    label = { Text("Kg iniziali") },
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.saveProgram(
                        name = programName,
                        totalWeeks = totalWeeks,
                        weeklyIncrementPercent = weeklyIncrementPercent,
                        category = null,
                        onSaved = onSaved,
                    )
                },
                enabled = programName.isNotBlank() && draft.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Genera programma ($totalWeeks settimane)")
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            catalog = catalog,
            onDismiss = { showPicker = false },
            onSelect = { exercise ->
                viewModel.addExercise(exercise.id, exercise.name)
                showPicker = false
            },
            onCreateCustom = { name, muscleGroup, imageUrl ->
                viewModel.createCustomExercise(name, muscleGroup, imageUrl)
                showPicker = false
            },
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = { onChange(value - 1) }) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onChange(value + 1) }) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}
