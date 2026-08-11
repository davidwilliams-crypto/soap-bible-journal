package com.soapjournal.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.plan.ReadingPlan
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingPlanScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    val prefs by container.prefs.preferences.collectAsStateWithLifecycle(
        initialValue = com.soapjournal.app.data.prefs.UserPreferences()
    )
    val scope = rememberCoroutineScope()
    val start = LocalDate.ofEpochDay(prefs.planStartEpochDay)
    val today = ReadingPlan.dayFor(start)
    val upcoming = ((today.dayIndex)..(today.dayIndex + 13).coerceAtMost(ReadingPlan.TOTAL_DAYS - 1))
        .map { ReadingPlan.day(it) }
    var passageText by remember(today.dayIndex, prefs.bibleVersion) { mutableStateOf("") }

    LaunchedEffect(today.dayIndex, prefs.bibleVersion, prefs.planStartEpochDay) {
        passageText = container.bible.passageText(today.passages, prefs.bibleVersion)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Plan") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Whole Bible in 2 years", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Day ${today.dayIndex + 1} of ${ReadingPlan.TOTAL_DAYS}",
                style = MaterialTheme.typography.titleMedium
            )
            LinearProgressIndicator(
                progress = { ReadingPlan.progressFraction(start).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Today: ${today.label}", style = MaterialTheme.typography.titleLarge)
            if (passageText.isNotBlank()) {
                Text(
                    passageText.take(500) + if (passageText.length > 500) "…" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "Loading ${prefs.bibleVersion.displayName} online, or open Bible for full text. Offline uses KJV samples when available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        val text = passageText.ifBlank {
                            container.bible.passageText(today.passages, prefs.bibleVersion)
                        }
                        val entry = container.repository.getOrCreateTodayEntry(
                            scriptureReference = today.label,
                            scriptureText = text,
                            readingPlanDay = today.dayIndex
                        )
                        onOpenEntry(entry.id)
                    }
                }
            ) {
                Text("Journal today's passage (SOAP)")
            }
            TextButton(
                onClick = {
                    scope.launch { container.prefs.setPlanStart(LocalDate.now()) }
                }
            ) {
                Text("Restart plan from today")
            }

            Text("Next two weeks", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(upcoming, key = { it.dayIndex }) { day ->
                    Text(
                        "Day ${day.dayIndex + 1}: ${day.label}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
