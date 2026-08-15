package com.soapjournal.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.soapjournal.app.MainActivity
import com.soapjournal.app.R
import com.soapjournal.app.SoapJournalApplication
import com.soapjournal.app.data.prefs.UserPreferences
import com.soapjournal.app.data.prefs.liveStreak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Home-screen widget: current streak plus today's cached verse, tap to open the app. */
class SoapWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Broadcasts only arrive once the app process (and its Application) is up,
                // so reading the container here is safe.
                val app = context.applicationContext as SoapJournalApplication
                val prefs = app.container.prefs.preferences.first()
                appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id, prefs) }
            } catch (_: Throwable) {
                // A failed refresh keeps the last rendered state; a periodic widget
                // update must never take the whole process down.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /**
         * Called whenever streak or verse-of-the-day state changes, so widgets stay current.
         * Takes [prefs] directly rather than re-reading the container, since this can run
         * while [SoapJournalApplication.container] is still being constructed.
         */
        fun updateAll(context: Context, prefs: UserPreferences) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SoapWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { id -> updateWidget(context, manager, id, prefs) }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            prefs: UserPreferences
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_soap)
            // liveStreak, not currentStreak: a broken streak lingers in prefs, and the
            // widget must not keep advertising a rhythm that already ended.
            val streak = liveStreak(prefs)
            views.setTextViewText(
                R.id.widget_streak,
                if (streak > 0) {
                    "$streak-day rhythm"
                } else {
                    "Begin today's SOAP"
                }
            )

            val today = LocalDate.now().toEpochDay()
            val hasFreshVerse = prefs.cachedVotdText.isNotBlank() &&
                prefs.cachedVotdEpochDay == today
            if (hasFreshVerse) {
                views.setTextViewText(R.id.widget_reference, prefs.cachedVotdReference)
                views.setTextViewText(R.id.widget_verse, prefs.cachedVotdText)
                views.setViewVisibility(R.id.widget_verse_group, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_verse_group, View.GONE)
            }

            val launchIntent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context,
                id,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}
