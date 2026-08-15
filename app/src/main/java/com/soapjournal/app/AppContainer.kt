package com.soapjournal.app

import android.content.Context
import com.soapjournal.app.backup.JournalBackupManager
import com.soapjournal.app.data.AppDatabase
import com.soapjournal.app.data.JournalRepository
import com.soapjournal.app.data.bible.BibleRepository
import com.soapjournal.app.data.bible.NetworkStatus
import com.soapjournal.app.data.ink.InkStore
import com.soapjournal.app.data.prefs.UserPreferencesRepository
import com.soapjournal.app.notifications.ReminderScheduler
import com.soapjournal.app.update.AppUpdateManager
import com.soapjournal.app.widget.SoapWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inkStore = InkStore(appContext)

    @Volatile
    private var githubUpdateToken: String = ""

    val prefs = UserPreferencesRepository(appContext)
    val bible = BibleRepository(
        isOnline = { NetworkStatus.isOnline(appContext) }
    )
    val repository = JournalRepository(
        dao = db.soapEntryDao(),
        inkStore = inkStore,
        memoryDao = db.memoryVerseDao(),
        prefs = prefs
    )
    val reminders = ReminderScheduler(appContext)
    val backup = JournalBackupManager(
        context = appContext,
        database = db,
        inkStore = inkStore,
        prefs = prefs,
        reminders = reminders
    )
    val updates = AppUpdateManager(
        context = appContext,
        tokenProvider = { githubUpdateToken }
    )

    init {
        scope.launch {
            prefs.preferences.collect { githubUpdateToken = it.githubUpdateToken }
        }
        scope.launch {
            prefs.preferences
                .distinctUntilChangedBy { Triple(it.currentStreak, it.cachedVotdText, it.cachedVotdEpochDay) }
                .collect { updated ->
                    // Widget refresh is best-effort; a RemoteViews failure must not
                    // kill this scope (or the process).
                    runCatching { SoapWidgetProvider.updateAll(appContext, updated) }
                }
        }
    }
}
