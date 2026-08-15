package com.soapjournal.app

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import com.soapjournal.app.notifications.MilestoneNotifier
import com.soapjournal.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class SoapJournalApplication : Application() {
    lateinit var container: AppContainer
        private set

    val repository get() = container.repository
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val startedActivities = AtomicInteger(0)

    private val isInForeground: Boolean
        get() = startedActivities.get() > 0

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        trackForeground()
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
                // The editor shows its own celebration dialog, so only notify when the
                // app isn't visible (e.g. the entry completed via a background flush).
                if (!isInForeground) {
                    MilestoneNotifier.celebrate(this@SoapJournalApplication, days)
                }
            }
        }
    }

    private fun trackForeground() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities.incrementAndGet()
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities.decrementAndGet()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
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
