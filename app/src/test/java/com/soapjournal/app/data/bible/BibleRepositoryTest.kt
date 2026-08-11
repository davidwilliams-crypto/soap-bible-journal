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
}
