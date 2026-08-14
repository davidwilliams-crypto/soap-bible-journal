package com.soapjournal.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.soapjournal.app.MainActivity
import com.soapjournal.app.R
import com.soapjournal.app.SoapJournalApplication
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun scheduleDaily(hour: Int, minute: Int) {
        val delay = millisUntil(hour, minute)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_TYPE to TYPE_DAILY))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleFollowThrough(hour: Int) {
        val delay = millisUntil(hour, 0)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_TYPE to TYPE_FOLLOW_THROUGH))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_FOLLOW_THROUGH,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleStreakRisk(hour: Int) {
        val delay = millisUntil(hour, 0)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_TYPE to TYPE_STREAK_RISK))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_STREAK_RISK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDaily() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_DAILY)
    }

    fun cancelFollowThrough() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_FOLLOW_THROUGH)
    }

    fun cancelStreakRisk() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_STREAK_RISK)
    }

    fun cancelAll() {
        cancelDaily()
        cancelFollowThrough()
        cancelStreakRisk()
    }

    private fun millisUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        const val CHANNEL_DAILY = "soap_daily"
        const val CHANNEL_FOLLOW_THROUGH = "soap_follow_through"
        const val CHANNEL_STREAK_RISK = "soap_streak_risk"
        const val WORK_DAILY = "daily_soap_reminder"
        const val WORK_FOLLOW_THROUGH = "follow_through_reminder"
        const val WORK_STREAK_RISK = "streak_risk_reminder"
        const val KEY_TYPE = "type"
        const val TYPE_DAILY = "daily"
        const val TYPE_FOLLOW_THROUGH = "follow_through"
        const val TYPE_STREAK_RISK = "streak_risk"
    }
}

/**
 * Reminders are habit nudges, not noise: each type checks the day's actual state
 * before notifying so a user who already journaled (or has no streak at risk) isn't pinged.
 */
class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val type = inputData.getString(ReminderScheduler.KEY_TYPE)
            ?: ReminderScheduler.TYPE_DAILY
        val container = (applicationContext as SoapJournalApplication).container
        val todayEntry = container.repository.getTodayEntry()
        val isDone = todayEntry != null && !todayEntry.isDraft

        when (type) {
            ReminderScheduler.TYPE_FOLLOW_THROUGH -> {
                val bothKept = todayEntry != null &&
                    todayEntry.applicationFollowThrough &&
                    todayEntry.prayerFollowThrough
                if (todayEntry == null || bothKept) return Result.success()
                showNotification(
                    "Follow through today",
                    "Have you lived out your application and prayed through it?",
                    ReminderScheduler.CHANNEL_FOLLOW_THROUGH
                )
            }
            ReminderScheduler.TYPE_STREAK_RISK -> {
                val prefs = container.prefs.preferences.first()
                if (isDone || prefs.currentStreak <= 0) return Result.success()
                showNotification(
                    "Your streak is at risk",
                    "You have a ${prefs.currentStreak}-day rhythm — open today's SOAP before midnight to keep it.",
                    ReminderScheduler.CHANNEL_STREAK_RISK
                )
            }
            else -> {
                if (isDone) return Result.success()
                showNotification(
                    "Time for SOAP",
                    "Open today's reading and journal Scripture, Observation, Application, and Prayer.",
                    ReminderScheduler.CHANNEL_DAILY
                )
            }
        }
        return Result.success()
    }

    private fun showNotification(title: String, body: String, channel: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val launch = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext,
            channel.hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext)
            .notify(channel.hashCode(), notification)
    }
}
