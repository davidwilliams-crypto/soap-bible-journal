package com.soapjournal.app.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.memory.MemoryVerseEntity
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
    var reference by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var practice by remember { mutableStateOf<MemoryVerseEntity?>(null) }
    var revealed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memorization") },
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
            Text(
                "Add verses from Scripture, Verse of the Day, or type them manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (reference.isNotBlank() && text.isNotBlank()) {
                        vm.add(reference, text)
                        reference = ""
                        text = ""
                    }
                }
            ) {
                Text("Add verse")
            }

            practice?.let { card ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Practice", style = MaterialTheme.typography.titleMedium)
                    Text(card.reference, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (revealed) card.text else "•••• •••• •••• (tap reveal)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { revealed = !revealed }) {
                            Text(if (revealed) "Hide" else "Reveal")
                        }
                        Button(onClick = {
                            vm.review(card.id)
                            practice = null
                            revealed = false
                        }) {
                            Text("Got it (+1 mastery)")
                        }
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(verses, key = { it.id }) { verse ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                        ) {
                            Text(verse.reference, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${verse.source} · mastery ${verse.masteryLevel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = {
                                practice = verse
                                revealed = false
                            }) {
                                Text("Practice")
                            }
                        }
                        IconButton(onClick = { vm.delete(verse.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}
