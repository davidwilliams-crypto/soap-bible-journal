package com.soapjournal.app.data

import com.soapjournal.app.data.ink.InkDocument
import com.soapjournal.app.data.ink.InkStore
import com.soapjournal.app.data.memory.MemoryVerseDao
import com.soapjournal.app.data.memory.MemoryVerseEntity
import com.soapjournal.app.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate
import java.time.ZoneId

class JournalRepository(
    private val dao: SoapEntryDao,
    private val inkStore: InkStore,
    private val memoryDao: MemoryVerseDao,
    private val prefs: UserPreferencesRepository
) {
    private val _milestoneEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Emits a streak milestone (7/30/100/365) the moment it's reached. */
    val milestoneEvents: SharedFlow<Int> = _milestoneEvents.asSharedFlow()

    fun observeEntries(): Flow<List<SoapEntryEntity>> = dao.observeAll()

    fun search(query: String): Flow<List<SoapEntryEntity>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    fun observeMemoryVerses(): Flow<List<MemoryVerseEntity>> = memoryDao.observeAll()

    fun observeMemoryVerseCount(): Flow<Int> = memoryDao.observeCount()

    fun observeCompletedEntryCount(): Flow<Int> = dao.observeCompletedCount()

    fun observeDaysJournaledCount(): Flow<Int> = dao.observeDaysJournaledCount()

    /** Distinct completed-entry days on or after [sinceEpochDay], ascending — for a heatmap. */
    fun observeHeatmapDays(sinceEpochDay: Long): Flow<List<Long>> =
        dao.observeCompletedDaysSince(sinceEpochDay)

    suspend fun getEntry(id: Long): SoapEntryEntity? = dao.getById(id)

    suspend fun getTodayEntry(date: LocalDate = LocalDate.now()): SoapEntryEntity? =
        dao.getForDate(date.toEpochDay())

    suspend fun getOrCreateTodayEntry(
        scriptureReference: String = "",
        scriptureText: String = "",
        readingPlanDay: Int? = null
    ): SoapEntryEntity {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        dao.getForDate(today)?.let { existing ->
            // Callers that pass a passage (Bible / reading plan) should apply it even when
            // today's entry already has a different or empty reference.
            if (scriptureReference.isNotBlank() &&
                (existing.scriptureReference.isBlank() ||
                    existing.scriptureReference != scriptureReference ||
                    existing.scriptureText != scriptureText ||
                    (readingPlanDay != null && existing.readingPlanDay != readingPlanDay))
            ) {
                val updated = existing.copy(
                    scriptureReference = scriptureReference,
                    scriptureText = scriptureText,
                    readingPlanDay = readingPlanDay ?: existing.readingPlanDay,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
                dao.update(updated)
                return updated
            }
            return existing
        }

        val now = System.currentTimeMillis()
        val id = dao.insert(
            SoapEntryEntity(
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                entryDateEpochDay = today,
                scriptureReference = scriptureReference,
                scriptureText = scriptureText,
                readingPlanDay = readingPlanDay,
                isDraft = true
            )
        )
        return dao.getById(id)!!
    }

    suspend fun createEntry(date: LocalDate = LocalDate.now()): SoapEntryEntity {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            SoapEntryEntity(
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                entryDateEpochDay = date.toEpochDay(),
                isDraft = true
            )
        )
        return dao.getById(id)!!
    }

    suspend fun updateMetadata(
        entryId: Long,
        scriptureReference: String,
        scriptureText: String,
        tags: String,
        markSaved: Boolean = false
    ) {
        val existing = dao.getById(entryId) ?: return
        dao.update(
            existing.copy(
                scriptureReference = scriptureReference,
                scriptureText = scriptureText,
                tags = tags,
                updatedAtEpochMs = System.currentTimeMillis(),
                isDraft = if (markSaved) false else existing.isDraft
            )
        )
        if (markSaved) {
            maybeRecordCompletion(existing)
        }
    }

    suspend fun updateFollowThrough(
        entryId: Long,
        applicationDone: Boolean? = null,
        prayerDone: Boolean? = null
    ) {
        val existing = dao.getById(entryId) ?: return
        dao.update(
            existing.copy(
                applicationFollowThrough = applicationDone ?: existing.applicationFollowThrough,
                prayerFollowThrough = prayerDone ?: existing.prayerFollowThrough,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadInk(entryId: Long, section: SoapSection): InkDocument =
        inkStore.load(entryId, section)

    suspend fun saveInk(entryId: Long, section: SoapSection, document: InkDocument) {
        inkStore.save(entryId, section, document)
        val existing = dao.getById(entryId) ?: return
        val hasContent = document.strokes.isNotEmpty()
        dao.update(
            existing.copy(
                updatedAtEpochMs = System.currentTimeMillis(),
                // Clearing ink should not flip a draft to completed.
                isDraft = if (hasContent) false else existing.isDraft
            )
        )
        if (hasContent) {
            maybeRecordCompletion(existing)
        }
    }

    suspend fun deleteEntry(entryId: Long) {
        inkStore.deleteAllForEntry(entryId)
        dao.deleteById(entryId)
    }

    suspend fun addMemoryVerse(
        reference: String,
        text: String,
        source: String = "manual"
    ) {
        memoryDao.insert(
            MemoryVerseEntity(
                reference = reference.trim(),
                text = text.trim(),
                source = source
            )
        )
    }

    suspend fun deleteMemoryVerse(id: Long) = memoryDao.delete(id)

    suspend fun reviewMemoryVerse(id: Long) = memoryDao.markReviewed(id)

    private suspend fun maybeRecordCompletion(entry: SoapEntryEntity) {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        // Editing past entries must not rewrite or reset today's streak.
        if (entry.entryDateEpochDay == today) {
            val milestone = prefs.recordDailyCompletion(LocalDate.ofEpochDay(today))
            if (milestone != null) {
                _milestoneEvents.emit(milestone)
            }
        }
    }
}
