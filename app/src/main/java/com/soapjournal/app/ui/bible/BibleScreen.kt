package com.soapjournal.app.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.bible.BibleVerse
import com.soapjournal.app.data.bible.BibleVersion
import com.soapjournal.app.data.prefs.UserPreferences
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
import kotlinx.coroutines.launch

class BibleViewModel(private val container: AppContainer) : ViewModel() {
    val preferences = container.prefs.preferences
    val books = container.bible.books()

    fun chapters(book: String) = container.bible.chaptersFor(book)

    suspend fun verses(book: String, chapter: Int, version: BibleVersion): List<BibleVerse> =
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
    var verses by remember { mutableStateOf<List<BibleVerse>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var usedOfflineFallback by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val surfaces = LocalJournalSurfaces.current

    LaunchedEffect(book, chapter, prefs.bibleVersion) {
        loading = true
        val loaded = vm.verses(book, chapter, prefs.bibleVersion)
        verses = loaded
        usedOfflineFallback = loaded.isNotEmpty() &&
            loaded.first().version == BibleVersion.KJV &&
            prefs.bibleVersion != BibleVersion.KJV
        loading = false
    }

    Scaffold(
        containerColor = surfaces.paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaces.paper),
                title = {
                    Column {
                        Text("Scripture", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "$book $chapter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    TextButton(onClick = { bookMenu = true }) { Text(book) }
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
                Box {
                    TextButton(onClick = { chapterMenu = true }) { Text("Ch $chapter") }
                    DropdownMenu(
                        expanded = chapterMenu,
                        onDismissRequest = { chapterMenu = false }
                    ) {
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
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    TextButton(onClick = { versionMenu = true }) {
                        Text(prefs.bibleVersion.displayName)
                    }
                    DropdownMenu(
                        expanded = versionMenu,
                        onDismissRequest = { versionMenu = false }
                    ) {
                        BibleVersion.entries.forEach { version ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        buildString {
                                            append(version.displayName)
                                            when {
                                                version.onlineAvailable && version.offlineAvailable ->
                                                    append(" · online / offline")
                                                version.onlineAvailable ->
                                                    append(" · online")
                                                version.offlineAvailable ->
                                                    append(" · offline")
                                            }
                                        }
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
            }

            val statusLine = when {
                usedOfflineFallback ->
                    "Offline · showing KJV samples"
                prefs.bibleVersion.onlineAvailable ->
                    "${prefs.bibleVersion.displayName} · words of Jesus in red"
                else ->
                    "${prefs.bibleVersion.displayName}"
            }
            Text(
                statusLine,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                verses.isEmpty() -> {
                    Text(
                        "No text loaded. Connect for CSB/NLT, or try again offline for KJV.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                else -> {
                    Button(
                        onClick = {
                            val text = verses.joinToString("\n\n") { "${it.verse} ${it.text}" }
                            onJournalPassage("$book $chapter", text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("Journal this chapter (SOAP)")
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(surfaces.paper)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        items(verses, key = { it.reference }) { verse ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    "${verse.verse}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 10.dp, top = 4.dp)
                                )
                                RedLetterVerseText(
                                    verse = verse,
                                    style = MaterialTheme.typography.bodyLarge,
                                    narratorColor = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            vm.addToMemory(verse.reference, verse.text)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.Psychology,
                                        contentDescription = "Memorize"
                                    )
                                }
                            }
                        }
                        prefs.bibleVersion.copyrightNotice?.let { notice ->
                            item {
                                Text(
                                    notice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
