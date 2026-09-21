package com.vyrncore.palestra.ui.pt

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vyrncore.palestra.data.local.ExerciseDifficulty
import com.vyrncore.palestra.data.local.ExerciseCatalogSeed
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.ui.components.DifficultyRank
import com.vyrncore.palestra.ui.components.MuscleGroupBadge
import com.vyrncore.palestra.util.youtubeTutorialSearchUrl

@Composable
fun ExercisePickerDialog(
    catalog: List<ExerciseEntity>,
    selectedIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSelect: (ExerciseEntity) -> Unit,
    onCreateCustom: (name: String, muscleGroup: String, imageUrl: String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var groupFilter by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val groups = remember(catalog) {
        ExerciseCatalogSeed.groupOrder.filter { g -> catalog.any { it.muscleGroup == g } } +
            catalog.map { it.muscleGroup }.filter { it !in ExerciseCatalogSeed.groupOrder }.distinct()
    }

    val filtered = remember(catalog, query, groupFilter) {
        catalog
            .filter { groupFilter == null || it.muscleGroup == groupFilter }
            .filter { query.isBlank() || it.name.contains(query, true) || it.muscleGroup.contains(query, true) }
            .sortedWith(
                compareBy(
                    { ExerciseCatalogSeed.groupOrder.indexOf(it.muscleGroup).let { i -> if (i < 0) 99 else i } },
                    { it.name },
                ),
            )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi esercizio") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cerca per nome o gruppo muscolare") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    FilterChip(
                        selected = groupFilter == null,
                        onClick = { groupFilter = null },
                        label = { Text("Tutti") },
                    )
                    groups.forEach { group ->
                        FilterChip(
                            selected = groupFilter == group,
                            onClick = { groupFilter = if (groupFilter == group) null else group },
                            label = { Text(group) },
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    Text(
                        "Nessun esercizio trovato",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(340.dp).padding(top = 8.dp)) {
                        items(filtered, key = { it.id }) { exercise ->
                            val alreadyAdded = exercise.id in selectedIds
                            ListItem(
                                leadingContent = {
                                    if (exercise.imageUrl != null) {
                                        AsyncImage(
                                            model = exercise.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                                        )
                                    } else {
                                        MuscleGroupBadge(group = exercise.muscleGroup ?: "", size = 48.dp)
                                    }
                                },
                                headlineContent = {
                                    Text(exercise.name, fontWeight = if (alreadyAdded) FontWeight.Normal else FontWeight.Medium)
                                },
                                supportingContent = {
                                    Column {
                                        Text(
                                            listOfNotNull(
                                                exercise.muscleGroup,
                                                exercise.equipment?.let { "· $it" },
                                                exercise.difficulty?.let { "· ${it.lowercase()}" },
                                            ).joinToString(" "),
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                        exercise.notes?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        DifficultyRank(difficulty = exercise.difficulty)
                                        val uriHandler = LocalUriHandler.current
                                        IconButton(onClick = { uriHandler.openUri(youtubeTutorialSearchUrl(exercise.name)) }) {
                                            Icon(Icons.Filled.OndemandVideo, contentDescription = "Cerca tutorial video")
                                        }
                                        if (alreadyAdded) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = "Già nella scheda",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .clickable(enabled = !alreadyAdded) { onSelect(exercise) },
                            )
                        }
                    }
                }
                AssistChip(
                    onClick = { showCreateDialog = true },
                    label = { Text("Crea esercizio personalizzato") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        },
    )

    if (showCreateDialog) {
        CreateCustomExerciseDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, muscleGroup, imageUrl ->
                onCreateCustom(name, muscleGroup, imageUrl)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CreateCustomExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, muscleGroup: String, imageUrl: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo esercizio") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    label = { Text("Gruppo muscolare") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL immagine/GIF dimostrativa (opzionale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), muscleGroup.trim(), imageUrl.trim().takeIf { it.isNotBlank() }) },
                enabled = name.isNotBlank() && muscleGroup.isNotBlank(),
            ) { Text("Crea") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
