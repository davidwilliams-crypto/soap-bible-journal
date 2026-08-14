package com.soapjournal.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.data.prefs.STREAK_MILESTONES
import com.soapjournal.app.data.prefs.freezesAvailable
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onBack: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val completedEntries by viewModel.completedEntryCount.collectAsStateWithLifecycle()
    val daysJournaled by viewModel.daysJournaledCount.collectAsStateWithLifecycle()
    val versesMemorized by viewModel.memoryVerseCount.collectAsStateWithLifecycle()
    val heatmapDays by viewModel.heatmapDays.collectAsStateWithLifecycle()
    val surfaces = LocalJournalSurfaces.current
    val freezes = freezesAvailable(prefs)

    Scaffold(
        containerColor = surfaces.paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaces.paper),
                title = { Text("Insights", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                "${prefs.currentStreak}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (prefs.currentStreak == 1) "day rhythm" else "day rhythm, keep going",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile("Longest streak", "${prefs.longestStreak}", Modifier.weight(1f))
                StatTile("Days journaled", "$daysJournaled", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile("Entries written", "$completedEntries", Modifier.weight(1f))
                StatTile("Verses memorized", "$versesMemorized", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                if (freezes > 0) {
                    "Streak freeze available this week — miss a day and your rhythm holds."
                } else {
                    "Streak freeze already used this week."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "JOURNALING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            JournalHeatmap(completedDays = heatmapDays, weeks = INSIGHTS_HEATMAP_WEEKS)
            Spacer(modifier = Modifier.height(8.dp))
            HeatmapLegend()

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "MILESTONES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                STREAK_MILESTONES.forEach { milestone ->
                    MilestoneBadge(
                        days = milestone,
                        achieved = prefs.longestStreak >= milestone,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MilestoneBadge(days: Int, achieved: Boolean, modifier: Modifier = Modifier) {
    val surfaces = LocalJournalSurfaces.current
    val color = if (achieved) MaterialTheme.colorScheme.primary else surfaces.margin
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color.copy(alpha = if (achieved) 1f else 0.5f), RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "$days",
            style = MaterialTheme.typography.titleMedium,
            color = if (achieved) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "days",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun JournalHeatmap(
    completedDays: Set<Long>,
    weeks: Int,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val todayEpoch = today.toEpochDay()
    val totalDays = weeks * 7
    val rawStart = todayEpoch - totalDays + 1
    val startDate = LocalDate.ofEpochDay(rawStart)
    // Align to the Sunday on/before the range start so every column is a full week.
    val daysSinceSunday = startDate.dayOfWeek.value % 7
    val alignedStart = rawStart - daysSinceSunday
    val columns = ((todayEpoch - alignedStart) / 7 + 1).toInt()

    val filledColor = MaterialTheme.colorScheme.primary
    val emptyColor = LocalJournalSurfaces.current.margin.copy(alpha = 0.45f)
    val scrollState = rememberScrollState()

    LaunchedEffect(columns) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (col in 0 until columns) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (row in 0 until 7) {
                    val day = alignedStart + col * 7 + row
                    val color = when {
                        day > todayEpoch -> Color.Transparent
                        completedDays.contains(day) -> filledColor
                        else -> emptyColor
                    }
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend() {
    val surfaces = LocalJournalSurfaces.current
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Less",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(surfaces.margin.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "More",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

