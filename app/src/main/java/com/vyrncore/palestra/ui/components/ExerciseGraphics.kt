package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwipeUp
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

/** Per-muscle-group visual identity: gradient pair + icon + label. One source of truth so the
 * badge, the hero artwork and any future surface stay consistent. */
data class MuscleGroupStyle(
    val icon: ImageVector,
    val colors: List<Color>,
    val label: String,
)

@Composable
fun muscleGroupStyle(group: String): MuscleGroupStyle {
    val scheme = MaterialTheme.colorScheme
    return when (group.lowercase()) {
        "petto" -> MuscleGroupStyle(
            Icons.Default.FitnessCenter,
            listOf(Color(0xFFD11F80), Color(0xFF7B1FA2)),
            "Petto",
        )
        "dorso" -> MuscleGroupStyle(
            Icons.Default.SwipeUp,
            listOf(Color(0xFF3949AB), Color(0xFF1E88E5)),
            "Dorso",
        )
        "gambe" -> MuscleGroupStyle(
            Icons.Default.DirectionsWalk,
            listOf(Color(0xFFE8672A), Color(0xFFF9A825)),
            "Gambe",
        )
        "spalle" -> MuscleGroupStyle(
            Icons.Default.AccessibilityNew,
            listOf(Color(0xFF00897B), Color(0xFF26A69A)),
            "Spalle",
        )
        "braccia" -> MuscleGroupStyle(
            Icons.Default.Straighten,
            listOf(Color(0xFF6D4C41), Color(0xFF8D6E63)),
            "Braccia",
        )
        "core" -> MuscleGroupStyle(
            Icons.Default.SelfImprovement,
            listOf(Color(0xFFC62828), Color(0xFFEF5350)),
            "Core",
        )
        "cardio" -> MuscleGroupStyle(
            Icons.Default.DirectionsRun,
            listOf(Color(0xFF9CE800), Color(0xFF2E7D32)),
            "Cardio",
        )
        "full body" -> MuscleGroupStyle(
            Icons.Default.Bolt,
            listOf(Color(0xFF5E35B1), Color(0xFFFF5FAE)),
            "Full Body",
        )
        else -> MuscleGroupStyle(
            Icons.Default.FitnessCenter,
            listOf(scheme.primary, scheme.tertiary),
            group,
        )
    }
}

/** A metallic badge representing a muscle group, used as a high-quality placeholder. */
@Composable
fun MuscleGroupBadge(
    group: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val style = muscleGroupStyle(group)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(style.colors))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = group,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

/** Hero artwork for the exercise sheet: full-bleed group gradient, oversized translucent icon
 * and the group label - the "cover art" shown when an exercise has no image of its own. */
@Composable
fun MuscleGroupArtwork(group: String, modifier: Modifier = Modifier) {
    val style = muscleGroupStyle(group)
    Box(
        modifier = modifier
            .background(Brush.linearGradient(style.colors, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset.Infinite)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.fillMaxSize(0.6f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(style.icon, contentDescription = group, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text(
                style.label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** A military-style insignia indicating exercise difficulty. */
@Composable
fun DifficultyRank(
    difficulty: String?,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (difficulty?.uppercase()) {
        ExerciseDifficulty.PRINCIPIANTE.name -> "RECRUIT" to Color(0xFF4CAF50)
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
