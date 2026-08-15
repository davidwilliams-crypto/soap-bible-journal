package com.soapjournal.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.soapjournal.app.SoapJournalApplication
import com.soapjournal.app.data.prefs.liveStreak
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun scheduleDaily(hour: Int, minute: Int) = schedule(WORK_DAILY, TYPE_DAILY, hour, minute)

    fun scheduleFollowThrough(hour: Int) = schedule(WORK_FOLLOW_THROUGH, TYPE_FOLLOW_THROUGH, hour)

    fun scheduleStreakRisk(hour: Int) = schedule(WORK_STREAK_RISK, TYPE_STREAK_RISK, hour)

    fun cancelDaily() = cancel(WORK_DAILY)

    fun cancelFollowThrough() = cancel(WORK_FOLLOW_THROUGH)

    fun cancelStreakRisk() = cancel(WORK_STREAK_RISK)

    fun cancelAll() {
        cancelDaily()
        cancelFollowThrough()
        cancelStreakRisk()
    }

    private fun schedule(workName: String, type: String, hour: Int, minute: Int = 0) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntil(hour, minute), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_TYPE to type))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancel(workName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
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
                // An untouched draft has no application or prayer to follow through on.
                val bothKept = todayEntry != null &&
                    todayEntry.applicationFollowThrough &&
                    todayEntry.prayerFollowThrough
                if (!isDone || bothKept) return Result.success()
                Notifications.post(
                    applicationContext,
                    ReminderScheduler.CHANNEL_FOLLOW_THROUGH,
                    "Follow through today",
                    "Have you lived out your application and prayed through it?"
                )
            }
            ReminderScheduler.TYPE_STREAK_RISK -> {
                // liveStreak returns 0 once the streak is already broken — no nagging
                // about a rhythm that can no longer be kept.
                val streak = liveStreak(container.prefs.preferences.first())
                if (isDone || streak <= 0) return Result.success()
                Notifications.post(
                    applicationContext,
                    ReminderScheduler.CHANNEL_STREAK_RISK,
                    "Your streak is at risk",
                    "You have a $streak-day rhythm — open today's SOAP before midnight to keep it."
                )
            }
            else -> {
                if (isDone) return Result.success()
                Notifications.post(
                    applicationContext,
                    ReminderScheduler.CHANNEL_DAILY,
                    "Time for SOAP",
                    "Open today's reading and journal Scripture, Observation, Application, and Prayer."
                )
            }
        }
        return Result.success()
    }
}
