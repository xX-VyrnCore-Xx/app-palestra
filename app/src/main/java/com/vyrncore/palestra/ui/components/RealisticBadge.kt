package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BadgeTier {
    LOCKED, BRONZE, SILVER, GOLD, PLATINUM
}

@Composable
fun RealisticBadge(
    icon: ImageVector,
    tier: BadgeTier,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val (primaryColor, secondaryColor, borderColor) = when (tier) {
        BadgeTier.LOCKED -> Triple(Color(0xFF2C2C2C), Color(0xFF1A1A1A), Color(0xFF3D3D3D))
        BadgeTier.BRONZE -> Triple(Color(0xFFCD7F32), Color(0xFF8B4513), Color(0xFFE6A36E))
        BadgeTier.SILVER -> Triple(Color(0xFFC0C0C0), Color(0xFF707070), Color(0xFFE8E8E8))
        BadgeTier.GOLD -> Triple(Color(0xFFFFD700), Color(0xFFDAA520), Color(0xFFFFF4B0))
        BadgeTier.PLATINUM -> Triple(Color(0xFFE5E4E2), Color(0xFFB4B4B4), Color(0xFFFFFFFF))
    }

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                // Outer glow/shadow
                if (tier != BadgeTier.LOCKED) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f),
                        radius = (size.toPx() / 2) + 4.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Metallic circular base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(borderColor, Color.Transparent, borderColor)
                    ),
                    shape = CircleShape
                )
                .padding(size / 6),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = if (tier == BadgeTier.LOCKED) Color.Gray else Color.White
            )
        }
    }
}
