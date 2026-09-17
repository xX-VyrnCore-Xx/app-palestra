package com.vyrncore.palestra.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Composable
fun ProgressLineChart(points: List<ExerciseProgressPoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        if (points.size < 2) return@Canvas
        val maxY = points.maxOf { it.maxWeightKg }.coerceAtLeast(1.0)
        val minY = points.minOf { it.maxWeightKg }
        val range = (maxY - minY).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (points.size - 1)

        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = size.height - ((point.maxWeightKg - minY) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
        }
        drawPath(path = path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    }
}
