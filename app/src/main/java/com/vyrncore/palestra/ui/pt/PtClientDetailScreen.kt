package com.vyrncore.palestra.ui.pt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.GradientHeader
import com.vyrncore.palestra.ui.components.MetricCard
import com.vyrncore.palestra.ui.components.SimpleLineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PtClientDetailScreen(
    onCreatePlan: (clientId: String) -> Unit,
    onCreateProgram: (clientId: String) -> Unit,
    onOpenChat: (clientId: String) -> Unit,
    viewModel: PtClientDetailViewModel = hiltViewModel(),
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val bodyMetrics by viewModel.bodyMetrics.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val injuries by viewModel.injuries.collectAsStateWithLifecycle()
    val clientName by viewModel.clientName.collectAsStateWithLifecycle()
    val allievoProfile by viewModel.allievoProfile.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ITALY) }

    var noteDraft by remember(note) { mutableStateOf(note?.content.orEmpty()) }
    var injuriesDraft by remember(injuries) { mutableStateOf(injuries.orEmpty()) }

    val completedSessions = sessions.count { it.endedAtEpochMs != null }
    val lastActive = sessions.mapNotNull { it.endedAtEpochMs }.maxOrNull()
    val weightTrend = bodyMetrics.mapNotNull { it.weightKg }.asReversed()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheda recluta") },
                actions = {
                    IconButton(onClick = { onOpenChat(viewModel.clientId) }) {
                        Icon(Icons.Filled.Forum, contentDescription = "Chat")
                    }
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                FloatingActionButton(onClick = { onCreateProgram(viewModel.clientId) }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Nuovo programma")
                }
                FloatingActionButton(
                    onClick = { onCreatePlan(viewModel.clientId) },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuova scheda")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
          if (clientName.isNotBlank()) {
            GradientHeader(
                title = clientName,
                subtitle = "$completedSessions allenamenti svolti",
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            )
          }
          AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 8 },
          ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (!injuries.isNullOrBlank()) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Icon(
                            Icons.Filled.HealthAndSafety,
                            contentDescription = null,
                            tint = if (!injuries.isNullOrBlank()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Infortuni e limitazioni",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (!injuries.isNullOrBlank()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        "Controllalo prima di assegnare esercizi: mal di schiena, lesioni, rotture, limitazioni fisiche.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    OutlinedTextField(
                        value = injuriesDraft,
                        onValueChange = { injuriesDraft = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        placeholder = { Text("Es. ernia L4-L5, ginocchio destro operato…") },
                        minLines = 2,
                    )
                    TextButton(
                        onClick = { viewModel.saveInjuries(injuriesDraft) },
                        enabled = injuriesDraft != injuries.orEmpty(),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("Salva")
                    }
                }
            }

            AllievoProfileCard(
                profile = allievoProfile,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                MetricCard(
                    icon = Icons.Filled.FitnessCenter,
                    value = "$completedSessions",
                    label = "ALLENAMENTI SVOLTI",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Filled.CalendarMonth,
                    value = lastActive?.let { dateFormat.format(Date(it)) } ?: "Mai",
                    label = "ULTIMO ALLENAMENTO",
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            if (weightTrend.size >= 2) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Andamento peso", style = MaterialTheme.typography.titleSmall)
                        SimpleLineChart(
                            values = weightTrend,
                            modifier = Modifier.padding(top = 8.dp),
                            lineColor = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            if (programs.isNotEmpty()) {
                Text(
                    "Programmi",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                programs.forEach { program ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(program.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${program.totalWeeks} settimane · +${program.weeklyIncrementPercent}%/settimana",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }

            Text(
                "Schede assegnate",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            val standalonePlans = plans.filter { it.programId == null }
            if (standalonePlans.isEmpty()) {
                Text(
                    "Nessuna scheda singola assegnata ancora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                standalonePlans.forEach { plan ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Text(plan.name, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            Text(
                "Note private",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Visibili solo a te, non all'allievo.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        placeholder = { Text("Osservazioni, obiettivi, infortuni…") },
                        minLines = 3,
                    )
                    TextButton(
                        onClick = { viewModel.saveNote(noteDraft) },
                        enabled = noteDraft != (note?.content ?: ""),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("Salva nota")
                    }
                }
            }
          }
          }
        }
    }
}

/** What the allievo told us in the Welcome questionnaire - shown to the PT to build a plan that
 * actually fits, not guessed from scratch. Absent until the allievo completes onboarding. */
@Composable
private fun AllievoProfileCard(
    profile: com.vyrncore.palestra.data.repository.AllievoPrivateProfile?,
    modifier: Modifier = Modifier,
) {
    if (profile == null || !profile.completedOnboarding) return

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Profilo recluta",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            val chips = listOfNotNull(
                profile.experienceLevel,
                profile.trainingDays,
                profile.primaryGoal,
                profile.activityLevel,
            )
            if (chips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 12.dp),
                ) {
                    chips.forEach { label ->
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }

            AllievoProfileNote(label = "Dolori/lesioni", value = profile.painInjuries)
            AllievoProfileNote(label = "Alimentazione", value = profile.nutrition)
            AllievoProfileNote(label = "Note", value = profile.goals)
        }
    }
}

@Composable
private fun AllievoProfileNote(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
    }
}
