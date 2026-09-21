package com.vyrncore.palestra.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.hilt.navigation.compose.hiltViewModel
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
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
        }
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
