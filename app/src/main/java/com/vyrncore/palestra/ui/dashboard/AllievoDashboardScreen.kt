package com.vyrncore.palestra.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyrncore.palestra.ui.ai.AiAssistantScreen
import com.vyrncore.palestra.ui.chat.AllievoChatScreen
import com.vyrncore.palestra.ui.components.AnimatedNavBar
import com.vyrncore.palestra.ui.components.NavBarItem
import com.vyrncore.palestra.ui.home.HomeScreen
import com.vyrncore.palestra.ui.profile.ProfileScreen
import com.vyrncore.palestra.ui.workout.WorkoutPlansScreen

private data class AllievoTab(val label: String, val icon: ImageVector)

/** Chat sits front and center (index 2) per the navbar layout: Home/Schede to its left,
 * Assistente/Profilo to its right. Cronologia, Calendario and Statistiche live directly in the
 * Home tab now instead of a dedicated "Progressi" slot. */
private const val CHAT_TAB_INDEX = 2

private val tabs = listOf(
    AllievoTab("Home", Icons.Filled.Home),
    AllievoTab("Schede", Icons.Filled.FitnessCenter),
    AllievoTab("Chat", Icons.Filled.Forum),
    AllievoTab("Assistente", Icons.Filled.AutoAwesome),
    AllievoTab("Profilo", Icons.Filled.Person),
)

@Composable
fun AllievoDashboardScreen(
    onOpenSession: (sessionId: String, planId: String) -> Unit,
    onOpenBodyMetrics: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSearch: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: AllievoDashboardViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val ptId by viewModel.ptId.collectAsState()
    val ptName by viewModel.ptName.collectAsState()
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
            label = "allievoTabContent",
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(
                    onStartSession = onOpenSession,
                    onOpenHistory = onOpenHistory,
                    onOpenCalendar = onOpenCalendar,
                    onOpenSearch = onOpenSearch,
                )
                1 -> WorkoutPlansScreen(onOpenSession = onOpenSession)
                CHAT_TAB_INDEX -> AllievoChatScreen(ptId = ptId, ptName = ptName.orEmpty())
                3 -> AiAssistantScreen(onBack = {})
                else -> ProfileScreen(onOpenBodyMetrics = onOpenBodyMetrics, onSignedOut = onSignedOut)
            }
        }
    }
}
