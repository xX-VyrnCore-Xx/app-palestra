package com.vyrncore.palestra.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.ThemeMode

@Composable
fun ProfileScreen(
    onOpenBodyMetrics: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val reminderThresholdDays by viewModel.reminderThresholdDays.collectAsState()
    val reminderCustomMessage by viewModel.reminderCustomMessage.collectAsState()
    val chatNotificationsEnabled by viewModel.chatNotificationsEnabled.collectAsState()
    val planNotificationsEnabled by viewModel.planNotificationsEnabled.collectAsState()
    val achievementNotificationsEnabled by viewModel.achievementNotificationsEnabled.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.updateAvatar(it) } }

    Scaffold(topBar = { TopAppBar(title = { Text("Profilo") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { avatarPicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (profile?.avatarUrl != null) {
                            AsyncImage(
                                model = profile?.avatarUrl,
                                contentDescription = "Foto profilo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else {
                            Text(
                                profile?.fullName?.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "Cambia foto",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile?.fullName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { showNameDialog = true }, modifier = Modifier.size(28.dp).padding(start = 4.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Modifica nome", modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            profile?.email.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (profile?.role == UserRole.PT) "Personal Trainer" else "Allievo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Text(
                "Aspetto",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "Sistema"
                                ThemeMode.LIGHT -> "Chiaro"
                                ThemeMode.DARK -> "Scuro"
                            },
                        )
                    }
                }
            }

            Text(
                "Notifiche",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Messaggi chat", modifier = Modifier.weight(1f))
                Switch(checked = chatNotificationsEnabled, onCheckedChange = viewModel::setChatNotificationsEnabled)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Aggiornamenti scheda", modifier = Modifier.weight(1f))
                Switch(checked = planNotificationsEnabled, onCheckedChange = viewModel::setPlanNotificationsEnabled)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Record e traguardi", modifier = Modifier.weight(1f))
                Switch(checked = achievementNotificationsEnabled, onCheckedChange = viewModel::setAchievementNotificationsEnabled)
            }

            if (profile?.role == UserRole.ALLIEVO) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Promemoria allenamento", modifier = Modifier.weight(1f))
                    Switch(checked = remindersEnabled, onCheckedChange = viewModel::setRemindersEnabled)
                }

                if (remindersEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Avvisami dopo", modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.setReminderThresholdDays((reminderThresholdDays - 1).coerceIn(1, 14)) },
                        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                        Text(
                            "$reminderThresholdDays gg",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        IconButton(
                            onClick = { viewModel.setReminderThresholdDays((reminderThresholdDays + 1).coerceIn(1, 14)) },
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }

                    var messageDraft by remember(reminderCustomMessage) { mutableStateOf(reminderCustomMessage.orEmpty()) }
                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = { messageDraft = it },
                        label = { Text("Messaggio personalizzato (opzionale)") },
                        placeholder = { Text("Es. Dai, oggi tocca a gambe!") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { viewModel.setReminderCustomMessage(messageDraft) },
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }

                OutlinedButton(
                    onClick = onOpenBodyMetrics,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text("Dati corporei")
                }
            }

            TextButton(
                onClick = { viewModel.signOut(onSignedOut) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Esci", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showNameDialog) {
        EditNameDialog(
            currentName = profile?.fullName.orEmpty(),
            onDismiss = { showNameDialog = false },
            onConfirm = { newName ->
                viewModel.updateFullName(newName)
                showNameDialog = false
            },
        )
    }
}

@Composable
private fun EditNameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica nome") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
