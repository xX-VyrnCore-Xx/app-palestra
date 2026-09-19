package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vyrncore.palestra.data.local.entity.ExerciseEntity

@Composable
fun ExercisePickerDialog(
    catalog: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSelect: (ExerciseEntity) -> Unit,
    onCreateCustom: (name: String, muscleGroup: String, imageUrl: String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    val filtered = remember(catalog, query) {
        if (query.isBlank()) {
            catalog
        } else {
            catalog.filter {
                it.name.contains(query, ignoreCase = true) || it.muscleGroup.contains(query, ignoreCase = true)
            }
        }
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
                if (filtered.isEmpty()) {
                    Text(
                        "Nessun esercizio trovato",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(320.dp).padding(top = 8.dp)) {
                        items(filtered, key = { it.id }) { exercise ->
                            ListItem(
                                leadingContent = if (exercise.imageUrl != null) {
                                    {
                                        AsyncImage(
                                            model = exercise.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                        )
                                    }
                                } else {
                                    null
                                },
                                headlineContent = { Text(exercise.name) },
                                supportingContent = { Text(exercise.muscleGroup) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(exercise) },
                            )
                        }
                    }
                }
                TextButton(onClick = { showCreateDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Crea esercizio personalizzato", modifier = Modifier.padding(start = 4.dp))
                }
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
