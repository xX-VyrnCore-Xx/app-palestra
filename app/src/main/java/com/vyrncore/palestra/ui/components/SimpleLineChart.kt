package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Minimal line chart used for any single-series trend (exercise progress, body weight, ...).
 * Pass [pointLabels] (same length as [values]) to draw date/category labels under the axis;
 * when there are too many points to fit, a evenly-spaced subset (first and last included)
 * is shown so labels never overlap. */
@Composable
fun SimpleLineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    pointLabels: List<String> = emptyList(),
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        if (values.size < 2) return@Canvas
        val hasLabels = pointLabels.size == values.size
        val labelSpace = if (hasLabels) 22.dp.toPx() else 0f
        val plotHeight = size.height - labelSpace

        val maxY = values.max().coerceAtLeast(1.0)
        val minY = values.min()
        val range = (maxY - minY).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (values.size - 1)

        // Point positions: the line hugs the top of the plot; labels live in the strip below.
        val positions = values.mapIndexed { index, value ->
            Offset(index * stepX, plotHeight - ((value - minY) / range * (plotHeight - 12f)).toFloat())
        }

        val path = Path()
        positions.forEachIndexed { index, p ->
            if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            drawCircle(color = lineColor, radius = 6f, center = p)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 4f))

        if (hasLabels) {
            val indices = labelIndices(values.size)
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            indices.forEach { index ->
                drawContext.canvas.nativeCanvas.drawText(
                    pointLabels[index],
                    positions[index].x,
                    plotHeight + labelSpace * 0.75f,
                    paint,
                )
            }
        }
    }
}

/** At most 4 label slots, always including first and last, evenly spaced between. */
private fun labelIndices(count: Int): List<Int> {
    if (count <= 4) return (0 until count).toList()
    return (0..3).map { i -> (i * (count - 1) / 3).coerceAtMost(count - 1) }.distinct()
}
