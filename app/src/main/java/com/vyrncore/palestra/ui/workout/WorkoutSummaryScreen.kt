package com.vyrncore.palestra.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyrncore.palestra.ui.components.BarChartEntry
import com.vyrncore.palestra.ui.components.SimpleBarChart
import com.vyrncore.palestra.ui.theme.Magenta60
import com.vyrncore.palestra.ui.theme.Orange50
import com.vyrncore.palestra.ui.theme.Violet40
import com.vyrncore.palestra.util.ImageShareUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutSummaryScreen(
    onDone: () -> Unit,
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val graphicsLayer = rememberGraphicsLayer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(56.dp).padding(top = 16.dp),
            )
            Text(
                "Missione completata!",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    },
            ) {
                ShareableWorkoutCard(uiState = uiState)
            }

            // Volume per exercise: the session's work laid out as bars, biggest first.
            if (uiState.volumeByExercise.isNotEmpty()) {
                SectionCard(title = "VOLUME PER ESERCIZIO") {
                    SimpleBarChart(
                        entries = uiState.volumeByExercise.map {
                            BarChartEntry(label = it.exerciseName, value = it.volumeKg)
                        },
                    )
                    Text(
                        "kg sollevati (peso × ripetizioni)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            // PR comparison: this session's best estimated 1RM vs the all-time best before it.
            if (uiState.prComparison.isNotEmpty()) {
                SectionCard(title = "CONFRONTO RECORD PERSONALI") {
                    uiState.prComparison.forEachIndexed { index, entry ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                            )
                        }
                        PrComparisonRow(entry = entry)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        ImageShareUtil.shareBitmap(context, bitmap, "Condividi il tuo allenamento")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text("Condividi", modifier = Modifier.padding(start = 8.dp))
            }

            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 12.dp),
            ) {
                Text("Fine")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Glass container used by the two analytics sections under the shareable card. */
@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

/** One exercise's session-best e1RM against its previous all-time best, with a signed delta. */
@Composable
private fun PrComparisonRow(entry: SummaryPrEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            val contextLine = when {
                entry.previousBestKg == null -> "Prima volta registrato"
                entry.deltaKg!! > 0.05 -> "+${"%.1f".format(entry.deltaKg)} kg vs il tuo record"
                entry.deltaKg < -0.05 -> "${"%.1f".format(entry.deltaKg)} kg dal record"
                else -> "Record eguagliato"
            }
            val contextColor = when {
                entry.previousBestKg == null -> MaterialTheme.colorScheme.onSurfaceVariant
                entry.deltaKg!! > 0.05 -> MaterialTheme.colorScheme.tertiary
                entry.deltaKg < -0.05 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                contextLine,
                style = MaterialTheme.typography.labelSmall,
                color = contextColor,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (entry.previousBestKg == null || entry.deltaKg!! > 0.05) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = "Nuovo record",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(18.dp).padding(end = 4.dp),
            )
        }
        Text(
            "≈ ${"%.1f".format(entry.sessionE1rmKg)} kg",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ShareableWorkoutCard(uiState: WorkoutSummaryUiState) {
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.ITALY) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Orange50, Magenta60, Violet40)))
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Color.White)
            Text(
                "VIBE FITNESS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            uiState.planName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (uiState.fullName.isNotBlank()) {
            Text(
                uiState.fullName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Text(
            dateFormat.format(Date(uiState.dateEpochMs)),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryStat(value = "${uiState.setCount}", label = "SERIE")
            SummaryStat(value = "${uiState.totalVolumeKg.toInt()}", label = "KG SOLLEVATI")
            if (uiState.durationMinutes != null) {
                SummaryStat(value = "${uiState.durationMinutes}", label = "MINUTI")
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}
