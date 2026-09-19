package com.vyrncore.palestra.ui.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.GradientHeader

/**
 * First-login-only questionnaire for the allievo, as a short wizard: single-select steps
 * (experience, training days, goal, lifestyle) followed by open-text ones (injuries, diet,
 * notes). Answers are readable by the allievo and their own PT (to tailor a plan) - never by the
 * AI assistant, which the copy on the privacy step says explicitly.
 */
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val step by viewModel.step.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        GradientHeader(
            title = "Benvenuto reclut@!",
            subtitle = "Qualche domanda veloce per costruirti un percorso su misura",
        )

        WizardProgress(
            current = step,
            total = viewModel.stepCount,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(350)) { it } + fadeOut(tween(200)))
                    }
                },
                label = "welcomeWizardStep",
                modifier = Modifier.fillMaxSize(),
            ) { targetStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    when (targetStep) {
                        0 -> SelectStep(
                            question = "Qual è il tuo livello di esperienza?",
                            options = EXPERIENCE_LEVELS,
                            selected = uiState.experienceLevel,
                            onSelect = viewModel::selectExperienceLevel,
                        )
                        1 -> SelectStep(
                            question = "Quanti giorni a settimana puoi allenarti?",
                            options = TRAINING_DAYS_OPTIONS,
                            selected = uiState.trainingDays,
                            onSelect = viewModel::selectTrainingDays,
                        )
                        2 -> SelectStep(
                            question = "Qual è il tuo obiettivo principale?",
                            options = PRIMARY_GOAL_OPTIONS,
                            selected = uiState.primaryGoal,
                            onSelect = viewModel::selectPrimaryGoal,
                        )
                        3 -> SelectStep(
                            question = "Che stile di vita fai di solito?",
                            options = ACTIVITY_LEVEL_OPTIONS,
                            selected = uiState.activityLevel,
                            onSelect = viewModel::selectActivityLevel,
                        )
                        4 -> OpenStep(
                            question = "Hai dolori o lesioni di cui tenere conto?",
                            placeholder = "Es. mal di schiena, ginocchio operato, ecc. Lascia vuoto se nessuno.",
                            value = uiState.painInjuries,
                            onValueChange = viewModel::updatePainInjuries,
                            showPrivacyNotice = true,
                        )
                        5 -> OpenStep(
                            question = "Come ti alimenti di solito?",
                            placeholder = "Diete, intolleranze, abitudini alimentari...",
                            value = uiState.nutrition,
                            onValueChange = viewModel::updateNutrition,
                        )
                        else -> OpenStep(
                            question = "Altro che vuoi dire al tuo PT?",
                            placeholder = "Obiettivi, richieste particolari, note libere...",
                            value = uiState.goals,
                            onValueChange = viewModel::updateGoals,
                        )
                    }
                }
            }
        }

        WizardNavigation(
            step = step,
            lastStep = viewModel.stepCount - 1,
            isSaving = uiState.isSaving,
            onBack = viewModel::previousStep,
            onNext = viewModel::nextStep,
            onFinish = { viewModel.save(onFinished) },
            onSkip = { viewModel.skip(onFinished) },
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )
    }
}

@Composable
private fun WizardProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            val width by animateDpAsState(
                targetValue = if (index == current) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "progressDotWidth",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .background(
                        color = if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

@Composable
private fun SelectStep(
    question: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    AnimatedVisibility(visible = true, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 10 }) {
        Column {
            Icon(
                Icons.Filled.MilitaryTech,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp).padding(top = 8.dp),
            )
            Text(
                question,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        label = {
                            Text(
                                option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenStep(
    question: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    showPrivacyNotice: Boolean = false,
) {
    AnimatedVisibility(visible = true, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 10 }) {
        Column {
            if (showPrivacyNotice) {
                PrivacyNotice(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }
            Text(
                question,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
            )
        }
    }
}

@Composable
private fun PrivacyNotice(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
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
                "Solo tu e il tuo PT potete vedere queste risposte, mai l'assistente IA né altri allievi.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun WizardNavigation(
    step: Int,
    lastStep: Int,
    isSaving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(visible = step > 0) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                }
            }
            Spacer(Modifier.weight(1f))
        }
        Button(
            onClick = if (step == lastStep) onFinish else onNext,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Text(if (step == lastStep) "Arruolati" else "Avanti")
            }
        }
        if (step == lastStep) {
            TextButton(
                onClick = onSkip,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salta per ora")
            }
        }
    }
}
