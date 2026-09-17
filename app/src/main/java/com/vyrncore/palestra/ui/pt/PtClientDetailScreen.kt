package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PtClientDetailScreen(
    onCreatePlan: (clientId: String) -> Unit,
    viewModel: PtClientDetailViewModel = hiltViewModel(),
) {
    val plans by viewModel.plans.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dettaglio allievo") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onCreatePlan(viewModel.clientId) }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuova scheda")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Schede assegnate", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(plans, key = { it.id }) { plan ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(plan.name, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            Text(
                "Allenamenti svolti: ${sessions.count { it.endedAtEpochMs != null }}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
