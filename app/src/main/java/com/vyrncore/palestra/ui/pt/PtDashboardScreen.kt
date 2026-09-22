package com.vyrncore.palestra.ui.pt

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.vyrncore.palestra.ui.components.MetricCard
import com.vyrncore.palestra.ui.components.NavBarItem
import com.vyrncore.palestra.ui.components.pressScale
import com.vyrncore.palestra.ui.profile.ProfileScreen
import com.vyrncore.palestra.ui.theme.Bronze40
import com.vyrncore.palestra.ui.theme.Gold40
import com.vyrncore.palestra.ui.theme.Silver40
import com.vyrncore.palestra.data.repository.PlotoneFeedPost
import java.time.Duration
import java.time.Instant

private data class PtTab(val label: String, val icon: ImageVector)

/** Chat sits front and center (index 2), mirroring the allievo-side navbar: Plotone/Schede to its
 * left, Assistente/Profilo to its right. */
private const val CHAT_TAB_INDEX = 2

private val tabs = listOf(
    PtTab("Home", Icons.Filled.People),
    PtTab("Schede", Icons.Filled.FitnessCenter),
    PtTab("Chat", Icons.Filled.Forum),
    PtTab("Assistente", Icons.Filled.AutoAwesome),
    PtTab("Profilo", Icons.Filled.Person),
)

@Composable
fun PtDashboardScreen(
    onOpenClient: (clientId: String) -> Unit,
    onOpenChat: (clientId: String) -> Unit,
    onOpenSearch: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: PtDashboardViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

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
                emphasizedIndex = CHAT_TAB_INDEX,
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.padding(padding),
            transitionSpec = {
                (fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                )) togetherWith fadeOut(tween(120))
            },
            label = "ptTabContent",
        ) { tab ->
            when (tab) {
                0 -> PtClientListScreen(onOpenClient = onOpenClient, onOpenSearch = onOpenSearch, viewModel = viewModel)
                1 -> PtPlansScreen(onOpenClient = onOpenClient, viewModel = viewModel)
                CHAT_TAB_INDEX -> ChatListScreen(onOpenChat = onOpenChat)
                3 -> AiAssistantScreen(onBack = {})
                else -> ProfileScreen(onOpenBodyMetrics = {}, onSignedOut = onSignedOut)
            }
        }
    }
}

