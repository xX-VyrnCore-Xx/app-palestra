package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
fun PlanEditorScreen(
    onSaved: () -> Unit,
    viewModel: PlanEditorViewModel = hiltViewModel(),
) {
    val catalog by viewModel.exerciseCatalog.collectAsState()
    val draft by viewModel.draftExercises.collectAsState()
    var planName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Nuova scheda") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = planName,
                onValueChange = { planName = it },
                label = { Text("Nome scheda") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
            ) {
                PLAN_CATEGORIES.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        label = { Text(category) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            Button(onClick = { showPicker = true }, modifier = Modifier.padding(top = 12.dp)) {
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
                                            exercise.restSeconds,
                                        )
                                    },
                                    label = { Text("Ripetizioni") },
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.savePlan(planName, description = null, category = selectedCategory, onSaved = onSaved) },
                enabled = planName.isNotBlank() && draft.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Assegna scheda")
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
        )
    }
}
