package com.soapjournal.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.data.plan.ReadingPlan
import com.soapjournal.app.data.prefs.liveStreak
import com.soapjournal.app.ui.bible.RedLetterVerseText
import com.soapjournal.app.ui.components.JournalAtmosphere
import com.soapjournal.app.ui.components.Kicker
import com.soapjournal.app.ui.components.PrimaryButton
import com.soapjournal.app.ui.components.RitualEnter
import com.soapjournal.app.ui.components.ScriptureQuotation
import com.soapjournal.app.ui.components.SecondaryButton
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
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit,
    resumeEntryId: Long? = null
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val votd by viewModel.verseOfTheDay.collectAsStateWithLifecycle()
    val votdLoading by viewModel.votdLoading.collectAsStateWithLifecycle()
    val today = LocalDate.now(ZoneId.systemDefault())
    val todayEntry = entries.firstOrNull { it.entryDateEpochDay == today.toEpochDay() }
    val reading = viewModel.todayReading(prefs)
    val context = LocalContext.current

    JournalAtmosphere {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                RitualEnter {
                    // First viewport: one ritual composition — brand, date, invitation, CTA.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SOAP",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Bible Journal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (todayEntry != null) {
                            "Return to today’s reflection."
                        } else {
                            "Open the page. Meet the Word with pen in hand."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(
                        onClick = {
                            if (todayEntry != null) onOpenEntry(todayEntry.id)
                            else viewModel.openToday(onOpenEntry)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            if (todayEntry != null) "Continue today’s SOAP" else "Begin today’s SOAP",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (resumeEntryId != null && resumeEntryId != todayEntry?.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { onOpenEntry(resumeEntryId) }) {
                            Text("Pick up where you left off")
                        }
                    }
                    if (todayEntry != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.createNew(onOpenEntry) }) {
                            Text("Start another entry")
                        }
                        val follow = buildString {
                            if (todayEntry.applicationFollowThrough) append("Application kept  ·  ")
                            else append("Application open  ·  ")
                            if (todayEntry.prayerFollowThrough) append("Prayer kept")
                            else append("Prayer open")
                        }
                        Text(
                            follow,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Below the fold: scripture, then plan — quiet, not a dashboard.
                when {
                    votdLoading && votd == null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    votd != null -> {
                        val todayVerse = votd!!
                        val verse = todayVerse.verse
                        ScriptureQuotation(
                            eyebrow = "Verse of the day",
                            reference = verse.reference
                        ) {
                            RedLetterVerseText(
                                verse = verse,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                narratorColor = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (todayVerse.fromOfflineFallback) {
                                    "Offline · ${verse.version.displayName}"
                                } else {
                                    verse.version.displayName
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    if (todayVerse.verse.text.startsWith("Unable to load")) {
                                        viewModel.refreshVerseOfTheDay(force = true)
                                    } else {
                                        viewModel.addVotdToMemory()
                                        Toast.makeText(
                                            context,
                                            "Saved for memorization",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    if (todayVerse.verse.text.startsWith("Unable to load")) {
                                        "Try again"
                                    } else {
                                        "Save for memorization"
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPlan),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Kicker("Today's reading")
                    Text(
                        reading.label,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Day ${reading.dayIndex + 1} of ${ReadingPlan.TOTAL_DAYS}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { viewModel.openTodayFromPlan(onOpenEntry) },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Journal this passage")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickLink("Bible", Icons.AutoMirrored.Outlined.MenuBook, onOpenBible)
                    QuickLink("Memorize", Icons.Outlined.Psychology, onOpenMemory)
                    QuickLink("Journal", Icons.Outlined.Notes, onOpenHistory)
                    QuickLink("Insights", Icons.Outlined.TrendingUp, onOpenInsights)
                }

                val streak = liveStreak(prefs)
                if (streak > 0) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onOpenInsights)
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            "$streak-day rhythm · longest ${prefs.longestStreak}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuickLink(label: String, icon: ImageVector, onClick: () -> Unit) {
    SecondaryButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
