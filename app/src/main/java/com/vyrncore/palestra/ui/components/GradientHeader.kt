package com.vyrncore.palestra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.ui.theme.Magenta60
import com.vyrncore.palestra.ui.theme.Orange50
import com.vyrncore.palestra.ui.theme.Violet10

/** Signature gradient block used at the top of hero screens (auth, dashboards). The gradient
 * slowly drifts sideways via an infinite transition: a cheap GPU-only animation (brush offset)
 * that makes the brand header feel alive without redrawing any child content. */
@Composable
fun GradientHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
) {
    val transition = rememberInfiniteTransition(label = "gradientDrift")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradientDriftOffset",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = driftingGradient(drift), shape = shape)
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** The brand gradient, sampled with a moving center so the colors visibly shift. */
private fun driftingGradient(drift: Float): Brush {
    val start = Offset(x = -200f + drift * 400f, y = 0f)
    val end = Offset(x = 800f + drift * 400f, y = 1000f)
    return Brush.linearGradient(listOf(Orange50, Magenta60, Violet10), start = start, end = end)
}
