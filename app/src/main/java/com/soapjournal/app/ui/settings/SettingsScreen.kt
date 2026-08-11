package com.soapjournal.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.AppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val prefs by container.prefs.preferences.collectAsStateWithLifecycle(
        initialValue = com.soapjournal.app.data.prefs.UserPreferences()
    )
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Themes", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark theme", modifier = Modifier.weight(1f))
                Switch(
                    checked = prefs.darkTheme,
                    onCheckedChange = { enabled ->
                        scope.launch { container.prefs.setDarkTheme(enabled) }
                    }
                )
            }

            Text("Reminders & streaks", style = MaterialTheme.typography.titleLarge)
            Text(
                "Current streak: ${prefs.currentStreak} · Longest: ${prefs.longestStreak}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily SOAP reminder", modifier = Modifier.weight(1f))
                Switch(
                    checked = prefs.remindersEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            container.prefs.setRemindersEnabled(enabled)
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                container.reminders.scheduleDaily(prefs.reminderHour, prefs.reminderMinute)
                            } else {
                                container.reminders.cancelAll()
                            }
                        }
                    }
                )
            }
            Text("Reminder hour: ${prefs.reminderHour}:00")
            Slider(
                value = prefs.reminderHour.toFloat(),
                onValueChange = { hour ->
                    scope.launch {
                        container.prefs.setReminderTime(hour.toInt(), 0)
                        if (prefs.remindersEnabled) {
                            container.reminders.scheduleDaily(hour.toInt(), 0)
                        }
                    }
                },
                valueRange = 5f..22f,
                steps = 16
            )

            Text("Follow-through", style = MaterialTheme.typography.titleLarge)
            Text(
                "Evening nudge to live out your application and prayer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Follow-through reminder", modifier = Modifier.weight(1f))
                Switch(
                    checked = prefs.followThroughEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            container.prefs.setFollowThroughEnabled(enabled)
                            if (enabled) {
                                container.reminders.scheduleFollowThrough(prefs.followThroughHour)
                            }
                        }
                    }
                )
            }
            Text("Follow-through hour: ${prefs.followThroughHour}:00")
            Slider(
                value = prefs.followThroughHour.toFloat(),
                onValueChange = { hour ->
                    scope.launch {
                        container.prefs.setFollowThroughHour(hour.toInt())
                        if (prefs.followThroughEnabled) {
                            container.reminders.scheduleFollowThrough(hour.toInt())
                        }
                    }
                },
                valueRange = 16f..22f,
                steps = 5
            )

            Text(
                "Preferred Bible version is chosen in the Bible screen. Offline text is KJV (public domain).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Contact the developer to report bugs and get assistance.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
