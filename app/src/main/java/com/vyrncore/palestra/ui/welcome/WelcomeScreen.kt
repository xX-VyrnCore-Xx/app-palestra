package com.vyrncore.palestra.ui.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.auth.GradientButton
import com.vyrncore.palestra.ui.components.GradientHeader
import com.vyrncore.palestra.ui.components.pressScale

/**
 * First-login-only questionnaire for the allievo, as a short wizard: single-select steps
 * (experience, training days, goal, lifestyle) followed by open-text ones (injuries, diet,
 * notes) and a final recap. Answers are readable by the allievo and their own PT (to tailor a
 * plan) - never by the AI assistant, which the copy on the privacy step says explicitly.
 *
 * Selection steps render as tappable cards with a leading icon and a spring check-badge, in a
 * Technogym-like onboarding feel; the recap step mirrors the chosen answers back before saving.
 */
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        GradientHeader(
            title = "Benvenuto!",
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
                            stepIcon = Icons.Filled.MilitaryTech,
                            question = "Qual è il tuo livello di esperienza?",
                            options = EXPERIENCE_LEVELS,
                            optionIcons = listOf(Icons.Filled.Spa, Icons.Filled.FitnessCenter, Icons.Filled.Bolt),
                            selected = uiState.experienceLevel,
                            onSelect = viewModel::selectExperienceLevel,
                        )
                        1 -> SelectStep(
                            stepIcon = Icons.Filled.CalendarMonth,
                            question = "Quanti giorni a settimana puoi allenarti?",
                            options = TRAINING_DAYS_OPTIONS,
                            optionIcons = listOf(Icons.Filled.EventNote, Icons.Filled.CalendarMonth, Icons.Filled.DirectionsRun),
                            selected = uiState.trainingDays,
                            onSelect = viewModel::selectTrainingDays,
                        )
                        2 -> SelectStep(
                            stepIcon = Icons.Filled.Flag,
                            question = "Qual è il tuo obiettivo principale?",
                            options = PRIMARY_GOAL_OPTIONS,
                            optionIcons = listOf(
                                Icons.Filled.MonitorWeight,
                                Icons.Filled.FitnessCenter,
                                Icons.Filled.DirectionsRun,
                                Icons.Filled.Spa,
                                Icons.Filled.Favorite,
                            ),
                            selected = uiState.primaryGoal,
                            onSelect = viewModel::selectPrimaryGoal,
                        )
                        3 -> SelectStep(
                            stepIcon = Icons.Filled.TrackChanges,
                            question = "Che stile di vita fai di solito?",
                            options = ACTIVITY_LEVEL_OPTIONS,
                            optionIcons = listOf(Icons.Filled.Spa, Icons.Filled.DirectionsRun, Icons.Filled.Bolt),
                            selected = uiState.activityLevel,
                            onSelect = viewModel::selectActivityLevel,
                        )
                        4 -> OpenStep(
                            stepIcon = Icons.Filled.Healing,
                            question = "Hai dolori o lesioni di cui tenere conto?",
                            placeholder = "Es. mal di schiena, ginocchio operato, ecc. Lascia vuoto se nessuno.",
                            value = uiState.painInjuries,
                            onValueChange = viewModel::updatePainInjuries,
                            showPrivacyNotice = true,
                        )
                        5 -> OpenStep(
                            stepIcon = Icons.Filled.Egg,
                            question = "Come ti alimenti di solito?",
                            placeholder = "Diete, intolleranze, abitudini alimentari...",
                            value = uiState.nutrition,
                            onValueChange = viewModel::updateNutrition,
                        )
                        6 -> OpenStep(
                            stepIcon = Icons.Filled.Flag,
                            question = "Altro che vuoi dire al tuo PT?",
                            placeholder = "Obiettivi, richieste particolari, note libere...",
                            value = uiState.goals,
                            onValueChange = viewModel::updateGoals,
                        )
                        else -> RecapStep(uiState = uiState)
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

/** One tappable answer card: leading icon, label and an animated check badge when selected. */
@Composable
private fun OptionCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "optionCardBorder",
    )
    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "optionCardContainer",
    )
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                        } else {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
                exit = fadeOut(tween(120)),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selezionato",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun SelectStep(
    stepIcon: ImageVector,
    question: String,
    options: List<String>,
    optionIcons: List<ImageVector>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Column {
        Icon(
            stepIcon,
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
            options.forEachIndexed { index, option ->
                OptionCard(
                    label = option,
                    icon = optionIcons.getOrElse(index) { Icons.Filled.FitnessCenter },
                    selected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun OpenStep(
    stepIcon: ImageVector,
    question: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    showPrivacyNotice: Boolean = false,
) {
    Column {
        if (showPrivacyNotice) {
            PrivacyNotice(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }
        Icon(
            stepIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Text(
            question,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
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

/** Final mirror step: echoes the picked answers back before saving. */
@Composable
private fun RecapStep(uiState: WelcomeUiState) {
    Column {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(32.dp),
        )
        Text(
            "Tutto pronto?",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
        )
        listOf(
            "Esperienza" to uiState.experienceLevel,
            "Allenamento" to uiState.trainingDays,
            "Obiettivo" to uiState.primaryGoal,
            "Stile di vita" to uiState.activityLevel,
        ).forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    value ?: "Non impostato",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                )
            }
        }
        Text(
            "Potrai modificare tutto dal tuo profilo in ogni momento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
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
        GradientButton(
            text = if (step == lastStep) "Inizia" else "Avanti",
            onClick = if (step == lastStep) onFinish else onNext,
            enabled = !isSaving,
            isLoading = isSaving,
            modifier = Modifier.padding(top = 8.dp),
        )
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
