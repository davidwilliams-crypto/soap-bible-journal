package com.soapjournal.app.data.bible

/**
 * Marks words of Jesus for red-letter display.
 *
 * Strategy (translation-agnostic):
 * 1. Only verses in [RedLetterIndex] may be red — prevents other speakers turning red.
 * 2. FULL verses are entirely words of Christ (discourse continuations, etc.).
 * 3. PARTIAL verses color quoted speech as Christ unless clearly attributed to someone else,
 *    using attribution before or after the quote (e.g. `"…" Jesus replied`).
 */
object WordsOfChristMarker {
    private val quoteOpen = setOf('“', '«')
    private val quoteClose = setOf('”', '»')

    private val jesusAttribution = Regex(
        pattern = """(?i)\b(?:jesus|the\s+lord\s+jesus|christ|the\s+son\s+of\s+man)\b""" +
            """[\s\S]{0,100}?\b(?:said|saith|saying|answered|replied|asked|spoke|spake|told|cried|""" +
            """continued|declared|commanded|taught)\b""" +
            """|""" +
            """(?i)\b(?:said|saith|replied|answered|asked|spoke|spake)\s+jesus\b"""
    )

    private val otherAttribution = Regex(
        pattern = """(?i)\b(?:nicodemus|pilate|peter|john|james|andrew|philip|thomas|matthew|""" +
            """mary|martha|lazarus|judas|caiaphas|caiphas|herod|the\s+jews|the\s+pharisees|""" +
            """the\s+crowd|the\s+crowds|the\s+people|the\s+disciples|his\s+disciples|""" +
            """a\s+man|a\s+woman|someone|they|she|we)\b""" +
            """[\s\S]{0,100}?\b(?:said|saith|saying|answered|replied|asked|spoke|spake|told|cried)\b""" +
            """|""" +
            """(?i)\b(?:said|saith|replied|answered|asked|spoke|spake)\s+""" +
            """(?:nicodemus|pilate|peter|she|they)\b"""
    )

    fun markChapter(verses: List<BibleVerse>): List<BibleVerse> {
        if (verses.isEmpty()) return verses
        return verses.map { verse ->
            val kind = RedLetterIndex.kind(verse.book, verse.chapter, verse.verse)
            val spans = when (kind) {
                RedLetterIndex.Kind.FULL -> listOf(VerseSpan(verse.text, wordsOfChrist = true))
                RedLetterIndex.Kind.PARTIAL -> markPartialVerse(verse.text)
                null -> listOf(VerseSpan(verse.text, wordsOfChrist = false))
            }
            verse.copy(spans = spans.ifEmpty { listOf(VerseSpan(verse.text, false)) })
        }
    }

    /** Test helper for a single partial/full verse body. */
    fun markVerse(
        text: String,
        jesusSpeakingAtStart: Boolean = false
    ): Pair<List<VerseSpan>, Boolean> {
        val spans = markPartialVerse(text, jesusSpeakingAtStart)
        val stillOpen = !quotesBalanced(text) && spans.any { it.wordsOfChrist }
        return spans to stillOpen
    }

