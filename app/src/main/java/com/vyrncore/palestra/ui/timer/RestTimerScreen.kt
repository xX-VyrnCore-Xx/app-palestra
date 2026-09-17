package com.vyrncore.palestra.ui.timer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(topBar = { TopAppBar(title = { Text("Recupero") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "%d:%02d".format(uiState.remainingSeconds / 60, uiState.remainingSeconds % 60),
                fontSize = 64.sp,
                style = MaterialTheme.typography.titleLarge,
            )
            if (uiState.isFinished) {
                Text("Tempo scaduto! Torna al lavoro.", modifier = Modifier.padding(top = 8.dp))
            }

            Row(modifier = Modifier.padding(top = 24.dp)) {
                IconButton(onClick = { viewModel.addSeconds(-15) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Togli 15s")
                }
                IconButton(onClick = { viewModel.addSeconds(15) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Aggiungi 15s")
                }
            }

            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = { if (uiState.isRunning) viewModel.pause() else viewModel.resume() }) {
                    Text(if (uiState.isRunning) "Pausa" else "Riprendi")
                }
                Button(onClick = onClose, modifier = Modifier.padding(start = 8.dp)) {
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
