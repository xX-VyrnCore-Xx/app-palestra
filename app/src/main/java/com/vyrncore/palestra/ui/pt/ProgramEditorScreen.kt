package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@Composable
fun ProgramEditorScreen(
    onSaved: () -> Unit,
    viewModel: ProgramEditorViewModel = hiltViewModel(),
) {
    val catalog by viewModel.exerciseCatalog.collectAsState()
    val draft by viewModel.draftExercises.collectAsState()
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

            Button(onClick = { showPicker = true }, modifier = Modifier.padding(top = 16.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Aggiungi esercizio", modifier = Modifier.padding(start = 4.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(draft, key = { it.exerciseId }) { exercise ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row {
                                Text(
                                    exercise.exerciseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.removeExercise(exercise.exerciseId) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Rimuovi")
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
