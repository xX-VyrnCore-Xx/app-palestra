package com.vyrncore.palestra.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.ui.components.VibeWordmark

/**
 * Shown when a Personal Trainer account signs in: the PT side now lives entirely in a separate
 * web management app, connected to the same Supabase backend, so this Android app is for allievi
 * only. Deliberately does NOT show or link that app's address anywhere in this screen (or
 * anywhere else in the app/APK) - it stays out of band, known only to the PTs who already have
 * it, rather than discoverable by decompiling this build. Just explains the situation and signs
 * the PT out.
 */
@Composable
fun PtWebAppScreen(onSignOut: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedAuthBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            VibeWordmark(width = 150.dp)
            Spacer(Modifier.height(28.dp))
            GlassCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    BrandBadge(icon = Icons.Filled.Computer)
                    Text(
                        "Area Personal Trainer",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Clienti, schede, programmi e abbonamenti si gestiscono ora dal gestionale web " +
                            "dedicato ai PT. L'app è riservata agli allievi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                        Text("Esci e accedi con un altro account")
                    }
                }
            }
        }
    }
}
