package com.vyrncore.palestra.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.util.Date
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onOpenSession: (sessionId: String, planId: String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsState()
    val today = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }

    val sessionsByDate = remember(sessions) {
        sessions.groupBy {
            Instant.ofEpochMilli(it.startedAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    val workoutsThisMonth = remember(sessionsByDate, currentMonth) {
        sessionsByDate.keys.count { YearMonth.from(it) == currentMonth }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Mese precedente")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALY)
                                    .replaceFirstChar { it.uppercase() } + " " + currentMonth.year,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "$workoutsThisMonth allenamenti questo mese",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Mese successivo")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "M", "G", "V", "S", "D").forEach { day ->
                            Text(
                                day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    val firstDay = currentMonth.atDay(1)
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val leadingBlanks = firstDay.dayOfWeek.value - 1
                    val totalCells = leadingBlanks + daysInMonth
                    val rows = (totalCells + 6) / 7

                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val dayNumber = row * 7 + col - leadingBlanks + 1
                                Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (dayNumber in 1..daysInMonth) {
                                        val date = currentMonth.atDay(dayNumber)
                                        val hasSession = sessionsByDate.containsKey(date)
                                        val isSelected = date == selectedDate
                                        val isToday = date == today
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .then(
                                                    if (isToday && !isSelected) {
                                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { selectedDate = date },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                "$dayNumber",
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            )
                                            if (hasSession) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary,
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
                        )
                        Text(
                            " Allenamento svolto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Text(
                            " Oggi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            selectedDate?.let { date ->
                val daySessions = sessionsByDate[date].orEmpty()
                Text(
                    "Allenamenti del ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.ITALY)}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (daySessions.isEmpty()) {
                    Text(
                        "Nessun allenamento in questo giorno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    daySessions.forEach { session ->
                        val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", Locale.ITALY) }
                        val durationMinutes = session.endedAtEpochMs?.let { (it - session.startedAtEpochMs) / 60_000 }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (session.endedAtEpochMs != null) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                            onClick = { session.planId?.let { onOpenSession(session.id, it) } },
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (session.endedAtEpochMs != null) "Allenamento completato" else "Allenamento in corso",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    listOfNotNull(
                                        timeFormat.format(Date(session.startedAtEpochMs)),
                                        durationMinutes?.let { "$it min" },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
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
