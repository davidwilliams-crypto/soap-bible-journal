package com.soapjournal.app.data.bible

enum class BibleVersion(
    val displayName: String,
    val offlineAvailable: Boolean,
    /** Remote API slug when this version can be fetched online. */
    val onlineSlug: String? = null,
    val copyrightNotice: String? = null
) {
    CSB(
        displayName = "CSB",
        offlineAvailable = false,
        onlineSlug = "CSB17",
        copyrightNotice = "Scripture quotations marked CSB have been taken from the Christian Standard Bible®, Copyright © 2017 by Holman Bible Publishers. Used by permission. Christian Standard Bible® and CSB® are federally registered trademarks of Holman Bible Publishers."
    ),
    ESV("ESV", false),
    NIV("NIV", false),
    NLT(
        displayName = "NLT",
        offlineAvailable = false,
        onlineSlug = "NLT",
        copyrightNotice = "Scripture quotations marked NLT are taken from the Holy Bible, New Living Translation, copyright © 1996, 2004, 2015 by Tyndale House Foundation. Used by permission of Tyndale House Publishers, Inc., Carol Stream, Illinois 60188. All rights reserved."
    ),
    MSG("MSG", false),
    NASB("NASB", false),
    AMP("AMP", false),
    KJV("KJV", true),
    NKJV("NKJV", false);

    val onlineAvailable: Boolean get() = onlineSlug != null

    companion object {
        val PREFERRED_DEFAULT: BibleVersion = CSB

        fun fromName(name: String): BibleVersion =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PREFERRED_DEFAULT
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
