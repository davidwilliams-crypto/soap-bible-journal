package com.soapjournal.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.soapjournal.app.notifications.MilestoneNotifier
import com.soapjournal.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SoapJournalApplication : Application() {
    lateinit var container: AppContainer
        private set

    val repository get() = container.repository
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        appScope.launch {
            container.prefs.ensurePlanStartInitialized()
            val prefs = container.prefs.preferences.first()
            if (prefs.remindersEnabled) {
                container.reminders.scheduleDaily(prefs.reminderHour, prefs.reminderMinute)
            }
            if (prefs.followThroughEnabled) {
                container.reminders.scheduleFollowThrough(prefs.followThroughHour)
            }
            if (prefs.streakRiskEnabled) {
                container.reminders.scheduleStreakRisk(prefs.streakRiskHour)
            }
        }
        appScope.launch {
            container.repository.milestoneEvents.collect { days ->
                MilestoneNotifier.celebrate(this@SoapJournalApplication, days)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderScheduler.CHANNEL_DAILY,
                "Daily SOAP reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderScheduler.CHANNEL_FOLLOW_THROUGH,
                "Application & prayer follow-through",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderScheduler.CHANNEL_STREAK_RISK,
                "Streak at risk",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                MilestoneNotifier.CHANNEL_MILESTONE,
                "Streak milestones",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }
}
