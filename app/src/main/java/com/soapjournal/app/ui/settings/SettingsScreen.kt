package com.soapjournal.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.AppContainer
import com.soapjournal.app.update.AvailableUpdate
import com.soapjournal.app.update.UpdateCheckResult
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
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
    val context = LocalContext.current
    val surfaces = LocalJournalSurfaces.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    var updateStatus by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var showAdvancedUpdates by remember { mutableStateOf(prefs.githubUpdateToken.isNotBlank()) }

    Scaffold(
        containerColor = surfaces.paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaces.paper),
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection("Appearance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark paper", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Warm night pages for evening journaling",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.darkTheme,
                        onCheckedChange = { enabled ->
                            scope.launch { container.prefs.setDarkTheme(enabled) }
                        }
                    )
                }
            }

            SettingsSection("Reminders") {
                Text(
                    if (prefs.currentStreak > 0) {
                        "Current rhythm: ${prefs.currentStreak} days · longest ${prefs.longestStreak}"
                    } else {
                        "Gentle nudges — never a scoreboard on your home page."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        permissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                    container.reminders.scheduleDaily(
                                        prefs.reminderHour,
                                        prefs.reminderMinute
                                    )
                                } else {
                                    container.reminders.cancelDaily()
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

                Text(
                    "Evening follow-through for application and prayer.",
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
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        permissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                    container.reminders.scheduleFollowThrough(
                                        prefs.followThroughHour
                                    )
                                } else {
                                    container.reminders.cancelFollowThrough()
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
            }

            SettingsSection("Updates") {
                Text(
                    "Installed ${container.updates.currentVersionName()} " +
                        "(${container.updates.currentVersionCode()})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Checks public GitHub Releases for a newer APK, then opens the system installer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            checking = true
                            updateStatus = "Checking GitHub Releases…"
                            availableUpdate = null
                            when (val result = container.updates.checkForUpdate()) {
                                is UpdateCheckResult.Available -> {
                                    availableUpdate = result.update
                                    updateStatus =
                                        "Update available: ${result.update.versionName} " +
                                            "(${result.update.versionCode})"
                                }
                                UpdateCheckResult.UpToDate -> {
                                    updateStatus = "You’re on the latest release."
                                }
                                is UpdateCheckResult.Error -> {
                                    updateStatus = result.message
                                }
                            }
                            checking = false
                        }
                    },
                    enabled = !checking && !downloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (checking) "Checking…" else "Check for updates")
                }

                availableUpdate?.let { update ->
                    Button(
                        onClick = {
                            scope.launch {
                                if (!container.updates.canInstallPackages()) {
                                    Toast.makeText(
                                        context,
                                        "Allow installs from this app, then tap Download & install again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    context.startActivity(container.updates.permissionIntent())
                                    return@launch
                                }
                                downloading = true
                                downloadProgress = 0f
                                updateStatus = "Downloading ${update.versionName}…"
                                runCatching {
                                    val file = container.updates.downloadApk(update) {
                                        downloaded,
                                        total ->
                                        downloadProgress = if (total > 0) {
                                            downloaded.toFloat() / total.toFloat()
                                        } else {
                                            0f
                                        }
                                    }
                                    updateStatus = "Opening installer…"
                                    container.updates.installApk(file)
                                }.onFailure {
                                    updateStatus = it.message ?: "Download failed"
                                }
                                downloading = false
                            }
                        },
                        enabled = !downloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (downloading) "Downloading…" else "Download & install")
                    }
                    if (downloading) {
                        LinearProgressIndicator(
                            progress = { downloadProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (update.htmlUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(update.htmlUrl))
                                )
                            }
                        ) {
                            Text("View release notes")
                        }
                    }
                }

                updateStatus?.let { status ->
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = { showAdvancedUpdates = !showAdvancedUpdates }) {
                    Text(if (showAdvancedUpdates) "Hide advanced" else "Advanced (private repo token)")
                }
                if (showAdvancedUpdates) {
                    OutlinedTextField(
                        value = prefs.githubUpdateToken,
                        onValueChange = { value ->
                            scope.launch { container.prefs.setGithubUpdateToken(value) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("GitHub token (optional)") },
                        placeholder = { Text("Only needed for private repos") },
                        singleLine = true
                    )
                }
            }

            SettingsSection("About") {
                Text(
                    "Bible versions load online via the selected translation (CSB, ESV, NIV, NLT, MSG, NASB, AMP, KJV, NKJV). Offline fallback uses public-domain KJV samples.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Typography: Instrument Serif, Cardo, Atkinson Hyperlegible (SIL Open Font License).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Designed as a calm, paper-first SOAP journal — ink over chrome.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        content()
    }
}
