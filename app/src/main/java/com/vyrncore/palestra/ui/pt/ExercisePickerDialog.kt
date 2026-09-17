package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.data.local.entity.ExerciseEntity

@Composable
fun ExercisePickerDialog(
    catalog: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSelect: (ExerciseEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
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
                                headlineContent = { Text(exercise.name) },
                                supportingContent = { Text(exercise.muscleGroup) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(exercise) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        },
    )
}
