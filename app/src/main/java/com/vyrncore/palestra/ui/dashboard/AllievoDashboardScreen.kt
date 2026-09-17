package com.vyrncore.palestra.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.calendar.CalendarScreen
import com.vyrncore.palestra.ui.chat.ChatThreadScreen
import com.vyrncore.palestra.ui.components.EmptyState
import com.vyrncore.palestra.ui.history.HistoryScreen
import com.vyrncore.palestra.ui.home.HomeScreen
import com.vyrncore.palestra.ui.profile.ProfileScreen
import com.vyrncore.palestra.ui.stats.StatsScreen
import com.vyrncore.palestra.ui.workout.WorkoutPlansScreen

private data class AllievoTab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    AllievoTab("Home", Icons.Filled.Home),
    AllievoTab("Schede", Icons.Filled.FitnessCenter),
    AllievoTab("Cronologia", Icons.Filled.History),
    AllievoTab("Calendario", Icons.Filled.CalendarMonth),
    AllievoTab("Chat", Icons.Filled.Forum),
    AllievoTab("Statistiche", Icons.Filled.ShowChart),
    AllievoTab("Profilo", Icons.Filled.Person),
)

@Composable
fun AllievoDashboardScreen(
    onOpenSession: (sessionId: String, planId: String) -> Unit,
    onOpenBodyMetrics: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: AllievoDashboardViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val ptId by viewModel.ptId.collectAsState()
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
                0 -> HomeScreen(onStartSession = onOpenSession)
                1 -> WorkoutPlansScreen(onOpenSession = onOpenSession)
                2 -> HistoryScreen()
                3 -> CalendarScreen(onOpenSession = onOpenSession)
                4 -> {
                    val peer = ptId
                    if (peer != null) {
                        ChatThreadScreen(peerId = peer)
                    } else {
                        EmptyState(
                            icon = Icons.Filled.Forum,
                            message = "Nessun Personal Trainer collegato ancora.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                5 -> StatsScreen()
                else -> ProfileScreen(onOpenBodyMetrics = onOpenBodyMetrics, onSignedOut = onSignedOut)
            }
        }
    }
}
