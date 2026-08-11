package com.soapjournal.app

import android.content.Context
import com.soapjournal.app.data.AppDatabase
import com.soapjournal.app.data.JournalRepository
import com.soapjournal.app.data.bible.BibleRepository
import com.soapjournal.app.data.ink.InkStore
import com.soapjournal.app.data.prefs.UserPreferencesRepository
import com.soapjournal.app.notifications.ReminderScheduler
import com.soapjournal.app.update.AppUpdateManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)

    val prefs = UserPreferencesRepository(appContext)
    val bible = BibleRepository()
    val repository = JournalRepository(
        dao = db.soapEntryDao(),
        inkStore = InkStore(appContext),
        memoryDao = db.memoryVerseDao(),
        prefs = prefs
    )
    val reminders = ReminderScheduler(appContext)
    val updates = AppUpdateManager(appContext)
}
