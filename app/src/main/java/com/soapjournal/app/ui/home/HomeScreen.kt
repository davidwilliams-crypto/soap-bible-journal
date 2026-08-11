package com.soapjournal.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.data.plan.ReadingPlan
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenEntry: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBible: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val today = LocalDate.now(ZoneId.systemDefault())
    val todayEntry = entries.firstOrNull { it.entryDateEpochDay == today.toEpochDay() }
    val votd = viewModel.verseOfTheDay
    val reading = viewModel.todayReading(prefs)
    val planProgress = ReadingPlan.progressFraction(LocalDate.ofEpochDay(prefs.planStartEpochDay))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SOAP Journal", style = MaterialTheme.typography.displaySmall)
                    Text(
                        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text("${prefs.currentStreak}", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Verse of the Day", style = MaterialTheme.typography.titleMedium)
                Text(votd.verse.reference, style = MaterialTheme.typography.labelLarge)
                Text(votd.verse.text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${prefs.bibleVersion.displayName} preferred · VOTD shown in KJV offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = viewModel::addVotdToMemory) {
                    Text("Add to memorization")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onOpenPlan)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Today's reading plan", style = MaterialTheme.typography.titleMedium)
                Text(reading.label, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Day ${reading.dayIndex + 1} of ${ReadingPlan.TOTAL_DAYS} · whole Bible in 2 years",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { planProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { viewModel.openTodayFromPlan(onOpenEntry) }) {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Journal this passage (SOAP)",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onOpenBible, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Bible", modifier = Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(onClick = onOpenMemory, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Outlined.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Memorize", modifier = Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Entries", modifier = Modifier.padding(start = 6.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (todayEntry != null) "Continue today's SOAP" else "Start a fresh SOAP entry",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (todayEntry != null) onOpenEntry(todayEntry.id)
                            else viewModel.openToday(onOpenEntry)
                        }
                    ) {
                        Text(if (todayEntry != null) "Continue" else "New entry")
                    }
                    OutlinedButton(onClick = { viewModel.createNew(onOpenEntry) }) {
                        Text("Another entry")
                    }
                }
                if (todayEntry != null) {
                    val follow = buildString {
                        if (todayEntry.applicationFollowThrough) append("Application ✓  ")
                        else append("Application pending  ")
                        if (todayEntry.prayerFollowThrough) append("Prayer ✓")
                        else append("Prayer pending")
                    }
                    Text(follow, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.AutoStories, contentDescription = null)
                Text(
                    "Longest streak: ${prefs.longestStreak} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
