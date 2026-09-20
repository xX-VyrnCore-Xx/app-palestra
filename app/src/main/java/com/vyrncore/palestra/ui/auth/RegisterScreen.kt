package com.vyrncore.palestra.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.ui.components.BackendConfigBanner

/**
 * Registration collects the essentials (role, name, email, password) plus optional body metrics,
 * then hands off to the Welcome wizard: [AuthUiState.justRegistered] tells the nav graph to
 * route a fresh ALLIEVO through onboarding before the dashboard, while a PT lands straight on
 * their dashboard.
 */
@Composable
fun RegisterScreen(
    onRegistered: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var ptId by rememberSaveable { mutableStateOf("") }
    var heightText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(UserRole.ALLIEVO) }
    val focusManager = LocalFocusManager.current

    val submit: () -> Unit = {
        viewModel.signUp(
            email = email,
            password = password,
            fullName = fullName,
            role = role,
            ptId = ptId,
            heightCm = heightText.toIntOrNull(),
            weightKg = weightText.toDoubleOrNull(),
            primaryGoal = null,
        )
    }

    LaunchedEffect(uiState.loggedInUserId) {
        uiState.loggedInUserId?.let(onRegistered)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedAuthBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            var heroVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { heroVisible = true }
            AnimatedVisibility(
                visible = heroVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 3 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandBadge(icon = Icons.Filled.FitnessCenter)
                    Text(
                        "Unisciti al Vibe",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        "Crea il tuo account e inizia il percorso",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                    )
                }
            }

            var formVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { formVisible = true }
            AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn(tween(450, delayMillis = 120)) + slideInVertically(tween(450, delayMillis = 120)) { it / 5 },
            ) {
                GlassCard {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Sei un...", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoleChip(
                                selected = role == UserRole.ALLIEVO,
                                label = "Allievo",
                                icon = Icons.Filled.Person,
                                onClick = { role = UserRole.ALLIEVO },
                                modifier = Modifier.weight(1f),
                            )
                            RoleChip(
                                selected = role == UserRole.PT,
                                label = "Personal Trainer",
                                icon = Icons.Filled.WorkspacePremium,
                                onClick = { role = UserRole.PT },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        AuthTextField(
                            value = fullName,
                            onValueChange = { fullName = it; viewModel.clearErrors() },
                            label = "Nome completo",
                            leadingIcon = Icons.Filled.Person,
                            isError = uiState.nameError != null,
                            errorMessage = uiState.nameError,
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        )
                        AuthTextField(
                            value = email,
                            onValueChange = { email = it; viewModel.clearErrors() },
                            label = "Email",
                            leadingIcon = Icons.Filled.Email,
                            isError = uiState.emailError != null,
                            errorMessage = uiState.emailError,
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        )
                        AuthTextField(
                            value = password,
                            onValueChange = { password = it; viewModel.clearErrors() },
                            label = "Password (min. 8 caratteri)",
                            leadingIcon = Icons.Filled.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            trailingContent = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Nascondi password" else "Mostra password",
                                    )
                                }
                            },
                            isError = uiState.passwordError != null,
                            errorMessage = uiState.passwordError,
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        )

                        AnimatedVisibility(
                            visible = role == UserRole.ALLIEVO,
                            enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                            exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                        ) {
                            Column {
                                AuthTextField(
                                    value = ptId,
                                    onValueChange = { ptId = it },
                                    label = "ID del tuo Personal Trainer (opzionale)",
                                    leadingIcon = Icons.Filled.Badge,
                                )
                                Text(
                                    "Inserisci i tuoi dati corporali per un percorso più preciso (opzionale)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AuthTextField(
                                        value = heightText,
                                        onValueChange = { heightText = it.filter(Char::isDigit).take(3) },
                                        label = "Altezza (cm)",
                                        leadingIcon = Icons.Filled.Straighten,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                        modifier = Modifier.weight(1f),
                                    )
                                    AuthTextField(
                                        value = weightText,
                                        onValueChange = { weightText = it.filter(Char::isDigit).take(3) },
                                        label = "Peso (kg)",
                                        leadingIcon = Icons.Filled.MonitorWeight,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                                        keyboardActions = KeyboardActions(onDone = { submit() }),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = uiState.errorMessage != null) {
                            Text(
                                uiState.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        GradientButton(
                            text = "Registrati",
                            onClick = { submit() },
                            isLoading = uiState.isLoading,
                            enabled = email.isNotBlank() && password.isNotBlank() && fullName.isNotBlank(),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            TextButton(onClick = onNavigateToLogin, modifier = Modifier.padding(top = 12.dp)) {
                Text("Hai già un account? Accedi")
            }

            BackendConfigBanner(modifier = Modifier.padding(vertical = 12.dp))

            Spacer(Modifier.height(24.dp))
        }
    }
}
