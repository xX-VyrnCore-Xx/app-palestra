package com.vyrncore.palestra.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.GradientHeader
import com.vyrncore.palestra.ui.components.MetricCard
import java.time.LocalTime

@Composable
fun HomeScreen(
    onStartSession: (sessionId: String, planId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Buongiorno"
        in 12..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            GradientHeader(
                title = "$greeting${if (uiState.fullName.isNotBlank()) ", ${uiState.fullName.substringBefore(' ')}" else ""}",
                subtitle = if (uiState.streakDays > 0) {
                    "🔥 ${uiState.streakDays} giorni di fila, continua così!"
                } else {
                    "Pronto per il prossimo allenamento?"
                },
            )

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                MetricCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = "${uiState.streakDays}",
                    label = "GIORNI DI STREAK",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = "${uiState.workoutsThisWeek}",
                    label = "ALLENAMENTI SETTIMANA",
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            if (uiState.nextPlanId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "PROSSIMO ALLENAMENTO",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            uiState.nextPlanName.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        )
                        Button(
                            onClick = {
                                viewModel.startWorkout(uiState.nextPlanId!!) { sessionId ->
                                    onStartSession(sessionId, uiState.nextPlanId!!)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text("INIZIA ORA", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Nessuna scheda assegnata ancora",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.longestStreakDays > 0) {
                Text(
                    "TRAGUARDI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    BADGE_MILESTONES.forEach { milestone ->
                        BadgeCircle(
                            days = milestone,
                            unlocked = uiState.unlockedBadges.contains(milestone),
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCircle(days: Int, unlocked: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (unlocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = if (unlocked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
        Text(
            "$days gg",
            style = MaterialTheme.typography.labelMedium,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
