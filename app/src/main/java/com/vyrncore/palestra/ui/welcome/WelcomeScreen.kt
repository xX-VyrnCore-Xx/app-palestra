package com.vyrncore.palestra.ui.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.GradientHeader

/**
 * First-login-only questionnaire for the allievo. Everything typed here stays owner-only in
 * Supabase (RLS on allievo_private_profiles, no PT exception) - it is never shown to the PT and
 * never forwarded to the AI assistant, and the copy on screen says so explicitly.
 */
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }
    remember { visible = true }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientHeader(
            title = "Benvenuto reclut@!",
            subtitle = "Prima di iniziare, raccontaci qualcosa di te — resta solo tuo",
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { it / 8 }),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                PrivacyNotice()

                Text(
                    "Queste informazioni ci aiutano a proporti allenamenti su misura. Puoi lasciare vuoto ciò che non vuoi condividere, e modificare tutto in seguito dal tuo profilo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
                )

                OnboardingField(
                    label = "Dolori o lesioni",
                    placeholder = "Es. mal di schiena, ginocchio operato, ecc.",
                    value = uiState.painInjuries,
                    onValueChange = viewModel::updatePainInjuries,
                )
                OnboardingField(
                    label = "Alimentazione",
                    placeholder = "Come mangi di solito? Diete o intolleranze?",
                    value = uiState.nutrition,
                    onValueChange = viewModel::updateNutrition,
                )
                OnboardingField(
                    label = "Stile di vita e lavoro",
                    placeholder = "Che lavoro fai, quanto ti muovi, come dormi?",
                    value = uiState.lifestyle,
                    onValueChange = viewModel::updateLifestyle,
                )
                OnboardingField(
                    label = "Obiettivi",
                    placeholder = "Cosa vuoi ottenere in palestra?",
                    value = uiState.goals,
                    onValueChange = viewModel::updateGoals,
                )

                Button(
                    onClick = { viewModel.save(onFinished) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Arruolati")
                    }
                }
                TextButton(
                    onClick = { viewModel.skip(onFinished) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Salta per ora")
                }
            }
        }
    }
}

@Composable
private fun PrivacyNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(
                "Dati riservati: nessuno può leggerli, nemmeno il tuo PT o l'assistente IA. Solo tu.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun OnboardingField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                Icons.Filled.MilitaryTech,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(end = 6.dp),
            )
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            minLines = 2,
            maxLines = 4,
        )
    }
}
