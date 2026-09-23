package com.vyrncore.palestra.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.data.repository.AiMessage
import com.vyrncore.palestra.ui.components.EmptyState
import com.vyrncore.palestra.ui.theme.Orange50
import com.vyrncore.palestra.ui.theme.Orange60
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    viewModel: AiAssistantViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AssistantHeader() },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        bottomBar = {
            // Always embedded as a dashboard tab (never a standalone destination), so the
            // system navigation bar inset is already reserved by the outer Scaffold's own
            // bottomBar (the AnimatedNavBar) - adding it again here would double the gap.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Scrivi un messaggio…") },
                    enabled = !uiState.isSending,
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
                val canSend = draft.isNotBlank() && !uiState.isSending
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) {
                                Brush.linearGradient(listOf(Orange60, Orange50))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = {
                            val text = draft
                            draft = ""
                            scope.launch { viewModel.sendMessage(text) }
                        },
                        enabled = canSend,
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Invia",
                                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.messages.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyState(
                            icon = Icons.Filled.AutoAwesome,
                            message = "Chiedi consigli su allenamento, esercizi, progressi, sedi e corsi: l'assistente è privato e vede solo i tuoi dati.",
                        )
                    }
                    SuggestedPrompts(
                        onPick = { text ->
                            draft = ""
                            scope.launch { viewModel.sendMessage(text) }
                        },
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    items(uiState.messages) { message -> AiMessageBubble(message) }
                    if (uiState.isSending) {
                        item { TypingBubble() }
                    }
                }
            }
        }
    }
}

/** Compact branded header: a gradient sparkle chip instead of a plain Material icon, matching
 * the gradient-icon language already used across the app's stat tiles and dashboards. */
@Composable
private fun AssistantHeader() {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Orange60, Orange50))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Assistente AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Il tuo coach personale",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val suggestedPrompts = listOf(
    "Crea una scheda per me",
    "Che corsi ci sono in palestra?",
    "Qual è la sede ViBE più vicina?",
    "Come sto andando questa settimana?",
)

/** Quick-start chips shown only on the empty conversation - the fastest way to discover what the
 * assistant can help with (workout plans, progress, and now locations/courses too) without having
 * to type a first message from scratch. */
@Composable
private fun SuggestedPrompts(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestedPrompts.forEach { prompt ->
            Surface(
                onClick = { onPick(prompt) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            ) {
                Text(
                    prompt,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun AiMessageBubble(message: AiMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) AssistantAvatar()
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .shadow(
                    elevation = if (isUser) 3.dp else 0.dp,
                    shape = bubbleShape(isUser),
                    spotColor = Orange50,
                )
                .background(
                    brush = if (isUser) {
                        Brush.linearGradient(listOf(Orange60, Orange50))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    },
                    shape = bubbleShape(isUser),
                )
                .then(
                    if (!isUser) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), bubbleShape(isUser))
                    } else Modifier
                ),
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Chat-style asymmetric corners: the "tail" corner (bottom-right for the sender, bottom-left for
 * the assistant) is nearly square, the rest stay fully rounded - the same visual grammar as the
 * chat feature elsewhere in the app. */
private fun bubbleShape(isUser: Boolean) = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = if (isUser) 18.dp else 4.dp,
    bottomEnd = if (isUser) 4.dp else 18.dp,
)

@Composable
private fun AssistantAvatar() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Orange60.copy(alpha = 0.9f), Orange50.copy(alpha = 0.9f)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

/** Shown while waiting for a reply - three dots instead of leaving the assistant's last turn
 * dangling with no feedback that it's "thinking". */
@Composable
private fun TypingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        AssistantAvatar()
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, bubbleShape(false))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), bubbleShape(false))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        }
    }
}