    private fun markPartialVerse(
        text: String,
        jesusSpeakingAtStart: Boolean = false
    ): List<VerseSpan> {
        if (text.isEmpty()) return listOf(VerseSpan("", false))

        data class Region(val start: Int, val end: Int, val isQuote: Boolean)

        val quoteRegions = mutableListOf<Region>()
        var i = 0
        var quoteStart = if (jesusSpeakingAtStart) 0 else -1
        var inStraight = jesusSpeakingAtStart

        while (i < text.length) {
            val ch = text[i]
            when {
                ch in quoteOpen -> {
                    if (quoteStart < 0) {
                        quoteStart = i
                        inStraight = false
                    }
                }
                ch in quoteClose -> {
                    if (quoteStart >= 0) {
                        quoteRegions += Region(quoteStart, i + 1, isQuote = true)
                        quoteStart = -1
                        inStraight = false
                    }
                }
                ch == '"' -> {
                    if (!inStraight && quoteStart < 0) {
                        quoteStart = i
                        inStraight = true
                    } else if (inStraight && quoteStart >= 0) {
                        quoteRegions += Region(quoteStart, i + 1, isQuote = true)
                        quoteStart = -1
                        inStraight = false
                    }
                }
            }
            i++
        }
        if (quoteStart >= 0) {
            quoteRegions += Region(quoteStart, text.length, isQuote = true)
        }

        val filled = mutableListOf<Region>()
        var cursor = 0
        for (region in quoteRegions) {
            if (region.start > cursor) {
                filled += Region(cursor, region.start, isQuote = false)
            }
            filled += region
            cursor = region.end
        }
        if (cursor < text.length) {
            filled += Region(cursor, text.length, isQuote = false)
        }

        if (filled.none { it.isQuote }) {
            // No quotes in a known partial WoC verse: color speech after attribution, else all.
            val match = jesusAttribution.find(text)
            return if (match != null && match.range.last + 1 < text.lastIndex) {
                listOf(
                    VerseSpan(text.substring(0, match.range.last + 1), false),
                    VerseSpan(text.substring(match.range.last + 1).trimStart().let { rest ->
                        // Keep original spacing from cut point
                        text.substring(match.range.last + 1)
                    }, true)
                ).filter { it.text.isNotEmpty() }.collapseAdjacent()
            } else {
                listOf(VerseSpan(text, true))
            }
        }

        return filled.map { region ->
            val chunk = text.substring(region.start, region.end)
            if (!region.isQuote) {
                VerseSpan(chunk, false)
            } else {
                VerseSpan(
                    chunk,
                    isJesusQuote(text, region.start, region.end, jesusSpeakingAtStart)
                )
            }
        }.collapseAdjacent()
    }

    private fun isJesusQuote(
        text: String,
        start: Int,
        end: Int,
        jesusSpeakingAtStart: Boolean
    ): Boolean {
        if (jesusSpeakingAtStart && start == 0) return true

        val before = text.substring(0, start)
        val after = text.substring(end).take(100)

        if (nearestSpeaker(before) == Speaker.OTHER) return false
        if (nearestSpeaker(before) == Speaker.JESUS) return true

        when (nearestSpeaker(after, fromStart = true)) {
            Speaker.OTHER -> return false
            Speaker.JESUS -> return true
            Speaker.UNKNOWN -> Unit
        }

        // Known red-letter partial: default quotes to Christ when unattributed.
        return true
    }

    private enum class Speaker { JESUS, OTHER, UNKNOWN }

    private fun nearestSpeaker(window: String, fromStart: Boolean = false): Speaker {
        if (window.isBlank()) return Speaker.UNKNOWN
        val jesusMatches = jesusAttribution.findAll(window).map { it.range }.toList()
        val otherMatches = otherAttribution.findAll(window).map { it.range }.toList()
        if (jesusMatches.isEmpty() && otherMatches.isEmpty()) return Speaker.UNKNOWN

        return if (fromStart) {
            val j = jesusMatches.minOfOrNull { it.first } ?: Int.MAX_VALUE
            val o = otherMatches.minOfOrNull { it.first } ?: Int.MAX_VALUE
            when {
                j == Int.MAX_VALUE && o == Int.MAX_VALUE -> Speaker.UNKNOWN
                o < j -> Speaker.OTHER
                else -> Speaker.JESUS
            }
        } else {
            val j = jesusMatches.maxOfOrNull { it.last } ?: -1
            val o = otherMatches.maxOfOrNull { it.last } ?: -1
            when {
                j < 0 && o < 0 -> Speaker.UNKNOWN
                o > j -> Speaker.OTHER
                else -> Speaker.JESUS
            }
        }
    }

    private fun List<VerseSpan>.collapseAdjacent(): List<VerseSpan> {
        if (isEmpty()) return this
        val out = mutableListOf<VerseSpan>()
        for (span in this) {
            val last = out.lastOrNull()
            if (last != null && last.wordsOfChrist == span.wordsOfChrist) {
                out[out.lastIndex] = last.copy(text = last.text + span.text)
            } else if (span.text.isNotEmpty()) {
                out += span
            }
        }
        return out
    }

    private fun quotesBalanced(text: String): Boolean {
        var curly = 0
        var straight = false
        for (ch in text) {
            when {
                ch in quoteOpen -> curly++
                ch in quoteClose -> curly = (curly - 1).coerceAtLeast(0)
                ch == '"' -> straight = !straight
            }
        }
        return curly == 0 && !straight
    }
}
