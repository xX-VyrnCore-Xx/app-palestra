package com.vyrncore.palestra.ui.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.ui.calendar.CalendarScreen
import com.vyrncore.palestra.ui.history.HistoryScreen
import com.vyrncore.palestra.ui.stats.StatsScreen

private val progressTabs = listOf("Cronologia", "Calendario", "Statistiche")

/**
 * Groups the three "what have I done" views (history, calendar, stats) behind one bottom-nav
 * slot with an internal segmented switch, so the main nav doesn't have to carry all three.
 */
@Composable
fun ProgressScreen(onOpenSession: (sessionId: String, planId: String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                progressTabs.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = progressTabs.size),
                    ) {
                        Text(label)
                    }
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HistoryScreen()
                1 -> CalendarScreen(onOpenSession = onOpenSession)
                else -> StatsScreen()
            }
        }
    }
}
