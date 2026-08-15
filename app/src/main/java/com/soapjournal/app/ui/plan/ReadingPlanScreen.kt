package com.soapjournal.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.plan.ReadingPlan
import com.soapjournal.app.ui.components.ConfirmActionDialog
import com.soapjournal.app.ui.components.Kicker
import com.soapjournal.app.ui.components.NocturneCard
import com.soapjournal.app.ui.components.PrimaryButton
import com.soapjournal.app.ui.components.SecondaryButton
import com.soapjournal.app.ui.components.Tag
import com.soapjournal.app.ui.components.TagStyle
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import com.soapjournal.app.ui.theme.ScriptureFamily
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingPlanScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenBible: () -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    val prefs by container.prefs.preferences.collectAsStateWithLifecycle(
        initialValue = com.soapjournal.app.data.prefs.UserPreferences()
    )
    val scope = rememberCoroutineScope()
    val surfaces = LocalJournalSurfaces.current
    val start = LocalDate.ofEpochDay(prefs.planStartEpochDay)
    val today = ReadingPlan.dayFor(start)
    val upcoming = ((today.dayIndex)..(today.dayIndex + 13).coerceAtMost(ReadingPlan.TOTAL_DAYS - 1))
        .map { ReadingPlan.day(it) }
    var passageText by remember(today.dayIndex, prefs.bibleVersion) { mutableStateOf("") }
    var passageLoading by remember { mutableStateOf(true) }
    var confirmRestart by remember { mutableStateOf(false) }

    LaunchedEffect(today.dayIndex, prefs.bibleVersion, prefs.planStartEpochDay) {
        passageLoading = true
        passageText = container.bible.passageText(today.passages, prefs.bibleVersion)
        passageLoading = false
    }

    if (confirmRestart) {
        ConfirmActionDialog(
            title = "Restart the plan?",
            body = "Today becomes day 1 again. Your journal entries stay; only the plan calendar resets.",
            confirmLabel = "Restart",
            onConfirm = {
                scope.launch { container.prefs.setPlanStart(LocalDate.now()) }
                confirmRestart = false
            },
            onDismiss = { confirmRestart = false }
        )
    }

    Scaffold(
        containerColor = surfaces.paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaces.paper),
                title = { Text("Reading plan", style = MaterialTheme.typography.titleLarge) },
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Whole Bible in two years",
                    style = MaterialTheme.typography.headlineMedium
                )
                Tag("Day ${today.dayIndex + 1} / ${ReadingPlan.TOTAL_DAYS}", style = TagStyle.Accent)
            }
            LinearProgressIndicator(
                progress = { ReadingPlan.progressFraction(start).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                trackColor = surfaces.paperDeep
            )
            NocturneCard(modifier = Modifier.fillMaxWidth()) {
                Kicker("Today")
                Spacer(modifier = Modifier.height(6.dp))
                Text(today.label, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(6.dp))
                when {
                    passageLoading -> {
                        Text(
                            "Loading ${prefs.bibleVersion.displayName}…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    passageText.isNotBlank() -> {
                        Text(
                            passageText.take(500) + if (passageText.length > 500) "…" else "",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = ScriptureFamily)
                        )
                    }
                    else -> {
                        Text(
                            "No preview for this passage offline. Journal it to load the full text when connected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(onClick = onOpenBible) {
                        Text("Read passage")
                    }
                    PrimaryButton(
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
                        Text("Journal this passage")
                    }
                }
            }
            TextButton(onClick = { confirmRestart = true }) {
                Text("Restart plan from today")
            }

            Kicker("Up next", modifier = Modifier.padding(top = 8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(upcoming, key = { it.dayIndex }) { day ->
                    Column {
                        Text(
                            "Day ${day.dayIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(day.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
