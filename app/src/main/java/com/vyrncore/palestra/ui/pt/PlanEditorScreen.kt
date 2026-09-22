package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PlanEditorScreen(
    onSaved: () -> Unit,
    viewModel: PlanEditorViewModel = hiltViewModel(),
) {
    val catalog by viewModel.exerciseCatalog.collectAsStateWithLifecycle()
    val draft by viewModel.draftExercises.collectAsStateWithLifecycle()
    val clientInjuries by viewModel.clientInjuries.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val aiGenerating by viewModel.aiGenerating.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val aiSuggestedMeta by viewModel.aiSuggestedMeta.collectAsStateWithLifecycle()
    var planName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    // Pre-fills what the AI already decided instead of making the PT retype it: only touches
    // fields the PT hasn't already set by hand, so it never clobbers a name/category they chose
    // themselves before generating.
    androidx.compose.runtime.LaunchedEffect(aiSuggestedMeta) {
        val (suggestedName, suggestedCategory) = aiSuggestedMeta ?: return@LaunchedEffect
        if (planName.isBlank() && !suggestedName.isNullOrBlank()) planName = suggestedName
        if (selectedCategory == null && suggestedCategory != null && suggestedCategory in PLAN_CATEGORIES) {
            selectedCategory = suggestedCategory
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Nuova scheda") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (!clientInjuries.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                "Infortuni/limitazioni segnalate",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                clientInjuries.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

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

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Button(
                    onClick = { showPicker = true },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Aggiungi", modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(
                    onClick = { showTemplatePicker = true },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                ) {
                    Icon(Icons.Filled.Bookmark, contentDescription = null)
                    Text("Modello", modifier = Modifier.padding(start = 4.dp))
                }
            }

            Button(
                onClick = { showAiDialog = true },
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Text("Crea con AI", modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.Bold)
            }

            if (draft.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                ) {
                    // Same "~1.5 min per set" estimate used to fill estimatedMinutes on save
                    // (see PlanEditorViewModel.savePlan), shown live so the PT sees it forming.
                    val estimatedMinutes = draft.sumOf { it.targetSets } * 3 / 2
                    Text(
                        "ESERCIZI (${draft.size}) · ~$estimatedMinutes MIN",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showSaveTemplateDialog = true }) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Salva modello")
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
                if (draft.isEmpty()) {
                    item {
                        com.vyrncore.palestra.ui.components.EmptyState(
                            icon = Icons.Filled.FitnessCenter,
                            message = "Aggiungi esercizi dal catalogo, parti da un modello o lascia che l'AI proponga una scheda completa.",
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                }
                itemsIndexed(draft, key = { _, item -> item.exerciseId }) { index, exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
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
                                Column {
                                    IconButton(
                                        onClick = { viewModel.moveExercise(exercise.exerciseId, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(22.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.KeyboardArrowUp,
                                            contentDescription = "Sposta su",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.moveExercise(exercise.exerciseId, 1) },
                                        enabled = index < draft.lastIndex,
                                        modifier = Modifier.size(22.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Sposta giù",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            ) {
                                Text(
                                    "RIPOSO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.updateExercise(
                                            exercise.exerciseId,
                                            exercise.targetSets,
                                            exercise.targetReps,
                                            (exercise.restSeconds - 15).coerceAtLeast(15),
                                        )
                                    },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Riduci riposo", modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    "${exercise.restSeconds}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.updateExercise(
                                            exercise.exerciseId,
                                            exercise.targetSets,
                                            exercise.targetReps,
                                            exercise.restSeconds + 15,
                                        )
                                    },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Aumenta riposo", modifier = Modifier.size(18.dp))
                                }
                            }
                            OutlinedTextField(
                                value = exercise.notes.orEmpty(),
                                onValueChange = { viewModel.updateExerciseNote(exercise.exerciseId, it) },
                                label = { Text("Nota (opzionale)") },
                                placeholder = { Text("Es. tempo 3-1-1, o cedimento all'ultima serie") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
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
            onCreateCustom = { name, muscleGroup, imageUrl ->
                viewModel.createCustomExercise(name, muscleGroup, imageUrl)
                showPicker = false
            },
        )
    }

    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("Usa un modello") },
            text = {
                if (templates.isEmpty()) {
                    Text("Nessun modello salvato ancora. Costruisci una scheda e salvala come modello per riusarla.")
                } else {
                    Column {
                        templates.forEach { template ->
                            Text(
                                template.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.applyTemplate(template.id)
                                        showTemplatePicker = false
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Chiudi") }
            },
        )
    }

    if (showAiDialog) {
        var goal by remember { mutableStateOf("") }
        // Auto-closes once generation finishes successfully, so the PT lands straight on the
        // populated draft instead of having to dismiss the dialog by hand.
        androidx.compose.runtime.LaunchedEffect(aiGenerating) {
            if (!aiGenerating && draft.isNotEmpty() && aiError == null && goal.isNotBlank()) {
                showAiDialog = false
            }
        }
        AlertDialog(
            onDismissRequest = { if (!aiGenerating) { showAiDialog = false; viewModel.clearAiError() } },
            title = { Text("Crea scheda con AI") },
            text = {
                Column {
                    Text(
                        "Descrivi l'obiettivo (es. \"push/pull/legs, 4 giorni a settimana, ipertrofia, livello intermedio\"). " +
                            "L'AI propone gli esercizi dal catalogo, che potrai comunque modificare prima di salvare.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text("Obiettivo") },
                        enabled = !aiGenerating,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    if (aiGenerating) {
                        Row(modifier = Modifier.padding(top = 12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            Text("Generazione in corso…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    aiError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.generateWithAi(goal) },
                    enabled = goal.isNotBlank() && !aiGenerating,
                ) { Text("Genera") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAiDialog = false; viewModel.clearAiError() },
                    enabled = !aiGenerating,
                ) { Text("Chiudi") }
            },
        )
    }

    if (showSaveTemplateDialog) {
        var templateName by remember { mutableStateOf(planName) }
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("Salva come modello") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Nome modello") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveAsTemplate(templateName, selectedCategory)
                        showSaveTemplateDialog = false
                    },
                    enabled = templateName.isNotBlank(),
                ) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) { Text("Annulla") }
            },
        )
    }
}
