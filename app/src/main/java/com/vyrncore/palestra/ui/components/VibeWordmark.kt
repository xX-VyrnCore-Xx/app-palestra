package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.R

/**
 * The ViBE wordmark from the launcher icon, on a tight viewport so it can be scaled up for
 * in-app hero moments (auth screens, about). Tint it per-surface: white on brand gradients,
 * primary on light surfaces. The 52:23 aspect ratio matches the vector's viewport.
 */
@Composable
fun VibeWordmark(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    width: Dp = 150.dp,
) {
    Icon(
        painter = painterResource(R.drawable.vibe_wordmark),
        contentDescription = "ViBE",
        tint = tint,
        modifier = modifier
            .width(width)
            .aspectRatio(52f / 23f),
    )
}
