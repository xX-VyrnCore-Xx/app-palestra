package com.vyrncore.palestra.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.ui.components.GradientHeader

@Composable
fun RegisterScreen(
    onRegistered: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ptId by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.ALLIEVO) }

    LaunchedEffect(uiState.loggedInUserId) {
        uiState.loggedInUserId?.let(onRegistered)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        GradientHeader(title = "Crea account", subtitle = "Inizia il tuo percorso in palestra")

        Surface(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Sei un...", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    FilterChip(
                        selected = role == UserRole.ALLIEVO,
                        onClick = { role = UserRole.ALLIEVO },
                        label = { Text("Allievo") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = role == UserRole.PT,
                        onClick = { role = UserRole.PT },
                        label = { Text("Personal Trainer") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nome completo") },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (role == UserRole.ALLIEVO) {
                    OutlinedTextField(
                        value = ptId,
                        onValueChange = { ptId = it },
                        label = { Text("ID del tuo Personal Trainer (opzionale)") },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }

                uiState.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }

                Button(
                    onClick = { viewModel.signUp(email, password, fullName, role, ptId) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !uiState.isLoading,
                ) {
                    Text("Registrati", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
