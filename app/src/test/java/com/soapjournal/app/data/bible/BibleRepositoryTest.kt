package com.soapjournal.app.data.bible

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BibleRepositoryTest {
    private val offlineRepo = BibleRepository(isOnline = { false })

    @Test
    fun verseOfTheDayIsStableForDateOffline() = runBlocking {
        val date = LocalDate.of(2026, 8, 10)
        val a = offlineRepo.verseOfTheDay(date)
        val b = offlineRepo.verseOfTheDay(date)
        assertEquals(a.verse.reference, b.verse.reference)
        assertTrue(a.verse.text.isNotBlank())
        assertTrue(a.fromOfflineFallback)
        assertEquals(BibleVersion.KJV, a.verse.version)
    }

    @Test
    fun johnThreeSixteenExists() {
        val verse = offlineRepo.lookup("John 3:16")
        assertTrue(verse != null)
        assertTrue(verse!!.text.contains("God so loved the world"))
    }

    @Test
    fun lookupIsCaseInsensitive() {
        val verse = offlineRepo.lookup("john 3:16")
        assertTrue(verse != null)
        assertEquals("John 3:16", verse!!.reference)
    }

    @Test
    fun chapterLookupIsCaseInsensitive() = runBlocking {
        val verses = offlineRepo.chapter("john", 3, BibleVersion.KJV)
        assertTrue(verses.any { it.verse == 16 })
    }

    @Test
    fun passageTextJoinsMultipleRefs() = runBlocking {
        val text = offlineRepo.passageText(
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
    fun allListedVersionsHaveOnlineSlugs() {
        BibleVersion.entries.forEach { version ->
            assertNotNull(
                "${version.name} must have an onlineSlug so it does not silently fall back to CSB",
                version.onlineSlug
            )
            assertTrue(version.onlineAvailable)
        }
        assertEquals("ESV", BibleVersion.ESV.onlineSlug)
        assertEquals("NIV", BibleVersion.NIV.onlineSlug)
        assertEquals("NKJV", BibleVersion.NKJV.onlineSlug)
        assertEquals("NASB", BibleVersion.NASB.onlineSlug)
        assertEquals("AMP", BibleVersion.AMP.onlineSlug)
        assertEquals("MSG", BibleVersion.MSG.onlineSlug)
        assertEquals("KJV", BibleVersion.KJV.onlineSlug)
        assertEquals("NLT", BibleVersion.NLT.onlineSlug)
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

    @Test
    fun cleansBreakTagsWithoutGluingWords() {
        val cleaned = OnlineBibleClient.cleanVerseText(
            "Jesus Teaches Nicodemus<br/>Now there was a man"
        )
        assertEquals("Jesus Teaches Nicodemus Now there was a man", cleaned)
    }

    @Test
    fun marksWordsOfChristInQuotes() {
        val verses = listOf(
            BibleVerse(
                book = "John",
                chapter = 3,
                verse = 3,
                text = "Jesus replied, “Truly I tell you, unless someone is born again, he cannot see the kingdom of God.”"
            )
        )
        val marked = WordsOfChristMarker.markChapter(verses).first()
        assertTrue(marked.displaySpans().any { it.wordsOfChrist })
        assertTrue(marked.displaySpans().any { !it.wordsOfChrist })
        val christ = marked.displaySpans().filter { it.wordsOfChrist }.joinToString("") { it.text }
        assertTrue(christ.contains("Truly I tell you"))
        assertFalse(christ.contains("Jesus replied"))
    }

    @Test
    fun continuesWordsOfChristAcrossVerses() {
        val verses = listOf(
            BibleVerse(
                book = "John",
                chapter = 3,
                verse = 5,
                text = "Jesus answered, “Truly I tell you, unless someone is born of water and the Spirit,"
            ),
            BibleVerse(
                book = "John",
                chapter = 3,
                verse = 6,
                text = "Whatever is born of the flesh is flesh, and whatever is born of the Spirit is spirit.”"
            )
        )
        val marked = WordsOfChristMarker.markChapter(verses)
        assertTrue(marked[0].displaySpans().any { it.wordsOfChrist })
        assertTrue(marked[1].displaySpans().all { it.wordsOfChrist })
    }

    @Test
    fun doesNotMarkNicodemusSpeechAsChrist() {
        val verses = listOf(
            BibleVerse(
                book = "John",
                chapter = 3,
                verse = 2,
                text = "This man came to him at night and said, “Rabbi, we know that you are a teacher who has come from God.”"
            ),
            BibleVerse(
                book = "John",
                chapter = 3,
                verse = 4,
                text = "“How can anyone be born when he is old?” Nicodemus asked him. “Can he enter his mother’s womb a second time and be born?”"
            )
        )
        val marked = WordsOfChristMarker.markChapter(verses)
        assertTrue(marked[0].displaySpans().none { it.wordsOfChrist })
        assertTrue(marked[1].displaySpans().none { it.wordsOfChrist })
    }

    @Test
    fun marksPostAttributedJesusQuote() {
        val verse = BibleVerse(
            book = "John",
            chapter = 3,
            verse = 10,
            text = "“Are you a teacher of Israel and don’t know these things?” Jesus replied."
        )
        val marked = WordsOfChristMarker.markChapter(listOf(verse)).first()
        val christ = marked.displaySpans().filter { it.wordsOfChrist }.joinToString("") { it.text }
        assertTrue(christ.contains("Are you a teacher"))
        assertFalse(christ.contains("Jesus replied"))
    }

    @Test
    fun marksFullRedLetterVersesEntirely() {
        val verse = BibleVerse(
            book = "Matthew",
            chapter = 5,
            verse = 3,
            text = "“Blessed are the poor in spirit, for theirs is the kingdom of heaven."
        )
        val marked = WordsOfChristMarker.markChapter(listOf(verse)).first()
        assertTrue(marked.displaySpans().all { it.wordsOfChrist })
        assertEquals(RedLetterIndex.Kind.FULL, RedLetterIndex.kind("Matthew", 5, 3))
    }

    @Test
    fun nonGospelVersesStayBlack() {
        val verse = BibleVerse(
            book = "Romans",
            chapter = 8,
            verse = 28,
            text = "Jesus said, “All things work together for good.”"
        )
        val marked = WordsOfChristMarker.markChapter(listOf(verse)).first()
        assertTrue(marked.displaySpans().none { it.wordsOfChrist })
        assertNull(RedLetterIndex.kind("Romans", 8, 28))
    }

    @Test
    fun johnThreeSixteenIsRedLetter() {
        assertEquals(RedLetterIndex.Kind.FULL, RedLetterIndex.kind("John", 3, 16))
    }

    @Test
    fun chapterCacheAvoidsRepeatOnlineFetches() = runBlocking {
        val client = object : OnlineBibleClient() {
            var fetches = 0
            override suspend fun fetchChapter(
                book: String,
                chapter: Int,
                version: BibleVersion
            ): List<BibleVerse> {
                fetches++
                return listOf(
                    BibleVerse(
                        book = "John",
                        chapter = chapter,
                        verse = 1,
                        text = "In the beginning",
                        version = version
                    )
                )
            }
        }
        val repo = BibleRepository(onlineClient = client, isOnline = { true })
        repo.chapter("John", 3, BibleVersion.CSB)
        repo.chapter("John", 3, BibleVersion.CSB)
        assertEquals(1, client.fetches)
        repo.chapter("John", 4, BibleVersion.CSB)
        assertEquals(2, client.fetches)
    }
}
