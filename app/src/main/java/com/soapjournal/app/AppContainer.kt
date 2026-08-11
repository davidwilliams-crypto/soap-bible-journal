package com.soapjournal.app

import android.content.Context
import com.soapjournal.app.data.AppDatabase
import com.soapjournal.app.data.JournalRepository
import com.soapjournal.app.data.bible.BibleRepository
import com.soapjournal.app.data.ink.InkStore
import com.soapjournal.app.data.prefs.UserPreferencesRepository
import com.soapjournal.app.notifications.ReminderScheduler
import com.soapjournal.app.update.AppUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var githubUpdateToken: String = ""

    val prefs = UserPreferencesRepository(appContext)
    val bible = BibleRepository()
    val repository = JournalRepository(
        dao = db.soapEntryDao(),
        inkStore = InkStore(appContext),
        memoryDao = db.memoryVerseDao(),
        prefs = prefs
    )
    val reminders = ReminderScheduler(appContext)
    val updates = AppUpdateManager(
        context = appContext,
        tokenProvider = { githubUpdateToken }
    )

    init {
        scope.launch {
            prefs.preferences.collect { githubUpdateToken = it.githubUpdateToken }
        }
    }
}
