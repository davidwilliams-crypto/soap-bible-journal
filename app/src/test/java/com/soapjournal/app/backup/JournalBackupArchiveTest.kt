package com.soapjournal.app.backup

import com.soapjournal.app.data.SoapEntryEntity
import com.soapjournal.app.data.memory.MemoryVerseEntity
import com.soapjournal.app.data.prefs.BackupPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalBackupArchiveTest {
    @Test
    fun roundTripsEntriesInkAndPreferences() {
        val payload = JournalBackupPayload(
            manifest = BackupManifest(
                appVersionCode = 8,
                appVersionName = "1.4.0",
                createdAtEpochMs = 1_700_000_000_000L,
                entryCount = 1,
                memoryCount = 1,
                inkFileCount = 1
            ),
            entries = listOf(
                SoapEntryEntity(
                    id = 42,
                    createdAtEpochMs = 1L,
                    updatedAtEpochMs = 2L,
                    entryDateEpochDay = 20000,
                    scriptureReference = "John 3:16",
                    scriptureText = "For God so loved the world",
                    tags = "love",
                    isDraft = false,
                    applicationFollowThrough = true,
                    prayerFollowThrough = false,
                    readingPlanDay = 12
                )
            ),
            memoryVerses = listOf(
                MemoryVerseEntity(
                    id = 7,
                    reference = "Psalm 23:1",
                    text = "The Lord is my shepherd",
                    source = "manual",
                    masteryLevel = 2
                )
            ),
            preferences = BackupPreferences(
                darkTheme = true,
                bibleVersion = "ESV",
                currentStreak = 4,
                longestStreak = 9,
                planStartEpochDay = 19990
            ),
            inkFiles = mapOf(
                "42_observation.json" to """{"strokes":[],"canvasWidth":100.0,"canvasHeight":200.0}""".toByteArray()
            )
        )

        val restored = JournalBackupArchive.roundTripBytes(payload)
        assertEquals(JournalBackupArchive.SCHEMA_VERSION, restored.manifest.schemaVersion)
        assertEquals(1, restored.entries.size)
        assertEquals("John 3:16", restored.entries.first().scriptureReference)
        assertEquals(42L, restored.entries.first().id)
        assertEquals("Psalm 23:1", restored.memoryVerses.first().reference)
        assertEquals(true, restored.preferences.darkTheme)
        assertEquals("ESV", restored.preferences.bibleVersion)
        assertEquals(4, restored.preferences.currentStreak)
        assertTrue(restored.inkFiles.containsKey("42_observation.json"))
        assertEquals(
            payload.inkFiles.getValue("42_observation.json").toString(Charsets.UTF_8),
            restored.inkFiles.getValue("42_observation.json").toString(Charsets.UTF_8)
        )
    }
}
