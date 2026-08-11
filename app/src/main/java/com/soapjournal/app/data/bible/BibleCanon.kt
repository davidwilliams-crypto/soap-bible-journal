package com.soapjournal.app.data.bible

/**
 * Protestant canon metadata used for Bible browsing and online CSB chapter fetches.
 * Book IDs match common API numbering (Genesis=1 … Revelation=66).
 */
object BibleCanon {
    data class Book(val id: Int, val name: String, val chapters: Int)

    val books: List<Book> = listOf(
        Book(1, "Genesis", 50), Book(2, "Exodus", 40), Book(3, "Leviticus", 27),
        Book(4, "Numbers", 36), Book(5, "Deuteronomy", 34), Book(6, "Joshua", 24),
        Book(7, "Judges", 21), Book(8, "Ruth", 4), Book(9, "1 Samuel", 31),
        Book(10, "2 Samuel", 24), Book(11, "1 Kings", 22), Book(12, "2 Kings", 25),
        Book(13, "1 Chronicles", 29), Book(14, "2 Chronicles", 36), Book(15, "Ezra", 10),
        Book(16, "Nehemiah", 13), Book(17, "Esther", 10), Book(18, "Job", 42),
        Book(19, "Psalm", 150), Book(20, "Proverbs", 31), Book(21, "Ecclesiastes", 12),
        Book(22, "Song of Solomon", 8), Book(23, "Isaiah", 66), Book(24, "Jeremiah", 52),
        Book(25, "Lamentations", 5), Book(26, "Ezekiel", 48), Book(27, "Daniel", 12),
        Book(28, "Hosea", 14), Book(29, "Joel", 3), Book(30, "Amos", 9),
        Book(31, "Obadiah", 1), Book(32, "Jonah", 4), Book(33, "Micah", 7),
        Book(34, "Nahum", 3), Book(35, "Habakkuk", 3), Book(36, "Zephaniah", 3),
        Book(37, "Haggai", 2), Book(38, "Zechariah", 14), Book(39, "Malachi", 4),
        Book(40, "Matthew", 28), Book(41, "Mark", 16), Book(42, "Luke", 24),
        Book(43, "John", 21), Book(44, "Acts", 28), Book(45, "Romans", 16),
        Book(46, "1 Corinthians", 16), Book(47, "2 Corinthians", 13), Book(48, "Galatians", 6),
        Book(49, "Ephesians", 6), Book(50, "Philippians", 4), Book(51, "Colossians", 4),
        Book(52, "1 Thessalonians", 5), Book(53, "2 Thessalonians", 3), Book(54, "1 Timothy", 6),
        Book(55, "2 Timothy", 4), Book(56, "Titus", 3), Book(57, "Philemon", 1),
        Book(58, "Hebrews", 13), Book(59, "James", 5), Book(60, "1 Peter", 5),
        Book(61, "2 Peter", 3), Book(62, "1 John", 5), Book(63, "2 John", 1),
        Book(64, "3 John", 1), Book(65, "Jude", 1), Book(66, "Revelation", 22)
    )

    private val byName: Map<String, Book> =
        books.associateBy { it.name.lowercase() } + mapOf(
            "psalms" to books.first { it.name == "Psalm" },
            "song of songs" to books.first { it.name == "Song of Solomon" },
            "canticles" to books.first { it.name == "Song of Solomon" }
        )

    fun find(book: String): Book? = byName[book.trim().lowercase()]

    fun bookId(book: String): Int? = find(book)?.id

    fun chaptersFor(book: String): List<Int> {
        val meta = find(book) ?: return emptyList()
        return (1..meta.chapters).toList()
    }
}
