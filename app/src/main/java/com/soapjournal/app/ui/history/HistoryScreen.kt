package com.soapjournal.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soapjournal.app.ui.components.ConfirmActionDialog
import com.soapjournal.app.ui.share.AccountabilityShare
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val surfaces = LocalJournalSurfaces.current
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    pendingDeleteId?.let { id ->
        ConfirmActionDialog(
            title = "Delete this entry?",
            body = "The SOAP pages for this day will be removed. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.delete(id)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
    }

    Scaffold(
        containerColor = surfaces.paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaces.paper),
                title = { Text("Journal", style = MaterialTheme.typography.titleLarge) },
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
        ) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                singleLine = true,
                label = { Text("Search reference or tags") }
            )

            if (entries.isEmpty()) {
                val searching = viewModel.query.isNotBlank()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (searching) "No matching entries" else "Your journal is empty",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        if (searching) {
                            "Try a different reference or tag."
                        } else {
                            "Begin a SOAP entry — the pages will gather here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        val date = LocalDate.ofEpochDay(entry.entryDateEpochDay)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenEntry(entry.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.scriptureReference.ifBlank { "Untitled entry" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = buildString {
                                        append(
                                            date.format(
                                                DateTimeFormatter.ofPattern("MMM d, yyyy")
                                            )
                                        )
                                        if (entry.tags.isNotBlank()) {
                                            append(" · ")
                                            append(entry.tags)
                                        }
                                        if (entry.isDraft) append(" · draft")
                                        if (entry.applicationFollowThrough) append(" · A✓")
                                        if (entry.prayerFollowThrough) append(" · P✓")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                AccountabilityShare.shareReflection(context, entry)
                            }) {
                                Icon(
                                    Icons.Outlined.Share,
                                    contentDescription = "Share with partners"
                                )
                            }
                            IconButton(onClick = { pendingDeleteId = entry.id }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                            }
                        }
                        HorizontalDivider(color = surfaces.rule)
                    }
                }
            }
        }
    }
}
