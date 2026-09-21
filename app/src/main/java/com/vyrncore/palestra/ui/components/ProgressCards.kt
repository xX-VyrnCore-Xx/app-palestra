package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.data.local.dao.PersonalRecord
import com.vyrncore.palestra.data.local.dao.WeeklyVolume

/** Best estimated 1RM (Epley formula) ever logged per exercise, ranked highest first - shared
 * between Statistiche and Home so both surface the same "what am I strongest at" summary. */
@Composable
fun PersonalRecordsCard(records: List<PersonalRecord>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            records.take(8).forEachIndexed { index, record ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(record.exerciseName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${"%.1f".format(record.estimatedOneRepMaxKg)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Volume this week vs the previous one, as a percentage delta - the "am I actually progressing"
 * answer at a glance, instead of having to eyeball a line chart. Shared between Statistiche and
 * Home. */
@Composable
fun WeekOverWeekCard(previousVolumeKg: Double, currentVolumeKg: Double, modifier: Modifier = Modifier) {
    val percentChange = if (previousVolumeKg > 0) {
        ((currentVolumeKg - previousVolumeKg) / previousVolumeKg) * 100
    } else if (currentVolumeKg > 0) {
        100.0
    } else {
        0.0
    }
    val (icon, tint) = when {
        percentChange > 0.5 -> Icons.Filled.TrendingUp to MaterialTheme.colorScheme.tertiary
        percentChange < -0.5 -> Icons.Filled.TrendingDown to MaterialTheme.colorScheme.error
        else -> Icons.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "${if (percentChange >= 0) "+" else ""}${"%.0f".format(percentChange)}% volume vs settimana scorsa",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${"%.0f".format(currentVolumeKg)} kg questa settimana · ${"%.0f".format(previousVolumeKg)} kg la scorsa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A simple linear projection of weekly volume a few weeks out - "if you keep this pace" rather
 * than a real forecast, computed with ordinary least squares over the weeks already logged. Needs
 * at least 3 weeks of data to say anything meaningful; says so honestly otherwise. */
@Composable
fun ProgressTrendCard(weeklyVolume: List<WeeklyVolume>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Proiezione", style = MaterialTheme.typography.titleSmall)
                if (weeklyVolume.size < 3) {
                    Text(
                        "Registra qualche settimana in più per vedere una proiezione del tuo ritmo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    val n = weeklyVolume.size
                    val xs = (0 until n).map { it.toDouble() }
                    val ys = weeklyVolume.map { it.totalVolumeKg }
                    val xMean = xs.average()
                    val yMean = ys.average()
                    val slope = xs.indices.sumOf { (xs[it] - xMean) * (ys[it] - yMean) } /
                        xs.sumOf { (it - xMean) * (it - xMean) }.coerceAtLeast(1e-9)
                    val intercept = yMean - slope * xMean
                    val weeksAhead = 4
                    val projected = (intercept + slope * (n - 1 + weeksAhead)).coerceAtLeast(0.0)
                    val message = when {
                        slope > 1.0 -> "In crescita: a questo ritmo, tra $weeksAhead settimane potresti essere a circa ${projected.toInt()} kg/settimana."
                        slope < -1.0 -> "In calo: a questo ritmo, tra $weeksAhead settimane il volume potrebbe scendere a circa ${projected.toInt()} kg/settimana."
                        else -> "Ritmo stabile intorno a ${ys.last().toInt()} kg/settimana."
                    }
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
