package com.vyrncore.palestra.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vyrncore.palestra.ui.components.DifficultyRank
import com.vyrncore.palestra.ui.components.MuscleGroupBadge
import com.vyrncore.palestra.util.youtubeTutorialSearchUrl
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onFinished: (sessionId: String, planId: String) -> Unit,
    onOpenRestTimer: (seconds: Int) -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prCelebration by viewModel.prCelebration.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var dialogExercise by remember { mutableStateOf<ActiveExerciseUi?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MISSIONE IN CORSO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onOpenRestTimer(90) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = "Timer di recupero")
                }
            },
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
            ) {
                val doneCount = uiState.exercises.count { it.completedSets >= it.targetSets }
                val overallProgress by animateFloatAsState(
                    targetValue = if (uiState.exercises.isNotEmpty()) doneCount / uiState.exercises.size.toFloat() else 0f,
                    animationSpec = tween(400),
                    label = "workoutOverallProgress",
                )

                // Progress Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PROGRESSO MISSIONE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "$doneCount / ${uiState.exercises.size} ESERCIZI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(overallProgress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.exercises, key = { it.planExerciseId }) { exercise ->
                        ActiveExerciseCard(
                            exercise = exercise,
                            onClick = { dialogExercise = exercise }
                        )
                    }
                }

                Button(
                    onClick = { viewModel.endWorkout { onFinished(viewModel.sessionId, viewModel.planId) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("TERMINA MISSIONE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        // In-app celebration when a set just beat every previous one: pops over the workout,
        // auto-dismisses, and taps anywhere to skip. Shown after returning from the rest timer.
        prCelebration?.let { celebration ->
            PrCelebrationOverlay(
                celebration = celebration,
                onDismiss = viewModel::dismissPrCelebration,
            )
        }
    }

    dialogExercise?.let { exercise ->
        LogSetDialog(
            exercise = exercise,
            onDismiss = { dialogExercise = null },
            onConfirm = { reps, weight ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.logSet(exercise.exerciseId, exercise.completedSets + 1, reps, weight)
                dialogExercise = null
                // Straight into recovery: jump to the rest timer with the plan's prescribed
                // rest, except after the final set of the exercise (nothing left to recover for).
                if (exercise.completedSets + 1 < exercise.targetSets && exercise.restSeconds > 0) {
                    onOpenRestTimer(exercise.restSeconds)
                }
            },
        )
    }
}

/** Full-screen celebration burst for a fresh personal record: confetti rain + a glass card
 * with the exercise and the new estimated 1RM. Auto-dismisses after a few seconds. */
@Composable
private fun PrCelebrationOverlay(
    celebration: PrCelebration,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(celebration) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(5000)
        onDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(220)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            ConfettiBurst(modifier = Modifier.fillMaxSize())

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f),
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Text(
                        "RECORD PERSONALE!",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        celebration.exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        "≈ ${"%.1f".format(celebration.estimatedOneRepMaxKg)} kg (1RM stimato)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Tocca per continuare",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val startX: Float,
    val drift: Float,
    val delayFraction: Float,
    val size: Float,
    val color: Color,
)

/** One-shot confetti rain drawn on a Canvas: 70 pieces fall over ~2.2s with per-particle
 * horizontal drift and staggered start times. Purely decorative, ignores input. */
@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFFFD700),
    )
    val particles = remember {
        List(70) {
            ConfettiParticle(
                startX = Random.nextFloat(),
                drift = Random.nextFloat() * 0.3f - 0.15f,
                delayFraction = Random.nextFloat() * 0.4f,
                size = Random.nextFloat() * 8f + 5f,
                color = colors[Random.nextInt(colors.size)],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(2200, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val fall = particle.delayFraction + progress.value * 1.2f
            if (fall in 0f..1f) {
                drawCircle(
                    color = particle.color,
                    radius = particle.size,
                    center = Offset(
                        x = (particle.startX + particle.drift * progress.value) * size.width,
                        y = fall * size.height,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    exercise: ActiveExerciseUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = exercise.completedSets >= exercise.targetSets
    val progress by animateFloatAsState(
        targetValue = (exercise.completedSets.toFloat() / exercise.targetSets.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "exerciseProgress",
    )
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = if (isDone) {
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (exercise.imageUrl != null) {
                        AsyncImage(
                            model = exercise.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        MuscleGroupBadge(group = exercise.muscleGroup ?: "", size = 64.dp)
                    }

                    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(
                            exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            DifficultyRank(difficulty = exercise.difficulty)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${exercise.completedSets}/${exercise.targetSets} SERIE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isDone) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Completato",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (!isDone) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Instructions / Technique Section
                    if (!exercise.notes.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    exercise.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { uriHandler.openUri(youtubeTutorialSearchUrl(exercise.name)) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "GUARDA TUTORIAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        Text(
                            "TARGET: ${exercise.targetReps} RIP." +
                                (exercise.targetWeightKg?.let { " × ${"%.1f".format(it)} KG" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar for the individual exercise
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

/** Set-logging dialog: numeric keyboards, +/- steppers for reps, target-weight prefill,
 * live validation and focus on the reps field on open, so logging a set is two taps. */
@Composable
private fun LogSetDialog(
    exercise: ActiveExerciseUi,
    onDismiss: () -> Unit,
    onConfirm: (reps: Int, weightKg: Double) -> Unit,
) {
    var reps by remember { mutableStateOf(exercise.targetReps.toString()) }
    var weight by remember {
        mutableStateOf(exercise.targetWeightKg?.let { "%.1f".format(it).trimEnd('0').trimEnd('.') } ?: "")
    }
    val repsValue = reps.toIntOrNull()
    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    val isValid = repsValue != null && repsValue > 0 && weightValue != null && weightValue > 0.0
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registra serie — ${exercise.name}") },
        text = {
            Column {
                StepperField(
                    label = "Ripetizioni",
                    value = reps,
                    onValueChange = { reps = it },
                    onStep = { reps = ((repsValue ?: exercise.targetReps) + it).coerceAtLeast(1).toString() },
                    isValid = repsValue != null && repsValue > 0,
                    focusRequester = focusRequester,
                )
                Spacer(modifier = Modifier.height(12.dp))
                StepperField(
                    label = "Peso (kg)",
                    value = weight,
                    onValueChange = { weight = it },
                    onStep = { weight = (((weightValue ?: 0.0) + it * 2.5).coerceAtLeast(0.0)).let { v -> "%.1f".format(v).trimEnd('0').trimEnd('.') } },
                    isValid = weightValue != null && weightValue > 0.0,
                    decimal = true,
                )
                exercise.targetWeightKg?.let { target ->
                    Text(
                        "Target del piano: ${"%.1f".format(target)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(repsValue ?: 0, weightValue ?: 0.0) },
                enabled = isValid,
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

@Composable
private fun StepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    isValid: Boolean,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    decimal: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = !isValid,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            ),
            modifier = Modifier
                .weight(1f)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        )
        IconButton(onClick = { onStep(-1) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Riduci $label")
        }
        IconButton(onClick = { onStep(1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Aumenta $label")
        }
    }
}
