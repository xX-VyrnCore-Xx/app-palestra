package com.vyrncore.palestra.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.data.repository.PlotoneFeedPost
import com.vyrncore.palestra.data.repository.WeeklyRankingEntry
import com.vyrncore.palestra.ui.components.BadgeTier
import com.vyrncore.palestra.ui.components.BarChartEntry
import com.vyrncore.palestra.ui.components.CircularProgressRing
import com.vyrncore.palestra.ui.components.ConnectionStatusBar
import com.vyrncore.palestra.ui.components.GradientHeader
import com.vyrncore.palestra.ui.components.MetricCard
import com.vyrncore.palestra.ui.components.PersonalRecordsCard
import com.vyrncore.palestra.ui.components.ProgressTrendCard
import com.vyrncore.palestra.ui.components.RealisticBadge
import com.vyrncore.palestra.ui.components.pressScale
import com.vyrncore.palestra.ui.components.SimpleBarChart
import com.vyrncore.palestra.ui.components.SimpleLineChart
import com.vyrncore.palestra.ui.components.WeekOverWeekCard
import com.vyrncore.palestra.ui.theme.Gold40
import com.vyrncore.palestra.ui.theme.Gold50
import com.vyrncore.palestra.ui.home.HomeSuggestionAction.Assistant
import com.vyrncore.palestra.ui.home.HomeSuggestionAction.ChatPt
import com.vyrncore.palestra.ui.home.HomeSuggestionAction.History
import com.vyrncore.palestra.ui.home.HomeSuggestionAction.StartWorkout
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartSession: (sessionId: String, planId: String) -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val weeklyRanking by viewModel.weeklyRanking.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val weeklyVolume by viewModel.weeklyVolume.collectAsStateWithLifecycle()
    val volumeByMuscleGroup by viewModel.volumeByMuscleGroup.collectAsStateWithLifecycle()
    val personalRecords by viewModel.personalRecords.collectAsStateWithLifecycle()
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Buongiorno"
        in 12..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }

    // The parent dashboard's own Scaffold (bottomBar) already applies the safe-area insets to
    // every tab's content; without this override, this nested Scaffold re-applied them again,
    // stacking a second status-bar-height gap on top of the first - most visible here since Home
    // has no topBar to visually absorb the extra space.
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
      PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.padding(padding),
      ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .verticalScroll(rememberScrollState()),
        ) {
            ConnectionStatusBar(isOnline = isOnline, isSyncing = isSyncing)
            Box {
                GradientHeader(
                    title = "$greeting${if (uiState.fullName.isNotBlank()) ", ${uiState.fullName.substringBefore(' ')}" else ""}",
                    subtitle = if (uiState.streakDays > 0) {
                        "🔥 ${uiState.streakDays} giorni di fila, continua così!"
                    } else {
                        "Pronto per il prossimo allenamento?"
                    },
                )
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 12.dp),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Cerca", tint = Color.White)
                }
            }

            RankCard(
                level = uiState.level,
                rankTitle = uiState.levelTitle,
                stars = rankStars(uiState.level),
                xpIntoLevel = uiState.xpIntoLevel,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            )

            val animatedStreak by animateIntAsState(
                targetValue = uiState.streakDays,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "streakDays",
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                MetricCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = "$animatedStreak",
                    label = "GIORNI DI FILA",
                    modifier = Modifier.weight(1f),
                )
                WeeklyGoalCard(
                    completed = uiState.workoutsThisWeek,
                    goal = WEEKLY_GOAL,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            WeeklyComparisonRow(
                thisWeek = uiState.workoutsThisWeek,
                lastWeek = uiState.workoutsLastWeek,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            SuggestionsSection(
                suggestions = uiState.suggestions,
                onSuggestionTap = { suggestion ->
                    when (suggestion.action) {
                        StartWorkout -> {
                            val planId = uiState.nextPlanId ?: return@SuggestionsSection
                            uiState.sessionsWithPending[planId]?.let { sessionId ->
                                onStartSession(sessionId, planId)
                            } ?: viewModel.startWorkout(planId) { sessionId ->
                                onStartSession(sessionId, planId)
                            }
                        }
                        History -> onOpenHistory()
                        Assistant -> onOpenAssistant()
                        ChatPt -> onOpenChat()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )

            if (weeklyRanking.isNotEmpty()) {
                WeeklyRankingCard(
                    ranking = weeklyRanking,
                    myName = uiState.fullName.substringBefore(' '),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            if (feed.isNotEmpty()) {
                PlotoneFeedCard(
                    posts = feed,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (uiState.nextPlanId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shape = MaterialTheme.shapes.large,
                            )
                            .padding(20.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                "PROSSIMO ALLENAMENTO",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                        Text(
                            uiState.nextPlanName.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        if (uiState.activeProgramName != null) {
                            Text(
                                "${uiState.activeProgramName} · Settimana ${uiState.activeProgramCurrentWeek} di ${uiState.activeProgramTotalWeeks}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Button(
                            onClick = {
                                viewModel.startWorkout(uiState.nextPlanId!!) { sessionId ->
                                    onStartSession(sessionId, uiState.nextPlanId!!)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text("INIZIA ALLENAMENTO", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            } else {
                NoMissionCard(
                    onOpenAssistant = onOpenAssistant,
                    onOpenChat = onOpenChat,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }

            Text(
                "I TUOI PROGRESSI",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                QuickLinkCard(
                    icon = Icons.Filled.History,
                    label = "Cronologia",
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f),
                )
                QuickLinkCard(
                    icon = Icons.Filled.CalendarMonth,
                    label = "Calendario",
                    onClick = onOpenCalendar,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            if (weeklyVolume.size >= 2) {
                val (previous, current) = weeklyVolume.takeLast(2)
                WeekOverWeekCard(
                    previousVolumeKg = previous.totalVolumeKg,
                    currentVolumeKg = current.totalVolumeKg,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (weeklyVolume.size >= 3) {
                ProgressTrendCard(
                    weeklyVolume = weeklyVolume,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (weeklyVolume.size >= 2) {
                Text(
                    "Andamento volume settimanale",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SimpleLineChart(
                        values = weeklyVolume.map { it.totalVolumeKg },
                        modifier = Modifier.padding(16.dp),
                        lineColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            if (volumeByMuscleGroup.isNotEmpty()) {
                Text(
                    "Volume per gruppo muscolare",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SimpleBarChart(
                        entries = volumeByMuscleGroup.map { BarChartEntry(it.muscleGroup, it.totalVolumeKg) },
                        modifier = Modifier.padding(16.dp),
                        barColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            if (personalRecords.isNotEmpty()) {
                Text(
                    "Record personali (1RM stimato)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                )
                PersonalRecordsCard(
                    records = personalRecords,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            Text(
                "MEDAGLIERE",
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
                MedalSummaryCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    title = "Servizio",
                    unlockedCount = uiState.unlockedBadges.size,
                    totalCount = BADGE_MILESTONES.size,
                    nextMilestone = BADGE_MILESTONES.firstOrNull { it > uiState.longestStreakDays },
                    currentValue = uiState.longestStreakDays,
                    suffix = "gg",
                    modifier = Modifier.padding(end = 12.dp),
                )
                MedalSummaryCard(
                    icon = Icons.Filled.MilitaryTech,
                    title = "Operative",
                    unlockedCount = uiState.unlockedWorkoutCountBadges.size,
                    totalCount = WORKOUT_COUNT_MILESTONES.size,
                    nextMilestone = WORKOUT_COUNT_MILESTONES.firstOrNull { it > uiState.totalWorkouts },
                    currentValue = uiState.totalWorkouts,
                    suffix = "",
                    modifier = Modifier.padding(end = 12.dp),
                )
                MedalSummaryCard(
                    icon = Icons.Filled.Star,
                    title = "Potenza",
                    unlockedCount = uiState.unlockedVolumeBadges.size,
                    totalCount = VOLUME_MILESTONES_KG.size,
                    nextMilestone = VOLUME_MILESTONES_KG.firstOrNull { it > uiState.totalVolumeKg },
                    currentValue = uiState.totalVolumeKg.toInt(),
                    suffix = " kg",
                    modifier = Modifier.padding(end = 12.dp, bottom = 8.dp),
                )
            }
        }
      }
    }
}

/** The "Ordini del giorno" block: context-aware, tappable nudges computed from the allievo's real
 * data. Every card is actionable — no dead-end chips: StartWorkout launches (or resumes) the
 * session, the rest deep-link to the screen that solves the nudge. */
@Composable
private fun SuggestionsSection(
    suggestions: List<HomeSuggestion>,
    onSuggestionTap: (HomeSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            "PER TE OGGI",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        suggestions.forEach { suggestion ->
            HomeActionCard(
                suggestion = suggestion,
                onClick = { onSuggestionTap(suggestion) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

/** One tappable suggestion: icon, title, one-line why, and a chevron so it clearly reads as a
 * button. Gives a light haptic tick plus the shared press-scale bounce on tap. */
@Composable
private fun HomeActionCard(
    suggestion: HomeSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .pressScale(interactionSource),
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                suggestion.action.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    suggestion.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Icon shown on a suggestion card, matched to the action the card deep-links into. */
private fun HomeSuggestionAction.icon() = when (this) {
    StartWorkout -> Icons.Filled.FitnessCenter
    History -> Icons.Filled.History
    Assistant -> Icons.Filled.AutoAwesome
    ChatPt -> Icons.Filled.Chat
}

/** Dead-end replacement for the old generic empty-state label: explains what's missing and offers
 * two tappable ways out — asking the PT for a plan, or letting the assistant suggest one. */
@Composable
private fun NoMissionCard(
    onOpenAssistant: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Replay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Nessun allenamento assegnato ancora",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Il PT può prepararti una scheda su misura: intanto l'assistente ti suggerisce come non fermarti.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                Button(onClick = onOpenChat, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Chiedi al PT", modifier = Modifier.padding(start = 6.dp))
                }
                Button(
                    onClick = onOpenAssistant,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Assistente", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/** A compact entry point into a full-screen destination that used to live in its own "Progressi"
 * tab - folded into Home so the allievo never has to hunt for a separate nav slot for it. */
@Composable
private fun QuickLinkCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Collapses a whole milestone-badge row into a single compact card: how many are unlocked out of
 * the total, plus a one-line "how far to the next one" readout - replacing three separate
 * horizontally-scrolling rows of mostly-locked circles that used to dominate the screen. */
@Composable
private fun MedalSummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    unlockedCount: Int,
    totalCount: Int,
    nextMilestone: Int?,
    currentValue: Int,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    val tier = when {
        unlockedCount == 0 -> BadgeTier.LOCKED
        unlockedCount >= totalCount -> BadgeTier.PLATINUM
        unlockedCount >= totalCount / 2 -> BadgeTier.GOLD
        unlockedCount >= 2 -> BadgeTier.SILVER
        else -> BadgeTier.BRONZE
    }

    Card(
        modifier = modifier.width(160.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RealisticBadge(
                icon = icon,
                tier = tier,
                size = 48.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "$unlockedCount / $totalCount Medaglie",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (nextMilestone != null) {
                val progress = (currentValue.toFloat() / nextMilestone).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                    )
                }
                Text(
                    "Prossima: $currentValue/$nextMilestone$suffix",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    "Livello massimo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun RankCard(level: Int, rankTitle: String, stars: Int, xpIntoLevel: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = xpIntoLevel / 100f,
        animationSpec = tween(600),
        label = "xpProgress",
    )
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            rankTitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Livello $level",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (stars > 0) {
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                repeat(stars) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Gold50,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Filled.MilitaryTech,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "XP LIVELLO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$xpIntoLevel / 100 XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyGoalCard(completed: Int, goal: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = (completed.toFloat() / goal).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "weeklyGoalProgress",
    )
    val isComplete = completed >= goal

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = if (isComplete) {
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isComplete) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.large
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "OBIETTIVO SETTIMANALE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isComplete) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            if (isComplete) "Missione Compiuta!" else "Ancora $completed su $goal missioni",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        if (isComplete) Icons.Filled.AutoAwesome else Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = if (isComplete) Gold50 else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CircularProgressRing(
                    progress = progress,
                    size = 104.dp,
                    strokeWidth = 12.dp,
                    trackColor = (if (isComplete) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        .copy(alpha = 0.12f),
                    progressBrush = Brush.sweepGradient(
                        if (isComplete) listOf(Gold50, Gold40, Gold50) else listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.primary,
                        )
                    ),
                ) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (isComplete) {
                    Text(
                        "Complimenti, hai superato le aspettative questa settimana!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

/** A quick "are you improving" read at a glance: how this week's workout count stacks up
 * against last week's, without making the allievo dig into the Statistiche tab for it. */
@Composable
private fun WeeklyComparisonRow(thisWeek: Int, lastWeek: Int, modifier: Modifier = Modifier) {
    val delta = thisWeek - lastWeek
    val (icon, tint, message) = when {
        lastWeek == 0 && thisWeek == 0 -> Triple(Icons.Filled.TrendingFlat, MaterialTheme.colorScheme.onSurfaceVariant, "Nessun allenamento ancora questa settimana")
        delta > 0 -> Triple(Icons.Filled.TrendingUp, MaterialTheme.colorScheme.tertiary, "+$delta rispetto alla settimana scorsa")
        delta < 0 -> Triple(Icons.Filled.TrendingDown, MaterialTheme.colorScheme.error, "$delta rispetto alla settimana scorsa")
        else -> Triple(Icons.Filled.TrendingFlat, MaterialTheme.colorScheme.onSurfaceVariant, "Stesso ritmo della settimana scorsa")
    }
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** How long ago a feed post's ISO 8601 timestamp was, in a short Italian phrasing. */
private fun timeAgo(iso: String?): String {
    val instant = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return ""
    val minutes = Duration.between(instant, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "adesso"
        minutes < 60 -> "${minutes}m fa"
        minutes < 24 * 60 -> "${minutes / 60}h fa"
        else -> "${minutes / (24 * 60)}gg fa"
    }
}

/** Auto-posted activity feed shared by every allievo of the same PT — a light social nudge each
 * time someone completes a workout, built on top of a table only readable by same-PT peers. */
@Composable
private fun PlotoneFeedCard(posts: List<PlotoneFeedPost>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DynamicFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Bacheca del team",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            posts.take(6).forEach { post ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        post.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        post.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        timeAgo(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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
                    Icons.Filled.MilitaryTech,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    "Classifica del team",
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
