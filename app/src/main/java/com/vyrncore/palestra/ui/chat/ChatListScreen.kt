package com.vyrncore.palestra.ui.chat

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.components.EmptyState
import com.vyrncore.palestra.ui.theme.Lime50
import com.vyrncore.palestra.ui.theme.Magenta60
import com.vyrncore.palestra.ui.theme.Orange50
import com.vyrncore.palestra.ui.theme.Violet40

private val avatarGradients = listOf(
    listOf(Magenta60, Violet40),
    listOf(Orange50, Magenta60),
    listOf(Violet40, Lime50),
    listOf(Lime50, Orange50),
)

private fun gradientFor(seed: String): Brush {
    val colors = avatarGradients[(seed.hashCode().mod(avatarGradients.size))]
    return Brush.linearGradient(colors)
}

@Composable
fun ChatListScreen(
    onOpenChat: (peerId: String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val conversations by viewModel.conversations.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Messaggi") }) }) { padding ->
        if (conversations.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Forum,
                message = "Nessun allievo collegato ancora.",
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(conversations, key = { it.peerId }) { conversation ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .animateItem(placementSpec = tween(220)),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (conversation.unreadCount > 0) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (conversation.unreadCount > 0) 3.dp else 1.dp),
                        onClick = { onOpenChat(conversation.peerId) },
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(gradientFor(conversation.peerId)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    conversation.peerName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(
                                    conversation.peerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                )
                                Text(
                                    conversation.lastMessage ?: "Nessun messaggio ancora",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (conversation.unreadCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("${conversation.unreadCount}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
