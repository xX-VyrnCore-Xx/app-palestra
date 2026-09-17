package com.vyrncore.palestra.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
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
fun ActiveWorkoutScreen(
    onFinished: () -> Unit,
    onOpenRestTimer: (seconds: Int) -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var dialogExercise by remember { mutableStateOf<ActiveExerciseUi?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Allenamento in corso") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenRestTimer(90) }) {
                Icon(Icons.Filled.Timer, contentDescription = "Timer di recupero")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.exercises, key = { it.planExerciseId }) { exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        onClick = { dialogExercise = exercise },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${exercise.completedSets}/${exercise.targetSets} serie · target ${exercise.targetReps} rip.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { viewModel.endWorkout(onFinished) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text("Termina allenamento")
            }
        }
    }

    dialogExercise?.let { exercise ->
        LogSetDialog(
            exercise = exercise,
            onDismiss = { dialogExercise = null },
            onConfirm = { reps, weight ->
                viewModel.logSet(exercise.exerciseId, exercise.completedSets + 1, reps, weight)
                dialogExercise = null
            },
        )
    }
}

@Composable
private fun LogSetDialog(
    exercise: ActiveExerciseUi,
    onDismiss: () -> Unit,
    onConfirm: (reps: Int, weightKg: Double) -> Unit,
) {
    var reps by remember { mutableStateOf(exercise.targetReps.toString()) }
    var weight by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registra serie — ${exercise.name}") },
        text = {
            Column {
                OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Ripetizioni") })
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(reps.toIntOrNull() ?: 0, weight.toDoubleOrNull() ?: 0.0)
            }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
