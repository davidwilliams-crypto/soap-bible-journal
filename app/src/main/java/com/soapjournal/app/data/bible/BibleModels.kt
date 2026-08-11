package com.soapjournal.app.data.bible

enum class BibleVersion(val displayName: String, val offlineAvailable: Boolean) {
    ESV("ESV", false),
    NIV("NIV", false),
    NLT("NLT", false),
    MSG("MSG", false),
    NASB("NASB", false),
    AMP("AMP", false),
    KJV("KJV", true),
    NKJV("NKJV", false);

    companion object {
        fun fromName(name: String): BibleVersion =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: KJV
    }
}

data class BibleVerse(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val version: BibleVersion = BibleVersion.KJV
) {
    val reference: String get() = "$book $chapter:$verse"
}

data class PassageRef(
    val book: String,
    val startChapter: Int,
    val endChapter: Int,
    val startVerse: Int? = null,
    val endVerse: Int? = null
) {
    val display: String
        get() = when {
            startChapter == endChapter && startVerse != null && endVerse != null ->
                "$book $startChapter:$startVerse–$endVerse"
            startChapter == endChapter && startVerse != null ->
                "$book $startChapter:$startVerse"
            startChapter == endChapter ->
                "$book $startChapter"
            else ->
                "$book $startChapter–$endChapter"
        }
}

data class VerseOfTheDay(
    val verse: BibleVerse,
    val dateEpochDay: Long
)
