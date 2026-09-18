package com.vyrncore.palestra.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.data.repository.WeeklyRankingEntry
import com.vyrncore.palestra.ui.components.ConnectionStatusBar
import com.vyrncore.palestra.ui.components.GradientHeader
import com.vyrncore.palestra.ui.components.MetricCard
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartSession: (sessionId: String, planId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val weeklyRanking by viewModel.weeklyRanking.collectAsState()
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Buongiorno"
        in 12..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }

    Scaffold { padding ->
      PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.padding(padding),
      ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ConnectionStatusBar(isOnline = isOnline, isSyncing = isSyncing)
            GradientHeader(
                title = "$greeting${if (uiState.fullName.isNotBlank()) ", ${uiState.fullName.substringBefore(' ')}" else ""}",
                subtitle = if (uiState.streakDays > 0) {
                    "🔥 ${uiState.streakDays} giorni di fila, continua così!"
                } else {
                    "Pronto per il prossimo allenamento?"
                },
            )

            LevelCard(
                level = uiState.level,
                levelTitle = uiState.levelTitle,
                xpIntoLevel = uiState.xpIntoLevel,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            )

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                MetricCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = "${uiState.streakDays}",
                    label = "GIORNI DI STREAK",
                    modifier = Modifier.weight(1f),
                )
                WeeklyGoalCard(
                    completed = uiState.workoutsThisWeek,
                    goal = WEEKLY_GOAL,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            if (weeklyRanking.isNotEmpty()) {
                WeeklyRankingCard(
                    ranking = weeklyRanking,
                    myName = uiState.fullName.substringBefore(' '),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            if (uiState.nextPlanId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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

            if (uiState.unlockedBadges.isNotEmpty() || BADGE_MILESTONES.isNotEmpty()) {
                BadgeSection(
                    title = "TRAGUARDI DI COSTANZA",
                    icon = Icons.Filled.EmojiEvents,
                    milestones = BADGE_MILESTONES,
                    unlocked = uiState.unlockedBadges,
                    suffix = "gg",
                )
            }

            BadgeSection(
                title = "TRAGUARDI DI ALLENAMENTO",
                icon = Icons.Filled.Star,
                milestones = WORKOUT_COUNT_MILESTONES,
                unlocked = uiState.unlockedWorkoutCountBadges,
                suffix = "",
            )

            BadgeSection(
                title = "TRAGUARDI DI VOLUME",
                icon = Icons.Filled.FitnessCenter,
                milestones = VOLUME_MILESTONES_KG,
                unlocked = uiState.unlockedVolumeBadges,
                suffix = " kg",
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
      }
    }
}

@Composable
private fun LevelCard(level: Int, levelTitle: String, xpIntoLevel: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = xpIntoLevel / 100f,
        animationSpec = tween(600),
        label = "xpProgress",
    )
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Livello $level · $levelTitle", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$xpIntoLevel / 100 XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.tertiary),
                )
            }
        }
    }
}

@Composable
private fun WeeklyGoalCard(completed: Int, goal: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = (completed.toFloat() / goal).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "weeklyGoalProgress",
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.35f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                    )
                }
                Text(
                    "$completed/$goal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                "OBIETTIVO SETTIMANALE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BadgeSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    milestones: List<Int>,
    unlocked: List<Int>,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            milestones.forEach { milestone ->
                BadgeCircle(
                    icon = icon,
                    label = "$milestone$suffix",
                    unlocked = unlocked.contains(milestone),
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun BadgeCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
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
                icon,
                contentDescription = null,
                tint = if (unlocked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Where you stand among the other allievi of the same PT this week — a light nudge, built from
 * a server-side function that only ever returns first names + a count, never other users' data. */
@Composable
private fun WeeklyRankingCard(ranking: List<WeeklyRankingEntry>, myName: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(top = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    "Classifica della settimana",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            ranking.take(5).forEachIndexed { index, entry ->
                val isMe = entry.displayName.equals(myName, ignoreCase = true)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        if (isMe) "${entry.displayName} (tu)" else entry.displayName,
                        style = if (isMe) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        " ${entry.workoutsThisWeek}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}
