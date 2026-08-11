package com.soapjournal.app.data.plan

import com.soapjournal.app.data.bible.PassageRef
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Two-year whole-Bible reading plan (~1–2 chapters/day across 730 days).
 */
object ReadingPlan {
    const val TOTAL_DAYS = 730

    data class DayReading(
        val dayIndex: Int,
        val passages: List<PassageRef>,
        val label: String
    ) {
        /** Primary passage for callers that only need a single ref (first book slice). */
        val passage: PassageRef get() = passages.first()
    }

    private val books: List<Pair<String, Int>> = listOf(
        "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36, "Deuteronomy" to 34,
        "Joshua" to 24, "Judges" to 21, "Ruth" to 4, "1 Samuel" to 31, "2 Samuel" to 24,
        "1 Kings" to 22, "2 Kings" to 25, "1 Chronicles" to 29, "2 Chronicles" to 36,
        "Ezra" to 10, "Nehemiah" to 13, "Esther" to 10, "Job" to 42, "Psalm" to 150,
        "Proverbs" to 31, "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66,
        "Jeremiah" to 52, "Lamentations" to 5, "Ezekiel" to 48, "Daniel" to 12,
        "Hosea" to 14, "Joel" to 3, "Amos" to 9, "Obadiah" to 1, "Jonah" to 4,
        "Micah" to 7, "Nahum" to 3, "Habakkuk" to 3, "Zephaniah" to 3, "Haggai" to 2,
        "Zechariah" to 14, "Malachi" to 4,
        "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21, "Acts" to 28,
        "Romans" to 16, "1 Corinthians" to 16, "2 Corinthians" to 13, "Galatians" to 6,
        "Ephesians" to 6, "Philippians" to 4, "Colossians" to 4, "1 Thessalonians" to 5,
        "2 Thessalonians" to 3, "1 Timothy" to 6, "2 Timothy" to 4, "Titus" to 3,
        "Philemon" to 1, "Hebrews" to 13, "James" to 5, "1 Peter" to 5, "2 Peter" to 3,
        "1 John" to 5, "2 John" to 1, "3 John" to 1, "Jude" to 1, "Revelation" to 22
    )

    private val allChapters: List<PassageRef> by lazy {
        books.flatMap { (book, count) ->
            (1..count).map { chapter -> PassageRef(book, chapter, chapter) }
        }
    }

    private val schedule: List<DayReading> by lazy {
        val totalChapters = allChapters.size
        (0 until TOTAL_DAYS).map { day ->
            val start = (day * totalChapters) / TOTAL_DAYS
            val endExclusive = (((day + 1) * totalChapters) / TOTAL_DAYS).coerceAtLeast(start + 1)
            val slice = allChapters.subList(start, endExclusive.coerceAtMost(totalChapters))
            val first = slice.first()
            val last = slice.last()
            val passages = groupPassages(slice)
            val label = if (first.book == last.book) {
                passages.first().display
            } else {
                "${first.book} ${first.startChapter} – ${last.book} ${last.endChapter}"
            }
            DayReading(dayIndex = day, passages = passages, label = label)
        }
    }

    private fun groupPassages(slice: List<PassageRef>): List<PassageRef> {
        if (slice.isEmpty()) return emptyList()
        val grouped = mutableListOf<PassageRef>()
        var book = slice.first().book
        var startChapter = slice.first().startChapter
        var endChapter = slice.first().endChapter
        for (i in 1 until slice.size) {
            val chapter = slice[i]
            if (chapter.book == book) {
                endChapter = chapter.endChapter
            } else {
                grouped += PassageRef(book, startChapter, endChapter)
                book = chapter.book
                startChapter = chapter.startChapter
                endChapter = chapter.endChapter
            }
        }
        grouped += PassageRef(book, startChapter, endChapter)
        return grouped
    }

    fun dayFor(planStart: LocalDate, date: LocalDate = LocalDate.now()): DayReading {
        val raw = ChronoUnit.DAYS.between(planStart, date).toInt()
        val index = raw.coerceIn(0, TOTAL_DAYS - 1)
        return schedule[index]
    }

    fun day(dayIndex: Int): DayReading = schedule[dayIndex.coerceIn(0, TOTAL_DAYS - 1)]

    fun progressFraction(planStart: LocalDate, date: LocalDate = LocalDate.now()): Float {
        val raw = ChronoUnit.DAYS.between(planStart, date).toInt().coerceAtLeast(0)
        return (raw + 1).toFloat() / TOTAL_DAYS
    }
}
