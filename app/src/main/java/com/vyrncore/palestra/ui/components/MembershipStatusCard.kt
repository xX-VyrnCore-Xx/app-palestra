package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.data.repository.MembershipInfo
import com.vyrncore.palestra.data.repository.MembershipStatus
import com.vyrncore.palestra.ui.theme.Coral50
import com.vyrncore.palestra.ui.theme.Gold40
import com.vyrncore.palestra.ui.theme.Lime50
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

/**
 * The allievo's membership window (set by their PT) with a status color that does the talking:
 * green while there's real runway left, amber inside the last week, red once it's lapsed. Shown
 * to the allievo (read-only) and to the PT on a client's detail screen (with an edit action).
 */
@Composable
fun MembershipStatusCard(
    membership: MembershipInfo?,
    modifier: Modifier = Modifier,
    onManage: (() -> Unit)? = null,
) {
    val (icon, accent, headline, detail) = when {
        membership == null -> Quadruple(
            Icons.Filled.CardMembership,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Nessun abbonamento registrato",
            "Il PT non ha ancora impostato una data di scadenza.",
        )
        membership.status == MembershipStatus.ACTIVE -> Quadruple(
            Icons.Filled.CheckCircle,
            Lime50,
            "Abbonamento attivo",
            "Scade il ${membership.endDate.format(dateFormatter)} · tra ${membership.daysUntilEnd} giorni",
        )
        membership.status == MembershipStatus.EXPIRING_SOON -> Quadruple(
            Icons.Filled.WarningAmber,
            Gold40,
            "In scadenza tra ${membership.daysUntilEnd} giorni",
            "Scade il ${membership.endDate.format(dateFormatter)} - è il momento di rinnovare.",
        )
        else -> Quadruple(
            Icons.Filled.ErrorOutline,
            Coral50,
            "Abbonamento scaduto",
            "Scaduto il ${membership.endDate.format(dateFormatter)}.",
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(headline, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    membership?.planLabel?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (onManage != null) {
                OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(if (membership == null) "Imposta abbonamento" else "Rinnova / modifica")
                }
            }
        }
    }
}

private data class Quadruple(val icon: ImageVector, val accent: Color, val headline: String, val detail: String)
