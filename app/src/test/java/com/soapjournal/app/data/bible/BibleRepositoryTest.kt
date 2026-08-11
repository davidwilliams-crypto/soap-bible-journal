package com.soapjournal.app.data.bible

import kotlinx.coroutines.runBlocking
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
    fun chapterLookupIsCaseInsensitive() = runBlocking {
        val verses = repo.chapter("john", 3, BibleVersion.KJV)
        assertTrue(verses.any { it.verse == 16 })
    }

    @Test
    fun passageTextJoinsMultipleRefs() = runBlocking {
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

    @Test
    fun preferredDefaultIsCsb() {
        assertEquals(BibleVersion.CSB, BibleVersion.PREFERRED_DEFAULT)
        assertTrue(BibleVersion.CSB.onlineAvailable)
        assertEquals("CSB17", BibleVersion.CSB.onlineSlug)
    }

    @Test
    fun nltIsAvailableOnline() {
        assertTrue(BibleVersion.NLT.onlineAvailable)
        assertEquals("NLT", BibleVersion.NLT.onlineSlug)
        assertTrue(BibleVersion.NLT.copyrightNotice!!.contains("Tyndale"))
    }

    @Test
    fun canonIncludesFullProtestantBible() {
        assertEquals(66, BibleCanon.books.size)
        assertEquals(43, BibleCanon.bookId("John"))
        assertEquals(150, BibleCanon.find("Psalms")?.chapters)
        assertEquals(21, BibleCanon.chaptersFor("John").size)
    }

    @Test
    fun cleansRemoteVerseHtml() {
        val cleaned = OnlineBibleClient.cleanVerseText(
            "For God loved <sup>ⓜ</sup>the world in this way: <sup>[5]</sup>He gave his Son."
        )
        assertEquals("For God loved the world in this way: He gave his Son.", cleaned)
    }
}
