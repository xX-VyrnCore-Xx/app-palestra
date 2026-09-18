package com.vyrncore.palestra.ui.pt

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.ai.AiAssistantScreen
import com.vyrncore.palestra.ui.chat.ChatListScreen
import com.vyrncore.palestra.ui.components.ConnectionStatusBar
import com.vyrncore.palestra.ui.components.EmptyState
import com.vyrncore.palestra.ui.components.GradientHeader
import com.vyrncore.palestra.ui.profile.ProfileScreen

private data class PtTab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    PtTab("Allievi", Icons.Filled.People),
    PtTab("Chat", Icons.Filled.Forum),
    PtTab("Assistente", Icons.Filled.AutoAwesome),
    PtTab("Profilo", Icons.Filled.Person),
)

@Composable
fun PtDashboardScreen(
    onOpenClient: (clientId: String) -> Unit,
    onOpenChat: (clientId: String) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: PtDashboardViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val unreadCount by viewModel.unreadCount.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            if (tab.label == "Chat" && unreadCount > 0) {
                                BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> PtClientListScreen(onOpenClient = onOpenClient, viewModel = viewModel)
                1 -> ChatListScreen(onOpenChat = onOpenChat)
                2 -> AiAssistantScreen(onBack = {})
                else -> ProfileScreen(onOpenBodyMetrics = {}, onSignedOut = onSignedOut)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PtClientListScreen(
    onOpenClient: (clientId: String) -> Unit,
    viewModel: PtDashboardViewModel,
) {
    val clients by viewModel.clients.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("I tuoi allievi") }) }) { padding ->
      PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.padding(padding),
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ConnectionStatusBar(isOnline = isOnline, isSyncing = isSyncing)
            GradientHeader(
                title = "${clients.size} allievi",
                subtitle = "ID PT: ${viewModel.ptId.take(8)}… — condividilo per collegare nuovi allievi",
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(24.dp),
            )

            if (clients.isEmpty()) {
                EmptyState(icon = Icons.Filled.People, message = "Nessun allievo collegato ancora.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(clients, key = { it.id }) { client ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            onClick = { onOpenClient(client.id) },
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        client.fullName.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Column(modifier = Modifier.padding(start = 16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(client.fullName, style = MaterialTheme.typography.titleMedium)
                                        if (!client.injuries.isNullOrBlank()) {
                                            Icon(
                                                Icons.Filled.HealthAndSafety,
                                                contentDescription = "Infortuni segnalati",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(start = 6.dp).size(18.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        client.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
