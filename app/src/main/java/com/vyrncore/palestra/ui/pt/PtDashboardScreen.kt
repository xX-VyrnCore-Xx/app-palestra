package com.vyrncore.palestra.ui.pt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
fun PtDashboardScreen(
    onOpenClient: (clientId: String) -> Unit,
    viewModel: PtDashboardViewModel = hiltViewModel(),
) {
    val clients by viewModel.clients.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("I tuoi allievi") }) }) { padding ->
        if (clients.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Nessun allievo collegato ancora.")
                Text(
                    "Condividi il tuo ID PT (${viewModel.ptId}) con i tuoi allievi durante la registrazione.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(clients, key = { it.id }) { client ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        onClick = { onOpenClient(client.id) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(client.fullName, style = MaterialTheme.typography.titleMedium)
                            Text(client.email, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
