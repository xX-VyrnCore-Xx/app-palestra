package com.vyrncore.palestra.ui.timer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RestTimerScreen(
    onClose: () -> Unit,
    viewModel: RestTimerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) vibrate(context)
    }

    val progress by animateFloatAsState(
        targetValue = if (uiState.totalSeconds > 0) uiState.remainingSeconds / uiState.totalSeconds.toFloat() else 0f,
        animationSpec = tween(400),
        label = "restTimerProgress",
    )
    val ringColor by animateColorAsState(
        targetValue = if (uiState.isFinished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "restTimerRingColor",
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Recupero") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = ringColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%d:%02d".format(uiState.remainingSeconds / 60, uiState.remainingSeconds % 60),
                        fontSize = 56.sp,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (uiState.isFinished) {
                        Text(
                            "Tempo scaduto!",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 28.dp)) {
                IconButton(
                    onClick = { viewModel.addSeconds(-15) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Togli 15s")
                }
                Text(
                    "15s",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.CenterVertically),
                )
                IconButton(
                    onClick = { viewModel.addSeconds(15) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Aggiungi 15s")
                }
            }

            Row(modifier = Modifier.padding(top = 20.dp)) {
                Button(
                    onClick = { if (uiState.isRunning) viewModel.pause() else viewModel.resume() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(52.dp),
                ) {
                    Icon(
                        if (uiState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(if (uiState.isRunning) "Pausa" else "Riprendi")
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.padding(start = 12.dp).height(52.dp),
                ) {
                    Text("Chiudi")
                }
            }
        }
    }
}

private fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(400)
    }
}
