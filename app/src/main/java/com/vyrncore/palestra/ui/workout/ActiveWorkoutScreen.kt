package com.vyrncore.palestra.ui.workout

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@Composable
fun ActiveWorkoutScreen(
    onFinished: () -> Unit,
    onOpenRestTimer: (seconds: Int) -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var dialogExercise by remember { mutableStateOf<ActiveExerciseUi?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Missione in corso") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenRestTimer(90) }) {
                Icon(Icons.Filled.Timer, contentDescription = "Timer di recupero")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val doneCount = uiState.exercises.count { it.completedSets >= it.targetSets }
            val overallProgress by animateFloatAsState(
                targetValue = if (uiState.exercises.isNotEmpty()) doneCount / uiState.exercises.size.toFloat() else 0f,
                animationSpec = tween(400),
                label = "workoutOverallProgress",
            )
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Progresso missione", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "$doneCount/${uiState.exercises.size} esercizi",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.exercises, key = { it.planExerciseId }) { exercise ->
                    val isDone = exercise.completedSets >= exercise.targetSets
                    val exerciseProgress by animateFloatAsState(
                        targetValue = (exercise.completedSets.toFloat() / exercise.targetSets.coerceAtLeast(1)).coerceIn(0f, 1f),
                        animationSpec = tween(400),
                        label = "exerciseProgress",
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .animateItem(placementSpec = tween(220))
                            .animateContentSize(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 2.dp),
                        onClick = { dialogExercise = exercise },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (exercise.imageUrl != null) {
                                AsyncImage(
                                    model = exercise.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                if (isDone) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Completato",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            Text(
                                "${exercise.completedSets}/${exercise.targetSets} serie · target ${exercise.targetReps} rip.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LinearProgressIndicator(
                                progress = { exerciseProgress },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { viewModel.endWorkout(onFinished) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text("Termina missione")
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
