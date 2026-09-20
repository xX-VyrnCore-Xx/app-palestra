package com.vyrncore.palestra.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.BackendConfigBanner

/**
 * Login: name, email and password are all it ever asks - no extra steps, no questionnaire.
 * The welcome questionnaire is a Register-only concern (see PalestraNavGraph), and a returning
 * user is straight into their dashboard after this screen.
 */
@Composable
fun LoginScreen(
    onLoggedIn: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.loggedInUserId) {
        uiState.loggedInUserId?.let(onLoggedIn)
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
            Spacer(Modifier.height(48.dp))

            // Hero: brand badge springs in, then the headline fades up.
            var heroVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { heroVisible = true }
            AnimatedVisibility(
                visible = heroVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 3 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandBadge(icon = Icons.Filled.FitnessCenter)
                    Text(
                        "Vibe Fitness",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        "Bentornato. Ogni ripetizione conta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 28.dp),
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
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Accedi",
                            style = MaterialTheme.typography.titleLarge,
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
                            label = "Password",
                            leadingIcon = Icons.Filled.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { viewModel.signIn(email, password) }),
                        )

                        AnimatedVisibility(visible = uiState.errorMessage != null) {
                            Text(
                                uiState.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        GradientButton(
                            text = "Accedi",
                            onClick = { viewModel.signIn(email, password) },
                            isLoading = uiState.isLoading,
                            enabled = email.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            TextButton(onClick = onNavigateToRegister, modifier = Modifier.padding(top = 12.dp)) {
                Text("Non hai un account? Registrati")
            }

            BackendConfigBanner(modifier = Modifier.padding(vertical = 12.dp))

            Spacer(Modifier.height(24.dp))
        }
    }
}
