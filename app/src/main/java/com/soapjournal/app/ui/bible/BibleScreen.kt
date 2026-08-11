package com.soapjournal.app.ui.bible

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.bible.BibleVersion
import com.soapjournal.app.data.prefs.UserPreferences
import kotlinx.coroutines.launch

class BibleViewModel(private val container: AppContainer) : ViewModel() {
    val preferences = container.prefs.preferences
    val books = container.bible.books()

    fun chapters(book: String) = container.bible.chaptersFor(book)

    fun verses(book: String, chapter: Int, version: BibleVersion) =
        container.bible.chapter(book, chapter, version)

    fun setVersion(version: BibleVersion) {
        viewModelScope.launch { container.prefs.setBibleVersion(version) }
    }

    fun addToMemory(reference: String, text: String) {
        viewModelScope.launch {
            container.repository.addMemoryVerse(reference, text, source = "bible")
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BibleViewModel(container) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onJournalPassage: (reference: String, text: String) -> Unit
) {
    val vm: BibleViewModel = viewModel(factory = BibleViewModel.Factory(container))
    val prefs by vm.preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
    var book by remember { mutableStateOf(vm.books.firstOrNull() ?: "John") }
    var chapter by remember { mutableIntStateOf(vm.chapters(book).firstOrNull() ?: 1) }
    var versionMenu by remember { mutableStateOf(false) }
    var bookMenu by remember { mutableStateOf(false) }
    var chapterMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val verses = vm.verses(book, chapter, prefs.bibleVersion)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bible") },
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
            Box {
                OutlinedButton(
                    onClick = { versionMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Version: ${prefs.bibleVersion.displayName}")
                }
                DropdownMenu(expanded = versionMenu, onDismissRequest = { versionMenu = false }) {
                    BibleVersion.entries.forEach { version ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (version.offlineAvailable) version.displayName
                                    else "${version.displayName} (licensed — shows KJV offline)"
                                )
                            },
                            onClick = {
                                vm.setVersion(version)
                                versionMenu = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { bookMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(book)
                    }
                    DropdownMenu(expanded = bookMenu, onDismissRequest = { bookMenu = false }) {
                        vm.books.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b) },
                                onClick = {
                                    book = b
                                    chapter = vm.chapters(b).firstOrNull() ?: 1
                                    bookMenu = false
                                }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { chapterMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ch $chapter")
                    }
                    DropdownMenu(expanded = chapterMenu, onDismissRequest = { chapterMenu = false }) {
                        vm.chapters(book).forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.toString()) },
                                onClick = {
                                    chapter = c
                                    chapterMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (!prefs.bibleVersion.offlineAvailable) {
                Text(
                    "Offline reading uses public-domain KJV. ${prefs.bibleVersion.displayName} requires a licensed source.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (verses.isEmpty()) {
                Text(
                    "This chapter isn’t in the offline library yet. Try John 3, Psalm 23, or Genesis 1 — or journal from the reading plan.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                TextButton(
                    onClick = {
                        val text = verses.joinToString("\n\n") { "${it.verse} ${it.text}" }
                        onJournalPassage("$book $chapter", text)
                    }
                ) {
                    Text("Start SOAP from this chapter")
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(verses, key = { it.reference }) { verse ->
                        Row {
                            Text(
                                "${verse.verse}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                verse.text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    scope.launch { vm.addToMemory(verse.reference, verse.text) }
                                }
                            ) {
                                Icon(Icons.Outlined.Psychology, contentDescription = "Memorize")
                            }
                        }
                    }
                }
            }
        }
    }
}
