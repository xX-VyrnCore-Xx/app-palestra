package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyrncore.palestra.data.local.ExerciseDifficulty

/** A metallic badge representing a muscle group, used as a high-quality placeholder. */
@Composable
fun MuscleGroupBadge(
    group: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val icon = when (group.lowercase()) {
        "petto" -> Icons.Default.FitnessCenter
        "dorso" -> Icons.Default.LineWeight
        "gambe" -> Icons.Default.DirectionsWalk
        "spalle" -> Icons.Default.AccessibilityNew
        "braccia" -> Icons.Default.Straighten
        "core" -> Icons.Default.SelfImprovement
        "cardio" -> Icons.Default.DirectionsRun
        else -> Icons.Default.DirectionsRun
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = group,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/** A military-style insignia indicating exercise difficulty. */
@Composable
fun DifficultyRank(
    difficulty: String?,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (difficulty?.uppercase()) {
        ExerciseDifficulty.PRINCIPIANTE.name -> "RECUTA" to Color(0xFF4CAF50)
        ExerciseDifficulty.INTERMEDIO.name -> "OPERATIVO" to Color(0xFFFF9800)
        ExerciseDifficulty.AVANZATO.name -> "ELITE" to Color(0xFFF44336)
        else -> "N/D" to Color.Gray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
    }
}
