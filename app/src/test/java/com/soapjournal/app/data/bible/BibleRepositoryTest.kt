package com.soapjournal.app.data.bible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BibleRepositoryTest {
    private val repo = BibleRepository()

    @Test
    fun verseOfTheDayIsStableForDate() {
        val date = LocalDate.of(2026, 8, 10)
        val a = repo.verseOfTheDay(date)
        val b = repo.verseOfTheDay(date)
        assertEquals(a.verse.reference, b.verse.reference)
        assertTrue(a.verse.text.isNotBlank())
    }

    @Test
    fun johnThreeSixteenExists() {
        val verse = repo.lookup("John 3:16")
        assertTrue(verse != null)
        assertTrue(verse!!.text.contains("God so loved the world"))
    }

    @Test
    fun lookupIsCaseInsensitive() {
        val verse = repo.lookup("john 3:16")
        assertTrue(verse != null)
        assertEquals("John 3:16", verse!!.reference)
    }

    @Test
    fun chapterLookupIsCaseInsensitive() {
        val verses = repo.chapter("john", 3, BibleVersion.KJV)
        assertTrue(verses.any { it.verse == 16 })
    }

    @Test
    fun passageTextJoinsMultipleRefs() {
        val text = repo.passageText(
            listOf(
                PassageRef("John", 3, 3, startVerse = 16, endVerse = 16),
                PassageRef("Psalm", 23, 23, startVerse = 1, endVerse = 1)
            ),
            BibleVersion.KJV
        )
        assertTrue(text.contains("God so loved the world"))
        assertTrue(text.contains("my shepherd"))
    }
}
