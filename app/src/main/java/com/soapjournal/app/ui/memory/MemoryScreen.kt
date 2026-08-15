package com.soapjournal.app.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.memory.MemoryVerseEntity
import com.soapjournal.app.ui.components.ConfirmActionDialog
import com.soapjournal.app.ui.components.NocturneCard
import com.soapjournal.app.ui.components.PrimaryButton
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(private val container: AppContainer) : ViewModel() {
    val verses = container.repository.observeMemoryVerses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(reference: String, text: String) {
        viewModelScope.launch {
            container.repository.addMemoryVerse(reference, text, source = "manual")
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { container.repository.deleteMemoryVerse(id) }
    }

    fun review(id: Long) {
        viewModelScope.launch { container.repository.reviewMemoryVerse(id) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MemoryViewModel(container) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val vm: MemoryViewModel = viewModel(factory = MemoryViewModel.Factory(container))
    val verses by vm.verses.collectAsStateWithLifecycle()
    val surfaces = LocalJournalSurfaces.current
    var reference by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var practice by remember { mutableStateOf<MemoryVerseEntity?>(null) }
    var revealed by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    pendingDeleteId?.let { id ->
        ConfirmActionDialog(
            title = "Remove this verse?",
            body = "It will leave your memorization list. You can add it again anytime.",
            confirmLabel = "Remove",
            onConfirm = {
                vm.delete(id)
                if (practice?.id == id) {
                    practice = null
                    revealed = false
                }
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
                title = { Text("Memorize", style = MaterialTheme.typography.titleLarge) },
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
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Hide the words. Recall them. Let Scripture settle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            practice?.let { card ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PRACTICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(card.reference, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (revealed) card.text else "········  ········  ········",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = if (revealed) FontStyle.Normal else FontStyle.Italic
                        ),
                        color = if (revealed) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { revealed = !revealed }) {
                            Text(if (revealed) "Hide" else "Reveal")
                        }
                        PrimaryButton(onClick = {
                            vm.review(card.id)
                            practice = null
                            revealed = false
                        }) {
                            Text("Remembered")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Verse text") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            PrimaryButton(
                onClick = {
                    if (reference.isNotBlank() && text.isNotBlank()) {
                        vm.add(reference, text)
                        reference = ""
                        text = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add verse")
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(verses, key = { it.id }) { verse ->
                    NocturneCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(verse.reference, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Mastery ${verse.masteryLevel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        practice = verse
                                        revealed = false
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Practice")
                                }
                            }
                            IconButton(onClick = { pendingDeleteId = verse.id }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
