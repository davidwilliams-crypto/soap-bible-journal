package com.soapjournal.app.ui.editor

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PostAdd
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.soapjournal.app.data.SoapSection
import com.soapjournal.app.ui.components.ConfirmActionDialog
import com.soapjournal.app.ui.export.PdfExporter
import com.soapjournal.app.ui.ink.InkCanvas
import com.soapjournal.app.ui.ink.InkTool
import com.soapjournal.app.ui.share.AccountabilityShare
import com.soapjournal.app.ui.theme.LocalJournalSurfaces
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
    val surfaces = LocalJournalSurfaces.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focus = viewModel.focusWriting

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.flushForBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.flushForBackground()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel.selectedSection) {
        val index = sections.indexOf(viewModel.selectedSection).coerceAtLeast(0)
        if (pagerState.currentPage != index) {
            pagerState.scrollToPage(index)
        }
    }

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
        containerColor = surfaces.paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaces.paper,
                    scrolledContainerColor = surfaces.paperDeep
                ),
                title = {
                    Column {
                        Text(
                            if (focus) viewModel.selectedSection.title else "SOAP",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!focus) {
                            Text(
                                text = viewModel.scriptureReference.ifBlank { "Add scripture" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFocusWriting() }) {
                        Icon(
                            if (focus) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                            contentDescription = if (focus) "Show tools" else "Focus writing"
                        )
                    }
                    if (!focus) {
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
                                    val uri = PdfExporter.exportEntry(
                                        context,
                                        entry.copy(
                                            scriptureReference = viewModel.scriptureReference,
                                            scriptureText = viewModel.scriptureText,
                                            tags = viewModel.tags
                                        ),
                                        ink
                                    )
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(share, "Export SOAP entry")
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = "Export PDF")
                        }
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
                .imePadding()
        ) {
            if (!focus) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 12.dp,
                    containerColor = surfaces.paper,
                    divider = {}
                ) {
                    sections.forEachIndexed { index, section ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    section.title,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = false,
                beyondViewportPageCount = 0
            ) { page ->
                val section = sections[page]
                when (section) {
                    SoapSection.SCRIPTURE -> ScripturePane(viewModel)
                    else -> InkPane(
                        section = section,
                        viewModel = viewModel,
                        focusWriting = focus
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
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            SoapSection.SCRIPTURE.prompt,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Anchor this entry in the Word. Observation, Application, and Prayer wait on the next pages.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = viewModel.scriptureReference,
            onValueChange = viewModel::updateReference,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Reference") },
            placeholder = { Text("e.g. John 3:16–17") },
            singleLine = true
        )
        OutlinedTextField(
            value = viewModel.scriptureText,
            onValueChange = viewModel::updateScriptureText,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Passage") },
            placeholder = { Text("Paste or type the passage") },
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 8,
            maxLines = 16
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
            Text("Save Scripture for memorization")
        }
    }
}

@Composable
private fun InkPane(
    section: SoapSection,
    viewModel: EntryEditorViewModel,
    focusWriting: Boolean
) {
    val state = viewModel.sectionInk[section] ?: SectionInkState()
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        ConfirmActionDialog(
            title = "Clear ${section.title.lowercase()}?",
            body = "This removes the ink on this page. You can still undo right after if you clear by mistake — this confirms a full wipe.",
            confirmLabel = "Clear page",
            onConfirm = {
                viewModel.clearSection(section)
                confirmClear = false
            },
            onDismiss = { confirmClear = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!focusWriting) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(section.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    section.prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
            FilterChip(
                selected = viewModel.tool == InkTool.PAN,
                onClick = { viewModel.chooseTool(InkTool.PAN) },
                label = { Text("Move") },
                leadingIcon = { Icon(Icons.Outlined.PanTool, contentDescription = null) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.extendCanvas(section) }) {
                Icon(Icons.Outlined.PostAdd, contentDescription = "Add writing space")
            }
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
            if (!focusWriting) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear page")
                }
            }
        }

        if (!focusWriting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stroke", style = MaterialTheme.typography.labelMedium)
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
                    modifier = Modifier.padding(horizontal = 8.dp),
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
        }

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
