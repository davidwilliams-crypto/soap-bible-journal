package com.soapjournal.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.soapjournal.app.BuildConfig
import com.soapjournal.app.data.AppDatabase
import com.soapjournal.app.data.ink.InkStore
import com.soapjournal.app.data.prefs.UserPreferencesRepository
import com.soapjournal.app.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

sealed class BackupResult {
    data class Success(
        val entryCount: Int,
        val memoryCount: Int,
        val inkFileCount: Int,
        val message: String
    ) : BackupResult()

    data class Error(val message: String) : BackupResult()
}

/**
 * Builds a portable journal archive and stores it in a user-chosen folder
 * (typically Google Drive via the system folder picker).
 */
class JournalBackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val inkStore: InkStore,
    private val prefs: UserPreferencesRepository,
    private val reminders: ReminderScheduler
) {
    companion object {
        const val LATEST_FILE_NAME = "SOAP-Journal-Backup-latest.zip"
        const val MIME_ZIP = "application/zip"
    }

    suspend fun buildPayload(): JournalBackupPayload = withContext(Dispatchers.IO) {
        val entries = database.soapEntryDao().getAll()
        val memory = database.memoryVerseDao().getAll()
        val inkFiles = JournalBackupArchive.inkMapFromFiles(inkStore.listFiles())
        val preferences = prefs.snapshotForBackup()
        JournalBackupPayload(
            manifest = BackupManifest(
                appVersionCode = BuildConfig.VERSION_CODE,
                appVersionName = BuildConfig.VERSION_NAME,
                createdAtEpochMs = System.currentTimeMillis(),
                entryCount = entries.size,
                memoryCount = memory.size,
                inkFileCount = inkFiles.size
            ),
            entries = entries,
            memoryVerses = memory,
            preferences = preferences,
            inkFiles = inkFiles
        )
    }

    suspend fun backupToDriveFolder(): BackupResult = withContext(Dispatchers.IO) {
        val folderUri = prefs.preferences.first().backupFolderUri
        if (folderUri.isBlank()) {
            return@withContext BackupResult.Error(
                "Choose a Google Drive folder first."
            )
        }
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
            ?: return@withContext BackupResult.Error("Could not open the backup folder.")
        if (!tree.canWrite()) {
            return@withContext BackupResult.Error(
                "Backup folder is not writable. Choose your Google Drive folder again."
            )
        }

        val payload = buildPayload()
        runCatching {
            writePayloadToTree(tree, LATEST_FILE_NAME, payload)
            // Keep a dated snapshot too, so older backups remain recoverable.
            val stamp = android.text.format.DateFormat.format(
                "yyyyMMdd-HHmm",
                Date(payload.manifest.createdAtEpochMs)
            )
            writePayloadToTree(tree, "SOAP-Journal-Backup-$stamp.zip", payload)
            prefs.setLastBackupEpochMs(payload.manifest.createdAtEpochMs)
            BackupResult.Success(
                entryCount = payload.manifest.entryCount,
                memoryCount = payload.manifest.memoryCount,
                inkFileCount = payload.manifest.inkFileCount,
                message = "Backed up ${payload.manifest.entryCount} entries to Google Drive."
            )
        }.getOrElse {
            BackupResult.Error(it.message ?: "Backup failed.")
        }
    }

    suspend fun restoreFromUri(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            val payload = context.contentResolver.openInputStream(uri)?.use {
                JournalBackupArchive.read(it)
            } ?: return@withContext BackupResult.Error("Could not read the backup file.")

            applyPayload(payload)
            BackupResult.Success(
                entryCount = payload.entries.size,
                memoryCount = payload.memoryVerses.size,
                inkFileCount = payload.inkFiles.size,
                message = "Restored ${payload.entries.size} entries and ${payload.memoryVerses.size} memory verses."
            )
        }.getOrElse {
            BackupResult.Error(it.message ?: "Restore failed.")
        }
    }

    suspend fun restoreLatestFromDriveFolder(): BackupResult = withContext(Dispatchers.IO) {
        val folderUri = prefs.preferences.first().backupFolderUri
        if (folderUri.isBlank()) {
            return@withContext BackupResult.Error("Choose a Google Drive folder first.")
        }
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
            ?: return@withContext BackupResult.Error("Could not open the backup folder.")
        val file = tree.findFile(LATEST_FILE_NAME)
            ?: tree.listFiles()
                .filter { it.isFile && it.name?.endsWith(".zip", ignoreCase = true) == true }
                .maxByOrNull { it.lastModified() }
            ?: return@withContext BackupResult.Error("No backup zip found in that folder.")
        restoreFromUri(file.uri)
    }

    fun takePersistableFolderPermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Some providers only grant for the session; backup may still work immediately.
        }
    }

    fun formatLastBackup(epochMs: Long): String {
        if (epochMs <= 0L) return "Not backed up yet"
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(epochMs))
    }

    private fun writePayloadToTree(
        tree: DocumentFile,
        fileName: String,
        payload: JournalBackupPayload
    ) {
        tree.findFile(fileName)?.delete()
        val target = tree.createFile(MIME_ZIP, fileName)
            ?: error("Could not create $fileName in the Drive folder.")
        context.contentResolver.openOutputStream(target.uri)?.use { stream ->
            JournalBackupArchive.write(stream, payload)
        } ?: error("Could not open $fileName for writing.")
    }

    private suspend fun applyPayload(payload: JournalBackupPayload) {
        val entryDao = database.soapEntryDao()
        val memoryDao = database.memoryVerseDao()

        entryDao.deleteAll()
        memoryDao.deleteAll()
        inkStore.clearAll()

        payload.entries.forEach { entry ->
            entryDao.insert(entry)
        }
        payload.memoryVerses.forEach { verse ->
            memoryDao.insert(verse)
        }
        payload.inkFiles.forEach { (name, bytes) ->
            runCatching { inkStore.writeBytes(name, bytes) }
        }

        prefs.restoreFromBackup(payload.preferences)

        val restoredPrefs = prefs.preferences.first()
        if (restoredPrefs.remindersEnabled) {
            reminders.scheduleDaily(restoredPrefs.reminderHour, restoredPrefs.reminderMinute)
        } else {
            reminders.cancelDaily()
        }
        if (restoredPrefs.followThroughEnabled) {
            reminders.scheduleFollowThrough(restoredPrefs.followThroughHour)
        } else {
            reminders.cancelFollowThrough()
        }
    }
}
