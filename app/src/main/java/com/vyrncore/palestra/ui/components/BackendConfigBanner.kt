package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.BuildConfig

/**
 * Shown on the auth screens when this build was compiled without SUPABASE_URL/SUPABASE_ANON_KEY
 * (e.g. a CI run before the repository secrets were set) — login/register would otherwise fail
 * with a raw "failed to connect to localhost" error that gives the person no idea what's wrong.
 */
@Composable
fun BackendConfigBanner(modifier: Modifier = Modifier) {
    if (BuildConfig.SUPABASE_URL.isNotBlank()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "Questa build non è collegata al backend (credenziali Supabase mancanti). L'accesso non funzionerà finché non viene ricompilata con i secret impostati.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
