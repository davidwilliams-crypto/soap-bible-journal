package com.soapjournal.app.data.bible

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches online Bible chapters via bolls.life (CSB, ESV, NIV, NLT, MSG, NASB, AMP, KJV, NKJV).
 * Offline KJV samples are handled by [BibleRepository] when the device has no network.
 */
open class OnlineBibleClient(
    private val gson: Gson = Gson(),
    private val baseUrl: String = "https://bolls.life"
) {
    open suspend fun fetchChapter(
        book: String,
        chapter: Int,
        version: BibleVersion
    ): List<BibleVerse> = withContext(Dispatchers.IO) {
        val slug = version.onlineSlug ?: return@withContext emptyList()
        val bookId = BibleCanon.bookId(book) ?: return@withContext emptyList()
        val canonicalName = BibleCanon.find(book)?.name ?: book
        val url = URL("$baseUrl/get-text/$slug/$bookId/$chapter/")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext emptyList()
            ensureActive()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            ensureActive()
            val type = object : TypeToken<List<RemoteVerse>>() {}.type
            val remote: List<RemoteVerse> = gson.fromJson(body, type) ?: emptyList()
            remote.mapNotNull { item ->
                val verse = item.verse ?: return@mapNotNull null
                val text = cleanVerseText(item.text.orEmpty())
                if (text.isBlank()) return@mapNotNull null
                BibleVerse(
                    book = canonicalName,
                    chapter = chapter,
                    verse = verse,
                    text = text,
                    version = version
                )
            }.sortedBy { it.verse }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val superscript = Regex("(?is)<sup\\b[^>]*>.*?</sup>")
        private val breakTag = Regex("(?is)<br\\s*/?>")
        private val strongs = Regex("(?is)</?S>")
        private val htmlTag = Regex("<[^>]+>")
        private val whitespace = Regex("\\s+")

        fun cleanVerseText(raw: String): String =
            raw.replace(superscript, "")
                .replace(breakTag, " ")
                .replace(strongs, "")
                .replace(htmlTag, "")
                .replace('\u00a0', ' ')
                .replace(whitespace, " ")
                .trim()
    }

    private data class RemoteVerse(
        @SerializedName("verse") val verse: Int?,
        @SerializedName("text") val text: String?
    )
}