@Composable
private fun PtPlansScreen(
    onOpenClient: (clientId: String) -> Unit,
    viewModel: PtDashboardViewModel,
) {
    val overviews by viewModel.clientPlanOverviews.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Schede") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (overviews.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.FitnessCenter,
                message = "Nessun cliente ancora.",
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(overviews, key = { it.clientId }) { overview ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .pressScale(interactionSource),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        interactionSource = interactionSource,
                        onClick = { onOpenClient(overview.clientId) },
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                                Text(overview.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    overview.activeProgramName?.let { "Programma attivo: $it" }
                                        ?: if (overview.planCount > 0) "${overview.planCount} sched${if (overview.planCount == 1) "a" else "e"}" else "Nessuna scheda assegnata",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (overview.planCount == 0 && overview.activeProgramName == null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PtClientListScreen(
    onOpenClient: (clientId: String) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: PtDashboardViewModel,
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val visibleClients by viewModel.visibleClients.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val activeThisWeek by viewModel.activeThisWeekCount.collectAsStateWithLifecycle()
    val inactiveCount by viewModel.inactiveCount.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val weeklyRanking by viewModel.weeklyRanking.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I tuoi clienti") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Cerca")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
      PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.padding(padding),
      ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column {
                    ConnectionStatusBar(isOnline = isOnline, isSyncing = isSyncing)
                    GradientHeader(
                        title = "${clients.size} client${if (clients.size == 1) "e" else "i"}",
                        subtitle = inviteCode?.let { "Codice invito: $it — condividilo per collegare un nuovo cliente" }
                            ?: "Generazione codice invito…",
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                    )

                    val animatedTotal by animateIntAsState(clients.size, label = "clientsTotal")
                    val animatedActive by animateIntAsState(activeThisWeek, label = "clientsActive")
                    val animatedInactive by animateIntAsState(inactiveCount, label = "clientsInactive")
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        MetricCard(
                            icon = Icons.Filled.People,
                            value = "$animatedTotal",
                            label = "TOTALI",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.toggleFilterMode(ClientFilterMode.ALL) },
                        )
                        MetricCard(
                            icon = Icons.Filled.Whatshot,
                            value = "$animatedActive",
                            label = "ATTIVE 7GG",
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            selected = filterMode == ClientFilterMode.ACTIVE_THIS_WEEK,
                            onClick = { viewModel.toggleFilterMode(ClientFilterMode.ACTIVE_THIS_WEEK) },
                        )
                        MetricCard(
                            icon = Icons.Filled.EventBusy,
                            value = "$animatedInactive",
                            label = "FERME",
                            modifier = Modifier.weight(1f),
                            selected = filterMode == ClientFilterMode.INACTIVE,
                            onClick = { viewModel.toggleFilterMode(ClientFilterMode.INACTIVE) },
                        )
                    }

                    if (weeklyRanking.isNotEmpty()) {
                        WeeklyRankingCard(ranking = weeklyRanking, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    }

                    if (feed.isNotEmpty()) {
                        PlotoneFeedCard(posts = feed, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }

                    if (filterMode != ClientFilterMode.ALL) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Text(
                                if (filterMode == ClientFilterMode.INACTIVE) "Mostro solo i clienti fermi" else "Mostro solo gli attivi negli ultimi 7 giorni",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel.toggleFilterMode(ClientFilterMode.ALL) }) {
                                Text("Rimuovi filtro")
                            }
                        }
                    }

                    if (clients.isNotEmpty()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text("Cerca cliente per nome o email") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        )

                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            FilterChip(
                                selected = sortMode == ClientSortMode.LAST_ACTIVE,
                                onClick = { viewModel.setSortMode(ClientSortMode.LAST_ACTIVE) },
                                leadingIcon = { Icon(Icons.Filled.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Più recenti") },
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            FilterChip(
                                selected = sortMode == ClientSortMode.NAME,
                                onClick = { viewModel.setSortMode(ClientSortMode.NAME) },
                                leadingIcon = { Icon(Icons.Filled.SortByAlpha, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Nome") },
                            )
                        }
                    }
                }
            }

            if (clients.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Filled.People, message = "Nessun cliente ancora.")
                }
            } else if (visibleClients.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Filled.Search, message = "Nessun cliente corrisponde alla ricerca.")
                }
            } else {
                items(visibleClients, key = { it.clientId }) { client ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .animateItem()
                            .pressScale(interactionSource),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        interactionSource = interactionSource,
                        onClick = { onOpenClient(client.clientId) },
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            BadgedBox(
                                badge = {
                                    if (client.unreadFromClient > 0) {
                                        Badge { Text("${client.unreadFromClient}") }
                                    }
                                },
                            ) {
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
                            }
                            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
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
                                    lastActiveLabel(client.lastActiveEpochMs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (client.lastActiveEpochMs == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (client.workoutsThisWeek > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        "${client.workoutsThisWeek}× 7gg",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
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

/** Human-readable "when did they last train" label, used both to sort by recency and to flag
 * clients who have gone quiet without the PT having to compute it themself. */
private fun lastActiveLabel(lastActiveEpochMs: Long?): String {
    if (lastActiveEpochMs == null) return "Nessun allenamento ancora"
    val days = Duration.between(Instant.ofEpochMilli(lastActiveEpochMs), Instant.now()).toDays()
    return when {
        days <= 0 -> "Attivo oggi"
        days == 1L -> "Attivo ieri"
        days < 7 -> "Attivo $days giorni fa"
        else -> "Fermo da $days giorni"
    }
}

private fun timeAgo(iso: String?): String {
    val instant = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return ""
    val minutes = Duration.between(instant, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "adesso"
        minutes < 60 -> "${minutes}m fa"
        minutes < 24 * 60 -> "${minutes / 60}h fa"
        else -> "${minutes / (24 * 60)}gg fa"
    }
}

/** Same auto-posted activity feed shown to allievi, from the PT's side: every workout completion
 * across the whole plotone, at a glance. */
@Composable
private fun PlotoneFeedCard(posts: List<PlotoneFeedPost>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DynamicFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Bacheca del team",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            posts.take(6).forEach { post ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(post.displayName, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 4.dp))
                    Text(
                        post.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        timeAgo(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A light motivational nudge for the PT: who's been most active this week, at a glance. */
@Composable
private fun WeeklyRankingCard(ranking: List<ClientRanking>, modifier: Modifier = Modifier) {
    val medalColors = listOf(Gold40, Silver40, Bronze40)
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
                    "Classifica del team",
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
