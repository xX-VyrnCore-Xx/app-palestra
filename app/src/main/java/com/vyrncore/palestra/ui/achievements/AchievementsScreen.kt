package com.vyrncore.palestra.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyrncore.palestra.ui.components.BadgeTier
import com.vyrncore.palestra.ui.components.RealisticBadge
import com.vyrncore.palestra.ui.home.BADGE_MILESTONES
import com.vyrncore.palestra.ui.home.HomeViewModel
import com.vyrncore.palestra.ui.home.VOLUME_MILESTONES_KG
import com.vyrncore.palestra.ui.home.WORKOUT_COUNT_MILESTONES
import com.vyrncore.palestra.ui.home.XP_PER_LEVEL
import java.util.Locale

/**
 * Every individual milestone badge, locked and unlocked, across the three tracks the Home screen
 * already computes (streak, total workouts, total volume) - Home only ever showed one summary
 * medal per track, so this is the first place an allievo can see the whole collection at once.
 * Reuses `HomeViewModel` rather than duplicating its streak/XP math in a second ViewModel.
 */
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Traguardi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                LevelCard(
                    level = uiState.level,
                    levelTitle = uiState.levelTitle,
                    xpIntoLevel = uiState.xpIntoLevel,
                )
            }
            item {
                AchievementSection(
                    title = "Costanza",
                    icon = Icons.Filled.LocalFireDepartment,
                    milestones = BADGE_MILESTONES,
                    currentValue = uiState.longestStreakDays,
                    labelFor = { "$it gg" },
                )
            }
            item {
                AchievementSection(
                    title = "Allenamenti",
                    icon = Icons.Filled.MilitaryTech,
                    milestones = WORKOUT_COUNT_MILESTONES,
                    currentValue = uiState.totalWorkouts,
                    labelFor = { "$it" },
                )
            }
            item {
                AchievementSection(
                    title = "Potenza",
                    icon = Icons.Filled.Star,
                    milestones = VOLUME_MILESTONES_KG,
                    currentValue = uiState.totalVolumeKg.toInt(),
                    labelFor = ::formatVolumeLabel,
                )
            }
        }
    }
}

private fun formatVolumeLabel(kg: Int): String =
    if (kg >= 1000) String.format(Locale.ITALY, "%.0fk", kg / 1000f) else "$kg"

@Composable
private fun LevelCard(level: Int, levelTitle: String, xpIntoLevel: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                    ),
                ),
            )
            .padding(20.dp),
    ) {
        Text(
            "LIVELLO $level",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
        )
        Text(
            levelTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(xpIntoLevel.coerceIn(0, XP_PER_LEVEL) / XP_PER_LEVEL.toFloat())
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
            )
        }
        Text(
            "$xpIntoLevel / $XP_PER_LEVEL XP al prossimo livello",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AchievementSection(
    title: String,
    icon: ImageVector,
    milestones: List<Int>,
    currentValue: Int,
    labelFor: (Int) -> String,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        milestones.chunked(3).forEach { rowMilestones ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                rowMilestones.forEach { milestone ->
                    val index = milestones.indexOf(milestone)
                    val unlocked = currentValue >= milestone
                    val tier = when {
                        !unlocked -> BadgeTier.LOCKED
                        index >= milestones.size - 1 -> BadgeTier.PLATINUM
                        index >= milestones.size - 3 -> BadgeTier.GOLD
                        index >= milestones.size - 5 -> BadgeTier.SILVER
                        else -> BadgeTier.BRONZE
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(84.dp),
                    ) {
                        RealisticBadge(icon = icon, tier = tier, size = 56.dp)
                        Text(
                            labelFor(milestone),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (unlocked) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                // Pads the last, possibly-incomplete row so its badges stay left-aligned with the
                // ones above instead of centering with extra gaps.
                repeat(3 - rowMilestones.size) {
                    Column(modifier = Modifier.width(84.dp)) {}
                }
            }
        }
    }
}
