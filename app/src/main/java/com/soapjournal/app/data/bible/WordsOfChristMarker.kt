package com.soapjournal.app.data.bible

/**
 * Marks words of Jesus (red-letter spans) by tracking dialogue quotes in Gospel
 * (and Revelation) narrative, including speech that continues across verses.
 */
object WordsOfChristMarker {
    private val redLetterBooks = setOf(
        "matthew", "mark", "luke", "john", "revelation"
    )

    private val jesusAttribution = Regex(
        pattern = """(?i)\b(?:jesus|the\s+lord|the\s+son\s+of\s+man)\b[\s\S]{0,100}?\b(?:said|saith|saying|answered|replied|asked|spoke|spake|told|cried|continued|declared|commanded)\b"""
    )

    fun markChapter(verses: List<BibleVerse>): List<BibleVerse> {
        if (verses.isEmpty()) return verses
        val book = verses.first().book
        if (book.lowercase() !in redLetterBooks) {
            return verses.map { it.copy(spans = listOf(VerseSpan(it.text, false))) }
        }

        var jesusSpeaking = false
        return verses.map { verse ->
            val (spans, stillSpeaking) = markVerse(verse.text, jesusSpeaking)
            jesusSpeaking = stillSpeaking
            verse.copy(spans = spans.ifEmpty { listOf(VerseSpan(verse.text, false)) })
        }
    }

    fun markVerse(text: String, jesusSpeakingAtStart: Boolean = false): Pair<List<VerseSpan>, Boolean> {
        if (text.isEmpty()) return listOf(VerseSpan("", false)) to jesusSpeakingAtStart

        val spans = mutableListOf<VerseSpan>()
        val buffer = StringBuilder()
        var jesusSpeaking = jesusSpeakingAtStart
        // Continuation verses keep Jesus' speech open even without a fresh opening quote.
        var inQuote = jesusSpeakingAtStart
        var currentIsChrist = inQuote

        fun flush() {
            if (buffer.isEmpty()) return
            val chunk = buffer.toString()
            buffer.clear()
            val last = spans.lastOrNull()
            if (last != null && last.wordsOfChrist == currentIsChrist) {
                spans[spans.lastIndex] = last.copy(text = last.text + chunk)
            } else {
                spans += VerseSpan(chunk, currentIsChrist)
            }
        }

        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when (ch) {
                '“', '«' -> {
                    flush()
                    val attributed = jesusSpeaking || hasJesusAttributionBefore(text, i)
                    jesusSpeaking = attributed
                    inQuote = true
                    currentIsChrist = attributed
                    buffer.append(ch)
                }
                '”', '»' -> {
                    buffer.append(ch)
                    flush()
                    inQuote = false
                    currentIsChrist = false
                    jesusSpeaking = false
                }
                '"' -> {
                    if (!inQuote) {
                        flush()
                        val attributed = jesusSpeaking || hasJesusAttributionBefore(text, i)
                        jesusSpeaking = attributed
                        inQuote = true
                        currentIsChrist = attributed
                        buffer.append(ch)
                    } else {
                        buffer.append(ch)
                        flush()
                        inQuote = false
                        currentIsChrist = false
                        jesusSpeaking = false
                    }
                }
                else -> {
                    currentIsChrist = inQuote && jesusSpeaking
                    buffer.append(ch)
                }
            }
            i++
        }
        flush()
        return spans to (inQuote && jesusSpeaking)
    }

    private fun hasJesusAttributionBefore(text: String, quoteIndex: Int): Boolean {
        val prefix = text.substring(0, quoteIndex.coerceIn(0, text.length))
        return jesusAttribution.containsMatchIn(prefix)
    }
}
