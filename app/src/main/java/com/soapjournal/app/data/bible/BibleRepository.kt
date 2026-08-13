package com.soapjournal.app.data.bible

import java.time.LocalDate

class BibleRepository(
    private val onlineClient: OnlineBibleClient = OnlineBibleClient(),
    private val isOnline: () -> Boolean = { false }
) {
    private val byBookChapter: Map<Pair<String, Int>, List<BibleVerse>> =
        KjvCorpus.verses.groupBy { it.book.lowercase() to it.chapter }
            .mapValues { (_, list) -> list.sortedBy { it.verse } }

    private val byReference: Map<String, BibleVerse> =
        KjvCorpus.verses.associateBy { normalizeRef(it.reference) }

    private val chapterCache = object : LinkedHashMap<String, List<BibleVerse>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<BibleVerse>>?): Boolean =
            size > 24
    }
    private val cacheLock = Any()

    fun books(): List<String> = BibleCanon.books.map { it.name }

    fun chaptersFor(book: String): List<Int> = BibleCanon.chaptersFor(book)

    /** Offline-only KJV samples. Never used while the device is online. */
    fun chapterOffline(book: String, chapter: Int): List<BibleVerse> =
        WordsOfChristMarker.markChapter(
            byBookChapter[book.lowercase() to chapter].orEmpty()
        )

    /**
     * Loads a chapter.
     * - Online: uses CSB/NLT (or requested online version). Never returns KJV while online.
     * - Offline: public-domain KJV samples only.
     */
    suspend fun chapter(book: String, chapter: Int, version: BibleVersion): List<BibleVerse> {
        val online = isOnline()
        val resolved = if (online) resolveOnlineVersion(version) else BibleVersion.KJV
        val key = cacheKey(book, chapter, resolved, online)
        synchronized(cacheLock) { chapterCache[key] }?.let { return it }

        val loaded = if (online) {
            val remote = runCatching {
                onlineClient.fetchChapter(book, chapter, resolved)
            }.getOrDefault(emptyList())
            WordsOfChristMarker.markChapter(remote)
        } else {
            chapterOffline(book, chapter)
        }
        if (loaded.isNotEmpty()) {
            synchronized(cacheLock) { chapterCache[key] = loaded }
        }
        return loaded
    }

    fun lookup(reference: String): BibleVerse? = byReference[normalizeRef(reference)]?.let {
        WordsOfChristMarker.markChapter(listOf(it)).first()
    }

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

    /**
     * Verse of the Day uses the preferred online version (CSB by default) when connected.
     * KJV is used only when the phone is offline.
     */
    suspend fun verseOfTheDay(
        date: LocalDate = LocalDate.now(),
        preferredVersion: BibleVersion = BibleVersion.PREFERRED_DEFAULT
    ): VerseOfTheDay {
        val ref = votdRefFor(date)
        if (isOnline()) {
            val onlineVersion = resolveOnlineVersion(preferredVersion)
            val onlineVerse = chapter(ref.book, ref.chapter, onlineVersion)
                .firstOrNull { it.verse == ref.verse }
            if (onlineVerse != null) {
                return VerseOfTheDay(onlineVerse, date.toEpochDay(), fromOfflineFallback = false)
            }
            return VerseOfTheDay(
                verse = BibleVerse(
                    book = ref.book,
                    chapter = ref.chapter,
                    verse = ref.verse,
                    text = "Unable to load ${onlineVersion.displayName} right now. Tap Try again when connected.",
                    version = onlineVersion
                ),
                dateEpochDay = date.toEpochDay(),
                fromOfflineFallback = false
            )
        }

        val offline = lookup(ref.reference)
            ?: BibleVerse(ref.book, ref.chapter, ref.verse, "Offline KJV text unavailable for this verse.", BibleVersion.KJV)
        return VerseOfTheDay(offline, date.toEpochDay(), fromOfflineFallback = true)
    }

    fun votdRefFor(date: LocalDate = LocalDate.now()): VotdRef {
        val index = (date.dayOfYear - 1).mod(KjvCorpus.votdRefs.size)
        return KjvCorpus.votdRefs[index]
    }

    private fun resolveOnlineVersion(version: BibleVersion): BibleVersion =
        when {
            version.onlineAvailable -> version
            else -> BibleVersion.CSB
        }

    private fun cacheKey(
        book: String,
        chapter: Int,
        version: BibleVersion,
        online: Boolean
    ): String = "${book.lowercase()}|$chapter|${version.name}|$online"

    private fun normalizeRef(ref: String): String =
        ref.trim()
            .lowercase()
            .replace('–', '-')
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
}
