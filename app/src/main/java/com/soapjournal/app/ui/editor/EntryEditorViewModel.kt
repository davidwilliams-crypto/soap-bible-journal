package com.soapjournal.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soapjournal.app.data.JournalRepository
import com.soapjournal.app.data.SoapEntryEntity
import com.soapjournal.app.data.SoapSection
import com.soapjournal.app.data.ink.InkDocument
import com.soapjournal.app.ui.ink.InkTool
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class SectionInkState(
    val document: InkDocument = InkDocument(),
    val undoStack: List<InkDocument> = emptyList(),
    val redoStack: List<InkDocument> = emptyList()
)

class EntryEditorViewModel(
    private val repository: JournalRepository,
    private val entryId: Long
) : ViewModel() {

    var entry by mutableStateOf<SoapEntryEntity?>(null)
        private set
    var scriptureReference by mutableStateOf("")
        private set
    var scriptureText by mutableStateOf("")
        private set
    var tags by mutableStateOf("")
        private set
    var selectedSection by mutableStateOf(SoapSection.SCRIPTURE)
        private set
    var tool by mutableStateOf(InkTool.PEN)
        private set
    var strokeWidth by mutableStateOf(4f)
        private set
    var sectionInk by mutableStateOf(
        SoapSection.entries.associateWith { SectionInkState() }
    )
        private set
    var isLoading by mutableStateOf(true)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set
    var applicationFollowThrough by mutableStateOf(false)
        private set
    var prayerFollowThrough by mutableStateOf(false)
        private set

    private var metadataPersistJob: Job? = null
    private var inkPersistJob: Job? = null
    private var metadataDirty = false
    private val dirtyInkSections = mutableSetOf<SoapSection>()

    init {
        viewModelScope.launch {
            val loaded = repository.getEntry(entryId)
            entry = loaded
            if (loaded != null) {
                scriptureReference = loaded.scriptureReference
                scriptureText = loaded.scriptureText
                tags = loaded.tags
                applicationFollowThrough = loaded.applicationFollowThrough
                prayerFollowThrough = loaded.prayerFollowThrough
                val inkMap = SoapSection.entries.associateWith { section ->
                    SectionInkState(document = repository.loadInk(entryId, section))
                }
                sectionInk = inkMap
            }
            isLoading = false
        }
    }

    fun selectSection(section: SoapSection) {
        selectedSection = section
    }

    fun chooseTool(value: InkTool) {
        tool = value
    }

    fun changeStrokeWidth(value: Float) {
        strokeWidth = value
    }

    fun updateReference(value: String) {
        scriptureReference = value
        metadataDirty = true
        scheduleMetadataSave()
    }

    fun updateScriptureText(value: String) {
        scriptureText = value
        metadataDirty = true
        scheduleMetadataSave()
    }

    fun updateTags(value: String) {
        tags = value
        metadataDirty = true
        scheduleMetadataSave()
    }

    fun onInkChanged(section: SoapSection, document: InkDocument) {
        val current = sectionInk[section] ?: SectionInkState()
        sectionInk = sectionInk + (section to current.copy(
            document = document,
            undoStack = (current.undoStack + current.document).takeLast(40),
            redoStack = emptyList()
        ))
        scheduleInkSave(section)
    }

    fun undo(section: SoapSection) {
        val current = sectionInk[section] ?: return
        val previous = current.undoStack.lastOrNull() ?: return
        sectionInk = sectionInk + (section to current.copy(
            document = previous,
            undoStack = current.undoStack.dropLast(1),
            redoStack = current.redoStack + current.document
        ))
        scheduleInkSave(section)
    }

    fun redo(section: SoapSection) {
        val current = sectionInk[section] ?: return
        val next = current.redoStack.lastOrNull() ?: return
        sectionInk = sectionInk + (section to current.copy(
            document = next,
            undoStack = current.undoStack + current.document,
            redoStack = current.redoStack.dropLast(1)
        ))
        scheduleInkSave(section)
    }

    fun clearSection(section: SoapSection) {
        val current = sectionInk[section] ?: SectionInkState()
        val empty = InkDocument(canvasHeight = current.document.canvasHeight)
        sectionInk = sectionInk + (section to current.copy(
            document = empty,
            undoStack = (current.undoStack + current.document).takeLast(40),
            redoStack = emptyList()
        ))
        scheduleInkSave(section)
    }

    fun saveNow() {
        viewModelScope.launch {
            metadataPersistJob?.cancel()
            inkPersistJob?.cancel()
            dirtyInkSections.clear()
            metadataDirty = false
            SoapSection.entries.forEach { section ->
                val doc = sectionInk[section]?.document ?: InkDocument()
                repository.saveInk(entryId, section, doc)
            }
            persistMetadata(markSaved = true)
            entry = repository.getEntry(entryId)
            statusMessage = "Saved"
        }
    }

    fun consumeStatus() {
        statusMessage = null
    }

    fun markApplicationFollowThrough(done: Boolean) {
        applicationFollowThrough = done
        viewModelScope.launch {
            repository.updateFollowThrough(entryId, applicationDone = done)
            entry = repository.getEntry(entryId)
        }
    }

    fun markPrayerFollowThrough(done: Boolean) {
        prayerFollowThrough = done
        viewModelScope.launch {
            repository.updateFollowThrough(entryId, prayerDone = done)
            entry = repository.getEntry(entryId)
        }
    }

    fun addScriptureToMemory() {
        viewModelScope.launch {
            if (scriptureReference.isBlank() || scriptureText.isBlank()) {
                statusMessage = "Add a reference and text first"
                return@launch
            }
            repository.addMemoryVerse(scriptureReference, scriptureText, source = "scripture")
            statusMessage = "Added to memorization"
        }
    }

    override fun onCleared() {
        metadataPersistJob?.cancel()
        inkPersistJob?.cancel()
        // Flush pending edits so leaving within the debounce window does not drop work.
        runBlocking {
            val inkHasContent = dirtyInkSections.any { section ->
                sectionInk[section]?.document?.strokes?.isNotEmpty() == true
            }
            flushPending(markSaved = inkHasContent)
        }
        super.onCleared()
    }

    private fun scheduleMetadataSave() {
        metadataPersistJob?.cancel()
        metadataPersistJob = viewModelScope.launch {
            delay(400)
            persistMetadata(markSaved = false)
            metadataDirty = false
        }
    }

    private fun scheduleInkSave(section: SoapSection) {
        dirtyInkSections.add(section)
        inkPersistJob?.cancel()
        inkPersistJob = viewModelScope.launch {
            delay(350)
            flushInk()
        }
    }

    private suspend fun flushInk() {
        val sections = dirtyInkSections.toList()
        dirtyInkSections.clear()
        var anyContent = false
        sections.forEach { section ->
            val doc = sectionInk[section]?.document ?: InkDocument()
            if (doc.strokes.isNotEmpty()) anyContent = true
            repository.saveInk(entryId, section, doc)
        }
        if (sections.isNotEmpty() || metadataDirty) {
            // Clearing a canvas must not mark the day complete.
            persistMetadata(markSaved = anyContent)
            metadataDirty = false
            entry = repository.getEntry(entryId)
        }
    }

    private suspend fun flushPending(markSaved: Boolean) {
        val sections = dirtyInkSections.toList()
        dirtyInkSections.clear()
        sections.forEach { section ->
            val doc = sectionInk[section]?.document ?: InkDocument()
            repository.saveInk(entryId, section, doc)
        }
        if (metadataDirty || sections.isNotEmpty()) {
            persistMetadata(markSaved = markSaved)
            metadataDirty = false
        }
    }

    private suspend fun persistMetadata(markSaved: Boolean) {
        repository.updateMetadata(
            entryId = entryId,
            scriptureReference = scriptureReference,
            scriptureText = scriptureText,
            tags = tags,
            markSaved = markSaved
        )
    }

    class Factory(
        private val repository: JournalRepository,
        private val entryId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EntryEditorViewModel(repository, entryId) as T
        }
    }
}
