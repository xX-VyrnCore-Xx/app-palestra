package com.vyrncore.palestra.ui.chat

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.data.local.entity.ChatAttachmentType
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.ITALY) }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.sendAttachment(it) } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
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
                modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                items(messages, key = { it.id }) { message ->
                    val isMine = message.senderId == viewModel.userId
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Surface(
                            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .align(if (isMine) Alignment.CenterEnd else Alignment.CenterStart)
                                .widthIn(max = 280.dp),
                        ) {
                            Column {
                                MessageContent(message = message, isMine = isMine)
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
}

@Composable
private fun MessageContent(message: ChatMessageEntity, isMine: Boolean) {
    val context = LocalContext.current
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    if (message.attachmentUrl != null) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.attachmentUrl)))
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (message.attachmentType == ChatAttachmentType.IMAGE) Icons.Filled.Image else Icons.Filled.Description,
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
