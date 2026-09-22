package com.vyrncore.palestra.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Last step of the password-reset deep link flow: opened only via
 * "vibefitness://reset-password" (see MainActivity/RootViewModel/PalestraNavGraph), never as a
 * normal destination a user could navigate to by hand. Shares the same visual language as
 * Login/Register (animated gradient, glass card, brand badge) so it doesn't feel like a
 * dead-end web page bolted onto the app.
 */
@Composable
fun ResetPasswordScreen(
    accessToken: String,
    onPasswordUpdated: (userId: String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val state by viewModel.setNewPasswordState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state) {
        val done = state as? SetNewPasswordState.Done ?: return@LaunchedEffect
        onPasswordUpdated(done.userId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedAuthBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            BrandBadge(icon = Icons.Filled.LockReset)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Crea una nuova password",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                "Almeno 8 caratteri. Dopo il salvataggio accederai direttamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )
            GlassCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    AuthTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "Nuova password",
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Conferma password",
                        leadingIcon = Icons.Filled.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(force = true) }),
                        isError = state is SetNewPasswordState.Error,
                        errorMessage = (state as? SetNewPasswordState.Error)?.message,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GradientButton(
                        text = "Salva nuova password",
                        isLoading = state is SetNewPasswordState.Saving,
                        onClick = { viewModel.setNewPassword(accessToken, newPassword, confirmPassword) },
                    )
                }
            }
        }
    }
}
