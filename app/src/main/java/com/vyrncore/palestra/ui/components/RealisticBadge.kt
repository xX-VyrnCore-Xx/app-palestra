package com.vyrncore.palestra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BadgeTier {
    LOCKED, BRONZE, SILVER, GOLD, PLATINUM
}

/**
 * A medal-like badge with a metallic gradient base, a glossy specular highlight and a soft outer
 * glow for unlocked tiers, plus a gentle sparkle sweep on GOLD/PLATINUM to read as "special" at a
 * glance rather than just a bigger flat circle.
 */
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
    val isUnlocked = tier != BadgeTier.LOCKED
    val hasSparkle = tier == BadgeTier.GOLD || tier == BadgeTier.PLATINUM

    val infiniteTransition = rememberInfiniteTransition(label = "badgeSparkle")
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "sparkleRotation",
    )

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                if (isUnlocked) {
                    // Soft outer glow ring, tier-tinted.
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = (size.toPx() / 2) + 5.dp.toPx(),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (hasSparkle) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = primaryColor.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(size * 0.32f)
                    .align(Alignment.TopEnd)
                    .rotate(sparkleRotation),
            )
        }

        // Metallic circular base with a bevelled rim.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(colors = listOf(primaryColor, secondaryColor))
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(borderColor, Color.Transparent, borderColor)
                    ),
                    shape = CircleShape
                )
                .drawBehind {
                    // Glossy specular highlight, upper-left, like light hitting a curved medal.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(size.toPx() * 0.32f, size.toPx() * 0.28f),
                            radius = size.toPx() * 0.42f,
                        ),
                    )
                }
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
