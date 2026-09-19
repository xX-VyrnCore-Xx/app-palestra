package com.vyrncore.palestra.ui.chat

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vyrncore.palestra.data.local.entity.ChatAttachmentType
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatThreadScreen(
    peerId: String,
    onBack: (() -> Unit)? = null,
    viewModel: ChatThreadViewModel = hiltViewModel(),
) {
    LaunchedEffect(peerId) { viewModel.setPeer(peerId) }

    val messages by viewModel.messages.collectAsState()
    val peerName by viewModel.peerName.collectAsState()
    var draft by remember { mutableStateOf("") }
    var messageToDelete by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.ITALY) }
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.ITALY) }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.sendAttachment(it) } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(peerName) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { attachmentPicker.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Allega file")
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Scrivi un messaggio…") },
                )
                IconButton(
                    onClick = {
                        viewModel.sendMessage(draft.trim())
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Invia")
                }
            }
        },
    ) { padding ->
        if (messages.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Forum,
                message = "Nessun messaggio. Scrivi il primo!",
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                    val isMine = message.senderId == viewModel.userId
                    val previousDay = messages.getOrNull(index - 1)?.let { it.createdAtEpochMs / 86_400_000L }
                    val thisDay = message.createdAtEpochMs / 86_400_000L
                    if (previousDay != thisDay) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    dateFormat.format(Date(message.createdAtEpochMs)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .animateItem(placementSpec = tween(220)),
                    ) {
                        Surface(
                            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .align(if (isMine) Alignment.CenterEnd else Alignment.CenterStart)
                                .widthIn(max = 280.dp)
                                .animateContentSize(animationSpec = tween(180))
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { if (isMine && !message.isDeleted) messageToDelete = message.id },
                                ),
                        ) {
                            Column {
                                if (message.isDeleted) {
                                    Text(
                                        "Messaggio eliminato",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = (if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    )
                                } else {
                                    MessageContent(message = message, isMine = isMine)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                                ) {
                                    Text(
                                        timeFormat.format(Date(message.createdAtEpochMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = (if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                                    )
                                    if (isMine) {
                                        Icon(
                                            if (message.readAtEpochMs != null) Icons.Filled.DoneAll else Icons.Filled.Done,
                                            contentDescription = if (message.readAtEpochMs != null) "Letto" else "Inviato",
                                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(start = 4.dp).size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    messageToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Eliminare il messaggio?") },
            text = { Text("Il messaggio verrà rimosso per te e per l'altra persona.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMessage(id); messageToDelete = null }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun MessageContent(message: ChatMessageEntity, isMine: Boolean) {
    val context = LocalContext.current
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    if (message.attachmentUrl != null) {
        if (message.attachmentType == ChatAttachmentType.IMAGE) {
            AsyncImage(
                model = message.attachmentUrl,
                contentDescription = message.attachmentName ?: "Immagine",
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .widthIn(max = 260.dp)
                    .height(180.dp)
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.attachmentUrl)))
                    },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            return
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.attachmentUrl)))
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                tint = textColor,
            )
            Text(
                message.attachmentName ?: message.content,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        return
    }

    val urlMatcher = remember { Patterns.WEB_URL }
    val matcher = remember(message.content) { urlMatcher.matcher(message.content) }
    val hasLink = remember(message.content) { matcher.find() }

    if (!hasLink) {
        Text(
            message.content,
            color = textColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
        return
    }

    matcher.reset()
    val annotated = remember(message.content) { linkify(message.content, matcher, textColor) }
    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
            annotated,
            modifier = Modifier.clickable {
                val freshMatcher = Patterns.WEB_URL.matcher(message.content)
                if (freshMatcher.find()) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(freshMatcher.group())))
                }
            },
        )
    }
}

/** Renders [content] with any URL matched by [matcher] underlined, so a link visually stands out. */
private fun linkify(
    content: String,
    matcher: java.util.regex.Matcher,
    textColor: androidx.compose.ui.graphics.Color,
): AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    var lastEnd = 0
    while (matcher.find()) {
        withStyle(SpanStyle(color = textColor)) { append(content.substring(lastEnd, matcher.start())) }
        withStyle(SpanStyle(color = textColor, textDecoration = TextDecoration.Underline)) {
            append(content.substring(matcher.start(), matcher.end()))
        }
        lastEnd = matcher.end()
    }
    withStyle(SpanStyle(color = textColor)) { append(content.substring(lastEnd)) }
}
