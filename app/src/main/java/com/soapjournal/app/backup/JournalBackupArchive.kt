package com.soapjournal.app.backup

import com.soapjournal.app.data.SoapEntryEntity
import com.soapjournal.app.data.memory.MemoryVerseEntity
import com.soapjournal.app.data.prefs.BackupPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupManifest(
    val schemaVersion: Int = 1,
    val appVersionCode: Int,
    val appVersionName: String,
    val createdAtEpochMs: Long,
    val entryCount: Int,
    val memoryCount: Int,
    val inkFileCount: Int
)

data class JournalBackupPayload(
    val manifest: BackupManifest,
    val entries: List<SoapEntryEntity>,
    val memoryVerses: List<MemoryVerseEntity>,
    val preferences: BackupPreferences,
    val inkFiles: Map<String, ByteArray>
)

object JournalBackupArchive {
    const val SCHEMA_VERSION = 1
    const val MANIFEST = "manifest.json"
    const val ENTRIES = "entries.json"
    const val MEMORY = "memory.json"
    const val PREFERENCES = "preferences.json"
    const val INK_PREFIX = "ink/"

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    fun write(
        output: OutputStream,
        payload: JournalBackupPayload
    ) {
        ZipOutputStream(output).use { zip ->
            fun putJson(name: String, value: Any) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(gson.toJson(value).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            putJson(MANIFEST, payload.manifest)
            putJson(ENTRIES, payload.entries)
            putJson(MEMORY, payload.memoryVerses)
            putJson(PREFERENCES, payload.preferences)
            payload.inkFiles.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry("$INK_PREFIX$name"))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    fun read(input: InputStream): JournalBackupPayload {
        var manifest: BackupManifest? = null
        var entries: List<SoapEntryEntity> = emptyList()
        var memory: List<MemoryVerseEntity> = emptyList()
        var preferences: BackupPreferences? = null
        val ink = linkedMapOf<String, ByteArray>()

        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                val bytes = zip.readBytes()
                when {
                    entry.name == MANIFEST ->
                        manifest = gson.fromJson(bytes.toString(Charsets.UTF_8), BackupManifest::class.java)
                    entry.name == ENTRIES ->
                        entries = gson.fromJson(
                            bytes.toString(Charsets.UTF_8),
                            object : TypeToken<List<SoapEntryEntity>>() {}.type
                        ) ?: emptyList()
                    entry.name == MEMORY ->
                        memory = gson.fromJson(
                            bytes.toString(Charsets.UTF_8),
                            object : TypeToken<List<MemoryVerseEntity>>() {}.type
                        ) ?: emptyList()
                    entry.name == PREFERENCES ->
                        preferences = gson.fromJson(
                            bytes.toString(Charsets.UTF_8),
                            BackupPreferences::class.java
                        )
                    entry.name.startsWith(INK_PREFIX) -> {
                        val fileName = entry.name.removePrefix(INK_PREFIX)
                        if (fileName.isNotBlank()) ink[fileName] = bytes
                    }
                }
                zip.closeEntry()
            }
        }

        val resolvedManifest = manifest
            ?: error("Backup is missing manifest.json")
        require(resolvedManifest.schemaVersion <= SCHEMA_VERSION) {
            "This backup (v${resolvedManifest.schemaVersion}) is newer than the app supports (v$SCHEMA_VERSION)."
        }
        return JournalBackupPayload(
            manifest = resolvedManifest,
            entries = entries,
            memoryVerses = memory,
            preferences = preferences ?: BackupPreferences(),
            inkFiles = ink
        )
    }

    fun roundTripBytes(payload: JournalBackupPayload): JournalBackupPayload {
        val buffer = ByteArrayOutputStream()
        write(buffer, payload)
        return read(ByteArrayInputStream(buffer.toByteArray()))
    }

    fun inkMapFromFiles(files: List<File>): Map<String, ByteArray> =
        files.associate { it.name to it.readBytes() }
}
