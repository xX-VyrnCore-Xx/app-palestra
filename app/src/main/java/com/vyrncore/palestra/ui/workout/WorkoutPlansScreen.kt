package com.vyrncore.palestra.ui.workout

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
fun WorkoutPlansScreen(
    onOpenSession: (sessionId: String, planId: String) -> Unit,
    viewModel: WorkoutPlansViewModel = hiltViewModel(),
) {
    val plans by viewModel.plans.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Le tue schede") }) },
    ) { padding ->
        if (plans.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Nessuna scheda assegnata ancora. Il tuo PT te ne assegnerà una a breve.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(plans, key = { it.id }) { plan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onClick = { viewModel.startSession(plan.id) { sessionId -> onOpenSession(sessionId, plan.id) } },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(plan.name, style = MaterialTheme.typography.titleMedium)
                            plan.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
    }
}
