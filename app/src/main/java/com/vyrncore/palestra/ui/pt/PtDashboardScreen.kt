package com.vyrncore.palestra.ui.pt

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.vyrncore.palestra.ui.components.AnimatedNavBar
import com.vyrncore.palestra.ui.components.ConnectionStatusBar
import com.vyrncore.palestra.ui.components.EmptyState
import com.vyrncore.palestra.ui.components.GradientHeader
import com.vyrncore.palestra.ui.components.NavBarItem
import com.vyrncore.palestra.ui.components.pressScale
import com.vyrncore.palestra.ui.profile.ProfileScreen

private data class PtTab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    PtTab("Plotone", Icons.Filled.People),
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
            AnimatedNavBar(
                items = tabs.map { tab ->
                    NavBarItem(
                        label = tab.label,
                        icon = tab.icon,
                        badgeCount = if (tab.label == "Chat") unreadCount else 0,
                    )
                },
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.padding(padding),
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))) togetherWith
                    fadeOut(tween(140))
            },
            label = "ptTabContent",
        ) { tab ->
            when (tab) {
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
    val weeklyRanking by viewModel.weeklyRanking.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Il tuo plotone") }) }) { padding ->
      PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.padding(padding),
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ConnectionStatusBar(isOnline = isOnline, isSyncing = isSyncing)
            GradientHeader(
                title = "${clients.size} reclute",
                subtitle = "ID PT: ${viewModel.ptId.take(8)}… — condividilo per arruolare nuove reclute",
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(24.dp),
            )

            if (weeklyRanking.isNotEmpty()) {
                WeeklyRankingCard(ranking = weeklyRanking, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            if (clients.isEmpty()) {
                EmptyState(icon = Icons.Filled.People, message = "Nessuna recluta arruolata ancora.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(clients, key = { it.id }) { client ->
                        val interactionSource = remember { MutableInteractionSource() }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .animateItem()
                                .pressScale(interactionSource),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            interactionSource = interactionSource,
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

/** A light motivational nudge for the PT: who's been most active this week, at a glance. */
@Composable
private fun WeeklyRankingCard(ranking: List<ClientRanking>, modifier: Modifier = Modifier) {
    val medalColors = listOf(
        androidx.compose.ui.graphics.Color(0xFFFFC94A),
        androidx.compose.ui.graphics.Color(0xFFC7C7C7),
        androidx.compose.ui.graphics.Color(0xFFCB8B5B),
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MilitaryTech,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    "Classifica del plotone",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            ranking.take(3).forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(medalColors.getOrElse(index) { MaterialTheme.colorScheme.surfaceVariant }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.Black)
                    }
                    Text(
                        entry.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                    )
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        " ${entry.workoutsThisWeek}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}
