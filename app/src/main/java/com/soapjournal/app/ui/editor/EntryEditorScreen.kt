package com.soapjournal.app.ui.editor

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.soapjournal.app.data.SoapSection
import com.soapjournal.app.ui.export.PdfExporter
import com.soapjournal.app.ui.ink.InkCanvas
import com.soapjournal.app.ui.ink.InkTool
import com.soapjournal.app.ui.share.AccountabilityShare
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(
    viewModel: EntryEditorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sections = SoapSection.entries
    val pagerState = rememberPagerState(
        initialPage = sections.indexOf(viewModel.selectedSection).coerceAtLeast(0),
        pageCount = { sections.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectSection(sections[pagerState.currentPage])
    }

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SOAP Entry")
                        Text(
                            text = viewModel.scriptureReference.ifBlank { "Add a scripture reference" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.addScriptureToMemory() }) {
                        Icon(Icons.Outlined.Psychology, contentDescription = "Memorize")
                    }
                    IconButton(onClick = {
                        viewModel.entry?.let {
                            AccountabilityShare.shareReflection(
                                context,
                                it.copy(
                                    scriptureReference = viewModel.scriptureReference,
                                    scriptureText = viewModel.scriptureText,
                                    tags = viewModel.tags,
                                    applicationFollowThrough = viewModel.applicationFollowThrough,
                                    prayerFollowThrough = viewModel.prayerFollowThrough
                                )
                            )
                        }
                    }) {
                        Icon(Icons.Outlined.Groups, contentDescription = "Share with partners")
                    }
                    IconButton(onClick = { viewModel.saveNow() }) {
                        Icon(Icons.Outlined.Save, contentDescription = "Save")
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val entry = viewModel.entry ?: return@launch
                                val ink = SoapSection.entries.associateWith { section ->
                                    viewModel.sectionInk[section]?.document
                                        ?: com.soapjournal.app.data.ink.InkDocument()
                                }
                                viewModel.saveNow()
                                val uri = PdfExporter.exportEntry(context, entry.copy(
                                    scriptureReference = viewModel.scriptureReference,
                                    scriptureText = viewModel.scriptureText,
                                    tags = viewModel.tags
                                ), ink)
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Export SOAP entry"))
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Export PDF")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                sections.forEachIndexed { index, section ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(section.title.take(1)) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                // Never let pager swipe steal stylus strokes on O/A/P writing pages.
                userScrollEnabled = false
            ) { page ->
                val section = sections[page]
                when (section) {
                    SoapSection.SCRIPTURE -> ScripturePane(viewModel)
                    else -> InkPane(
                        section = section,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun ScripturePane(viewModel: EntryEditorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(SoapSection.SCRIPTURE.prompt, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = viewModel.scriptureReference,
            onValueChange = viewModel::updateReference,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Scripture reference") },
            placeholder = { Text("e.g. John 3:16–17") },
            singleLine = true
        )
        OutlinedTextField(
            value = viewModel.scriptureText,
            onValueChange = viewModel::updateScriptureText,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Passage text (optional)") },
            placeholder = { Text("Paste or type the passage") }
        )
        OutlinedTextField(
            value = viewModel.tags,
            onValueChange = viewModel::updateTags,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tags") },
            placeholder = { Text("faith, prayer, gratitude") },
            singleLine = true
        )
        TextButton(onClick = viewModel::addScriptureToMemory) {
            Text("Add Scripture to memorization")
        }
        Text(
            text = "Tip: use the tabs for Observation, Application, and Prayer to write with your stylus.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InkPane(
    section: SoapSection,
    viewModel: EntryEditorViewModel
) {
    val state = viewModel.sectionInk[section] ?: SectionInkState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Title + prompt on their own row so Observation ("What does it say?")
        // is never crushed by the pen/eraser toolbar on narrow phones.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(section.title, style = MaterialTheme.typography.titleLarge)
            Text(
                section.prompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = viewModel.tool == InkTool.PEN,
                onClick = { viewModel.chooseTool(InkTool.PEN) },
                label = { Text("Pen") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
            )
            FilterChip(
                selected = viewModel.tool == InkTool.ERASER,
                onClick = { viewModel.chooseTool(InkTool.ERASER) },
                label = { Text("Eraser") }
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { viewModel.undo(section) },
                enabled = state.undoStack.isNotEmpty()
            ) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo")
            }
            IconButton(
                onClick = { viewModel.redo(section) },
                enabled = state.redoStack.isNotEmpty()
            ) {
                Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = { viewModel.clearSection(section) }) {
                Icon(Icons.Outlined.Clear, contentDescription = "Clear")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Thickness", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Slider(
                value = viewModel.strokeWidth,
                onValueChange = viewModel::changeStrokeWidth,
                valueRange = 2f..14f,
                modifier = Modifier.weight(1f)
            )
        }

        if (section == SoapSection.APPLICATION || section == SoapSection.PRAYER) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val checked = if (section == SoapSection.APPLICATION) {
                    viewModel.applicationFollowThrough
                } else {
                    viewModel.prayerFollowThrough
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = { done ->
                        if (section == SoapSection.APPLICATION) {
                            viewModel.markApplicationFollowThrough(done)
                        } else {
                            viewModel.markPrayerFollowThrough(done)
                        }
                    }
                )
                Text(
                    if (section == SoapSection.APPLICATION) {
                        "I will follow through on this application"
                    } else {
                        "I prayed this through"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        InkCanvas(
            document = state.document,
            tool = viewModel.tool,
            strokeWidth = viewModel.strokeWidth,
            onDocumentChange = { viewModel.onInkChanged(section, it) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}
