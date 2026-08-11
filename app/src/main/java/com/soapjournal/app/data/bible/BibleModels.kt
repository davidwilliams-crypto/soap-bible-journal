package com.soapjournal.app.data.bible

enum class BibleVersion(
    val displayName: String,
    val offlineAvailable: Boolean,
    /** Remote API slug when this version can be fetched online (bolls.life). */
    val onlineSlug: String? = null,
    val copyrightNotice: String? = null
) {
    CSB(
        displayName = "CSB",
        offlineAvailable = false,
        onlineSlug = "CSB17",
        copyrightNotice = "Scripture quotations marked CSB have been taken from the Christian Standard Bible®, Copyright © 2017 by Holman Bible Publishers. Used by permission. Christian Standard Bible® and CSB® are federally registered trademarks of Holman Bible Publishers."
    ),
    ESV(
        displayName = "ESV",
        offlineAvailable = false,
        onlineSlug = "ESV",
        copyrightNotice = "Scripture quotations are from the ESV® Bible (The Holy Bible, English Standard Version®), © 2001 by Crossway, a publishing ministry of Good News Publishers. Used by permission. All rights reserved."
    ),
    NIV(
        displayName = "NIV",
        offlineAvailable = false,
        onlineSlug = "NIV",
        copyrightNotice = "Scripture quotations taken from The Holy Bible, New International Version®, NIV®. Copyright © 1973, 1978, 1984, 2011 by Biblica, Inc.® Used by permission. All rights reserved worldwide."
    ),
    NLT(
        displayName = "NLT",
        offlineAvailable = false,
        onlineSlug = "NLT",
        copyrightNotice = "Scripture quotations marked NLT are taken from the Holy Bible, New Living Translation, copyright © 1996, 2004, 2015 by Tyndale House Foundation. Used by permission of Tyndale House Publishers, Inc., Carol Stream, Illinois 60188. All rights reserved."
    ),
    MSG(
        displayName = "MSG",
        offlineAvailable = false,
        onlineSlug = "MSG",
        copyrightNotice = "Scripture taken from THE MESSAGE. Copyright © 1993, 2002, 2018 by Eugene H. Peterson. Used by permission of NavPress. All rights reserved. Represented by Tyndale House Publishers, Inc."
    ),
    NASB(
        displayName = "NASB",
        offlineAvailable = false,
        onlineSlug = "NASB",
        copyrightNotice = "Scripture quotations taken from the New American Standard Bible® (NASB). Copyright © 1960, 1971, 1977, 1995, 2020 by The Lockman Foundation. Used by permission. All rights reserved."
    ),
    AMP(
        displayName = "AMP",
        offlineAvailable = false,
        onlineSlug = "AMP",
        copyrightNotice = "Scripture quotations taken from the Amplified® Bible, Copyright © 2015 by The Lockman Foundation. Used by permission."
    ),
    KJV(
        displayName = "KJV",
        offlineAvailable = true,
        onlineSlug = "KJV",
        copyrightNotice = "King James Version (public domain)."
    ),
    NKJV(
        displayName = "NKJV",
        offlineAvailable = false,
        onlineSlug = "NKJV",
        copyrightNotice = "Scripture taken from the New King James Version®. Copyright © 1982 by Thomas Nelson. Used by permission. All rights reserved."
    );

    val onlineAvailable: Boolean get() = onlineSlug != null

    companion object {
        val PREFERRED_DEFAULT: BibleVersion = CSB

        fun fromName(name: String): BibleVersion =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PREFERRED_DEFAULT
    }
}

data class VerseSpan(
    val text: String,
    val wordsOfChrist: Boolean = false
)

data class BibleVerse(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val version: BibleVersion = BibleVersion.KJV,
    val spans: List<VerseSpan> = listOf(VerseSpan(text, false))
) {
    val reference: String get() = "$book $chapter:$verse"

    fun displaySpans(): List<VerseSpan> =
        spans.ifEmpty { listOf(VerseSpan(text, false)) }
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
    val dateEpochDay: Long,
    val fromOfflineFallback: Boolean = false
)

data class VotdRef(
    val book: String,
    val chapter: Int,
    val verse: Int
) {
    val reference: String get() = "$book $chapter:$verse"
}
