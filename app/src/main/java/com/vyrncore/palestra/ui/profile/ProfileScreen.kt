package com.vyrncore.palestra.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vyrncore.palestra.data.local.entity.UserRole
import com.vyrncore.palestra.data.repository.ThemeMode
import com.vyrncore.palestra.ui.theme.Orange50
import com.vyrncore.palestra.ui.theme.Orange50
import com.vyrncore.palestra.ui.theme.Orange60
import com.vyrncore.palestra.ui.theme.OrangeDeep
import com.vyrncore.palestra.ui.welcome.PRIMARY_GOAL_OPTIONS

/**
 * Profile hub: hero header with avatar/name/role, a personalization sheet (bio, height, weight,
 * goal) that mirrors the Welcome answers, then theme, notifications and reminders sections.
 */
@Composable
fun ProfileScreen(
    onOpenBodyMetrics: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val reminderThresholdDays by viewModel.reminderThresholdDays.collectAsStateWithLifecycle()
    val reminderCustomMessage by viewModel.reminderCustomMessage.collectAsStateWithLifecycle()
    val chatNotificationsEnabled by viewModel.chatNotificationsEnabled.collectAsStateWithLifecycle()
    val planNotificationsEnabled by viewModel.planNotificationsEnabled.collectAsStateWithLifecycle()
    val achievementNotificationsEnabled by viewModel.achievementNotificationsEnabled.collectAsStateWithLifecycle()
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val linkPtResult by viewModel.linkPtResult.collectAsStateWithLifecycle()
    var showNameDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showEditProfileSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(profile?.role) {
        if (profile?.role == UserRole.PT) viewModel.loadInviteCode()
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.updateAvatar(it) } }

    Scaffold(topBar = { TopAppBar(title = { Text("Profilo") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Hero header: brand gradient behind avatar + identity, same signature as auth screens.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(Orange60, Orange50, OrangeDeep))),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                            .clickable { avatarPicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (profile?.avatarUrl != null) {
                            AsyncImage(
                                model = profile?.avatarUrl,
                                contentDescription = "Foto profilo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else {
                            Text(
                                profile?.fullName?.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "Cambia foto",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(
                            profile?.fullName.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                        Text(
                            profile?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                        )
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    if (profile?.role == UserRole.PT) "Personal Trainer" else "Allievo",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (profile?.role == UserRole.PT) Icons.Filled.WorkspacePremium else Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                disabledContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                                disabledLabelColor = androidx.compose.ui.graphics.Color.White,
                                disabledLeadingIconContentColor = androidx.compose.ui.graphics.Color.White,
                            ),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    IconButton(onClick = { showEditProfileSheet = true }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Modifica profilo",
                            tint = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                }
            }

            // Bio preview, if set.
            profile?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Text(
                    "\"$bio\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                )
            }

            if (profile?.role == UserRole.ALLIEVO) {
                OutlinedButton(
                    onClick = onOpenBodyMetrics,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Dati corporei e progressi", modifier = Modifier.padding(start = 8.dp))
                }
            }

            if (profile?.role == UserRole.PT) {
                Text(
                    "Il tuo codice invito",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                inviteCode ?: "Generazione…",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "Dallo ai tuoi allievi per collegarli al volo, in registrazione o dal loro profilo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                inviteCode?.let { code ->
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Collegati a me su Vibe Fitness con il codice: $code")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Condividi codice invito"))
                                }
                            },
                            enabled = inviteCode != null,
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Condividi codice")
                        }
                    }
                }
            }

            if (profile?.role == UserRole.ALLIEVO && profile?.ptId == null) {
                Text(
                    "Collega il tuo PT",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var codeDraft by remember { mutableStateOf("") }
                        Text(
                            "Chiedi al tuo PT il suo codice invito e inseriscilo qui.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = codeDraft,
                                onValueChange = { codeDraft = it.uppercase().take(6); viewModel.clearLinkPtResult() },
                                label = { Text("Codice") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = { viewModel.linkToPt(codeDraft) },
                                enabled = codeDraft.isNotBlank(),
                                modifier = Modifier.padding(start = 8.dp),
                            ) { Text("Collega") }
                        }
                        when (val result = linkPtResult) {
                            is LinkPtResult.Success -> Text(
                                "Collegato a ${result.ptName}!",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            LinkPtResult.NotFound -> Text(
                                "Codice non valido, controlla e riprova.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            null -> {}
                        }
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
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SwitchRow("Messaggi chat", chatNotificationsEnabled, viewModel::setChatNotificationsEnabled)
                    SwitchRow("Aggiornamenti scheda", planNotificationsEnabled, viewModel::setPlanNotificationsEnabled)
                    SwitchRow("Record e traguardi", achievementNotificationsEnabled, viewModel::setAchievementNotificationsEnabled)
                }
            }

            if (profile?.role == UserRole.ALLIEVO) {
                Text(
                    "Promemoria allenamento",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SwitchRow("Promemoria attivo", remindersEnabled, viewModel::setRemindersEnabled)

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
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.exportAllData() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text("Esporta tutti i dati")
                }
            }

            TextButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                Text("Esci", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Uscire dall'account?") },
            text = { Text("Dovrai effettuare di nuovo l'accesso per usare l'app.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut(onSignedOut)
                    },
                ) { Text("Esci", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Annulla") }
            },
        )
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

    if (showEditProfileSheet) {
        EditProfileSheet(
            initialBio = profile?.bio.orEmpty(),
            initialHeightCm = profile?.heightCm,
            initialWeightKg = profile?.weightKg,
            initialGoal = profile?.primaryGoal,
            onDismiss = { showEditProfileSheet = false },
            onSave = { bio, heightCm, weightKg, goal ->
                viewModel.updateProfileExtras(bio, heightCm, weightKg, goal)
                showEditProfileSheet = false
            },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Personalization sheet: the same questions as the Welcome wizard plus a free-text bio, so the
 * user can refine their profile at any time without replaying the onboarding flow.
 */
@Composable
private fun EditProfileSheet(
    initialBio: String,
    initialHeightCm: Int?,
    initialWeightKg: Double?,
    initialGoal: String?,
    onDismiss: () -> Unit,
    onSave: (bio: String, heightCm: Int?, weightKg: Double?, goal: String?) -> Unit,
) {
    var bio by rememberSaveable { mutableStateOf(initialBio) }
    var heightText by rememberSaveable { mutableStateOf(initialHeightCm?.toString().orEmpty()) }
    var weightText by rememberSaveable { mutableStateOf(initialWeightKg?.toString().orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(initialGoal) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Personalizza il profilo", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(200) },
                label = { Text("Bio") },
                placeholder = { Text("Due righe su di te: chi sei, cosa cerchi in palestra...") },
                supportingText = { Text("${bio.length}/200") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Il tuo obiettivo",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            FlowRowOfGoals(
                options = PRIMARY_GOAL_OPTIONS,
                selected = goal,
                onSelect = { goal = if (goal == it) null else it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Altezza (cm)") },
                    leadingIcon = { Icon(Icons.Filled.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Peso (kg)") },
                    leadingIcon = { Icon(Icons.Filled.MonitorWeight, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Annulla")
                }
                Button(
                    onClick = {
                        onSave(
                            bio,
                            heightText.toIntOrNull(),
                            weightText.toDoubleOrNull(),
                            goal,
                        )
                    },
                    modifier = Modifier.weight(2f),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Salva", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Simple flow layout for goal chips; avoids a material3 ExperimentalLayout dependency. */
@Composable
private fun FlowRowOfGoals(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    androidx.compose.material3.FilterChip(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        label = { Text(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
            }
        }
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
