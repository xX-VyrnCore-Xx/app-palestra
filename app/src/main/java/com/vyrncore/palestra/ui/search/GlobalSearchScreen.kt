package com.vyrncore.palestra.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.EmptyState

@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    onStartSession: (sessionId: String, planId: String) -> Unit,
    onOpenClient: (clientId: String) -> Unit,
    onOpenChat: (peerId: String) -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        placeholder = { Text("Cerca schede, esercizi, persone...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { padding ->
        if (query.isBlank()) {
            EmptyState(
                icon = Icons.Filled.Search,
                message = "Cerca tra le tue schede, esercizi e persone.",
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else if (results.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Search,
                message = "Nessun risultato per \"$query\".",
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(results, key = { it.hashCode() }) { result ->
                    SearchResultRow(
                        result = result,
                        onClick = {
                            when (result) {
                                is SearchResult.Plan -> viewModel.startWorkout(result.planId) { sessionId ->
                                    onStartSession(sessionId, result.planId)
                                }
                                is SearchResult.Client -> onOpenClient(result.clientId)
                                is SearchResult.Contact -> onOpenChat(result.peerId)
                                is SearchResult.Exercise -> Unit
                            }
                        },
                    )
                }
            }
        }
    }
}

private data class SearchRowContent(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val trailingIcon: ImageVector? = null,
)

private fun SearchResult.toRowContent(): SearchRowContent = when (this) {
    is SearchResult.Plan -> SearchRowContent(Icons.Filled.FitnessCenter, name, subtitle, Icons.Filled.PlayArrow)
    is SearchResult.Exercise -> SearchRowContent(Icons.Filled.FitnessCenter, name, muscleGroup)
    is SearchResult.Client -> SearchRowContent(Icons.Filled.People, fullName, email)
    is SearchResult.Contact -> SearchRowContent(Icons.Filled.Forum, fullName, "Il tuo Personal Trainer", Icons.Filled.Forum)
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
    val content = result.toRowContent()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(content.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(content.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    content.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (content.trailingIcon != null) {
                Icon(content.trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
