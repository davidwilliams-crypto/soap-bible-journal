package com.soapjournal.app.data.ink

import android.content.Context
import com.google.gson.Gson
import com.soapjournal.app.data.SoapSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class InkStore(context: Context) {
    private val root = File(context.filesDir, "ink").also { it.mkdirs() }
    private val gson = Gson()

    private fun fileFor(entryId: Long, section: SoapSection): File =
        File(root, "${entryId}_${section.key}.json")

    suspend fun load(entryId: Long, section: SoapSection): InkDocument = withContext(Dispatchers.IO) {
        val file = fileFor(entryId, section)
        if (!file.exists()) return@withContext InkDocument()
        runCatching {
            gson.fromJson(file.readText(), InkDocument::class.java) ?: InkDocument()
        }.getOrElse { InkDocument() }
    }

    suspend fun save(entryId: Long, section: SoapSection, document: InkDocument) =
        withContext(Dispatchers.IO) {
            val file = fileFor(entryId, section)
            file.writeText(gson.toJson(document))
        }

    suspend fun deleteAllForEntry(entryId: Long) = withContext(Dispatchers.IO) {
        SoapSection.entries.forEach { section ->
            fileFor(entryId, section).delete()
        }
    }
}
