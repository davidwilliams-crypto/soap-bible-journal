package com.soapjournal.app.data.bible

import java.time.LocalDate

class BibleRepository(
    private val onlineClient: OnlineBibleClient = OnlineBibleClient()
) {
    private val byBookChapter: Map<Pair<String, Int>, List<BibleVerse>> =
        KjvCorpus.verses.groupBy { it.book.lowercase() to it.chapter }
            .mapValues { (_, list) -> list.sortedBy { it.verse } }

    private val byReference: Map<String, BibleVerse> =
        KjvCorpus.verses.associateBy { normalizeRef(it.reference) }

    fun books(): List<String> = BibleCanon.books.map { it.name }

    fun chaptersFor(book: String): List<Int> = BibleCanon.chaptersFor(book)

    /** Offline / synchronous chapter text (public-domain KJV samples). */
    fun chapterOffline(book: String, chapter: Int): List<BibleVerse> =
        byBookChapter[book.lowercase() to chapter].orEmpty()

    /**
     * Loads a chapter for [version]. Online versions (CSB) are fetched remotely when
     * possible; otherwise falls back to offline KJV samples.
     */
    suspend fun chapter(book: String, chapter: Int, version: BibleVersion): List<BibleVerse> {
        if (version.onlineAvailable) {
            val online = runCatching {
                onlineClient.fetchChapter(book, chapter, version)
            }.getOrDefault(emptyList())
            if (online.isNotEmpty()) return online
        }
        return chapterOffline(book, chapter)
    }

    fun lookup(reference: String): BibleVerse? = byReference[normalizeRef(reference)]

    suspend fun passageText(refs: List<PassageRef>, version: BibleVersion): String =
        refs.map { passageText(it, version) }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")

    suspend fun passageText(ref: PassageRef, version: BibleVersion): String {
        val chapters = (ref.startChapter..ref.endChapter).flatMap { chapter(ref.book, it, version) }
        val filtered = when {
            ref.startVerse != null && ref.endVerse != null && ref.startChapter == ref.endChapter ->
                chapters.filter { it.verse in ref.startVerse..ref.endVerse }
            ref.startVerse != null && ref.startChapter == ref.endChapter ->
                chapters.filter { it.verse >= ref.startVerse }
            else -> chapters
        }
        return if (filtered.isEmpty()) {
            ""
        } else {
            filtered.joinToString("\n\n") { "${it.verse} ${it.text}" }
        }
    }

    fun verseOfTheDay(date: LocalDate = LocalDate.now()): VerseOfTheDay {
        val pool = KjvCorpus.votdPool
        val index = (date.dayOfYear - 1).mod(pool.size)
        return VerseOfTheDay(pool[index], date.toEpochDay())
    }

    private fun normalizeRef(ref: String): String =
        ref.trim()
            .lowercase()
            .replace('–', '-')
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
}
