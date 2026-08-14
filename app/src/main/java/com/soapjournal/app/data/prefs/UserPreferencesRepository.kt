package com.soapjournal.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soapjournal.app.data.bible.BibleVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.WeekFields

private val Context.dataStore by preferencesDataStore("soap_prefs")

/** Journaling streak milestones celebrated in-app, ascending. */
val STREAK_MILESTONES = listOf(7, 30, 100, 365)

/** ISO week-based key ("2026-W33") used to grant one streak-freeze per calendar week. */
fun weekKeyForEpochDay(day: Long): String {
    val date = LocalDate.ofEpochDay(day)
    val fields = WeekFields.ISO
    val week = date.get(fields.weekOfWeekBasedYear())
    val year = date.get(fields.weekBasedYear())
    return "$year-W$week"
}

data class UserPreferences(
    val darkTheme: Boolean = false,
    val bibleVersion: BibleVersion = BibleVersion.PREFERRED_DEFAULT,
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val followThroughEnabled: Boolean = true,
    val followThroughHour: Int = 20,
    val planStartEpochDay: Long = LocalDate.now().toEpochDay(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedEpochDay: Long = -1L,
    val freezeWeekKey: String = "",
    val freezesUsedInWeek: Int = 0,
    val highestMilestoneAcknowledged: Int = 0,
    val streakRiskEnabled: Boolean = true,
    val streakRiskHour: Int = 21,
    val cachedVotdReference: String = "",
    val cachedVotdText: String = "",
    val cachedVotdEpochDay: Long = -1L,
    val githubUpdateToken: String = "",
    val backupFolderUri: String = "",
    val lastBackupEpochMs: Long = 0L,
    /** Last screen route for resume-after-app-switch (e.g. home, editor, bible). */
    val resumeRoute: String = "home",
    val resumeEntryId: Long = -1L,
    val resumeSection: String = ""
)

/** Preferences included in journal backups (never includes secrets). */
data class BackupPreferences(
    val darkTheme: Boolean = false,
    val bibleVersion: String = BibleVersion.PREFERRED_DEFAULT.name,
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val followThroughEnabled: Boolean = true,
    val followThroughHour: Int = 20,
    val planStartEpochDay: Long = LocalDate.now().toEpochDay(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedEpochDay: Long = -1L,
    val freezeWeekKey: String = "",
    val freezesUsedInWeek: Int = 0,
    val highestMilestoneAcknowledged: Int = 0
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val darkTheme = booleanPreferencesKey("dark_theme")
        val bibleVersion = stringPreferencesKey("bible_version")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val followThroughEnabled = booleanPreferencesKey("follow_through_enabled")
        val followThroughHour = intPreferencesKey("follow_through_hour")
        val planStart = longPreferencesKey("plan_start_epoch_day")
        val currentStreak = intPreferencesKey("current_streak")
        val longestStreak = intPreferencesKey("longest_streak")
        val lastCompleted = longPreferencesKey("last_completed_epoch_day")
        val freezeWeekKey = stringPreferencesKey("freeze_week_key")
        val freezesUsedInWeek = intPreferencesKey("freezes_used_in_week")
        val highestMilestoneAcknowledged = intPreferencesKey("highest_milestone_acknowledged")
        val streakRiskEnabled = booleanPreferencesKey("streak_risk_enabled")
        val streakRiskHour = intPreferencesKey("streak_risk_hour")
        val cachedVotdReference = stringPreferencesKey("cached_votd_reference")
        val cachedVotdText = stringPreferencesKey("cached_votd_text")
        val cachedVotdEpochDay = longPreferencesKey("cached_votd_epoch_day")
        val githubUpdateToken = stringPreferencesKey("github_update_token")
        val backupFolderUri = stringPreferencesKey("backup_folder_uri")
        val lastBackupEpochMs = longPreferencesKey("last_backup_epoch_ms")
        val resumeRoute = stringPreferencesKey("resume_route")
        val resumeEntryId = longPreferencesKey("resume_entry_id")
        val resumeSection = stringPreferencesKey("resume_section")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            darkTheme = prefs[Keys.darkTheme] ?: false,
            bibleVersion = BibleVersion.fromName(
                prefs[Keys.bibleVersion] ?: BibleVersion.PREFERRED_DEFAULT.name
            ),
            remindersEnabled = prefs[Keys.remindersEnabled] ?: true,
            reminderHour = prefs[Keys.reminderHour] ?: 8,
            reminderMinute = prefs[Keys.reminderMinute] ?: 0,
            followThroughEnabled = prefs[Keys.followThroughEnabled] ?: true,
            followThroughHour = prefs[Keys.followThroughHour] ?: 20,
            planStartEpochDay = prefs[Keys.planStart] ?: LocalDate.now().toEpochDay(),
            currentStreak = prefs[Keys.currentStreak] ?: 0,
            longestStreak = prefs[Keys.longestStreak] ?: 0,
            lastCompletedEpochDay = prefs[Keys.lastCompleted] ?: -1L,
            freezeWeekKey = prefs[Keys.freezeWeekKey].orEmpty(),
            freezesUsedInWeek = prefs[Keys.freezesUsedInWeek] ?: 0,
            highestMilestoneAcknowledged = prefs[Keys.highestMilestoneAcknowledged] ?: 0,
            streakRiskEnabled = prefs[Keys.streakRiskEnabled] ?: true,
            streakRiskHour = prefs[Keys.streakRiskHour] ?: 21,
            cachedVotdReference = prefs[Keys.cachedVotdReference].orEmpty(),
            cachedVotdText = prefs[Keys.cachedVotdText].orEmpty(),
            cachedVotdEpochDay = prefs[Keys.cachedVotdEpochDay] ?: -1L,
            githubUpdateToken = prefs[Keys.githubUpdateToken].orEmpty(),
            backupFolderUri = prefs[Keys.backupFolderUri].orEmpty(),
            lastBackupEpochMs = prefs[Keys.lastBackupEpochMs] ?: 0L,
            resumeRoute = prefs[Keys.resumeRoute] ?: "home",
            resumeEntryId = prefs[Keys.resumeEntryId] ?: -1L,
            resumeSection = prefs[Keys.resumeSection].orEmpty()
        )
    }

    suspend fun snapshotForBackup(): BackupPreferences {
        val prefs = preferences.first()
        return BackupPreferences(
            darkTheme = prefs.darkTheme,
            bibleVersion = prefs.bibleVersion.name,
            remindersEnabled = prefs.remindersEnabled,
            reminderHour = prefs.reminderHour,
            reminderMinute = prefs.reminderMinute,
            followThroughEnabled = prefs.followThroughEnabled,
            followThroughHour = prefs.followThroughHour,
            planStartEpochDay = prefs.planStartEpochDay,
            currentStreak = prefs.currentStreak,
            longestStreak = prefs.longestStreak,
            lastCompletedEpochDay = prefs.lastCompletedEpochDay,
            freezeWeekKey = prefs.freezeWeekKey,
            freezesUsedInWeek = prefs.freezesUsedInWeek,
            highestMilestoneAcknowledged = prefs.highestMilestoneAcknowledged
        )
    }

    suspend fun restoreFromBackup(backup: BackupPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.darkTheme] = backup.darkTheme
            prefs[Keys.bibleVersion] = backup.bibleVersion
            prefs[Keys.remindersEnabled] = backup.remindersEnabled
            prefs[Keys.reminderHour] = backup.reminderHour
            prefs[Keys.reminderMinute] = backup.reminderMinute
            prefs[Keys.followThroughEnabled] = backup.followThroughEnabled
            prefs[Keys.followThroughHour] = backup.followThroughHour
            prefs[Keys.planStart] = backup.planStartEpochDay
            prefs[Keys.currentStreak] = backup.currentStreak
            prefs[Keys.longestStreak] = backup.longestStreak
            prefs[Keys.lastCompleted] = backup.lastCompletedEpochDay
            prefs[Keys.freezeWeekKey] = backup.freezeWeekKey
            prefs[Keys.freezesUsedInWeek] = backup.freezesUsedInWeek
            prefs[Keys.highestMilestoneAcknowledged] = backup.highestMilestoneAcknowledged
        }
    }

    /**
     * Persist the plan start on first launch so day progress does not reset to "Day 1"
     * every time preferences are read without a stored value.
     */
    suspend fun ensurePlanStartInitialized(date: LocalDate = LocalDate.now()) {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.planStart] == null) {
                prefs[Keys.planStart] = date.toEpochDay()
            }
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.darkTheme] = enabled }
    }

    suspend fun setBibleVersion(version: BibleVersion) {
        context.dataStore.edit { it[Keys.bibleVersion] = version.name }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.remindersEnabled] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.reminderHour] = hour
            it[Keys.reminderMinute] = minute
        }
    }

    suspend fun setFollowThroughEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.followThroughEnabled] = enabled }
    }

    suspend fun setFollowThroughHour(hour: Int) {
        context.dataStore.edit { it[Keys.followThroughHour] = hour }
    }

    suspend fun setPlanStart(date: LocalDate) {
        context.dataStore.edit { it[Keys.planStart] = date.toEpochDay() }
    }

    suspend fun setGithubUpdateToken(token: String) {
        context.dataStore.edit { prefs ->
            val cleaned = token.trim()
            if (cleaned.isEmpty()) {
                prefs.remove(Keys.githubUpdateToken)
            } else {
                prefs[Keys.githubUpdateToken] = cleaned
            }
        }
    }

    suspend fun setBackupFolderUri(uri: String?) {
        context.dataStore.edit { prefs ->
            val cleaned = uri?.trim().orEmpty()
            if (cleaned.isEmpty()) {
                prefs.remove(Keys.backupFolderUri)
            } else {
                prefs[Keys.backupFolderUri] = cleaned
            }
        }
    }

    suspend fun setLastBackupEpochMs(epochMs: Long) {
        context.dataStore.edit { it[Keys.lastBackupEpochMs] = epochMs }
    }

    suspend fun setResumeSession(
        route: String,
        entryId: Long = -1L,
        section: String = ""
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.resumeRoute] = route
            if (entryId > 0L) {
                prefs[Keys.resumeEntryId] = entryId
            } else {
                prefs.remove(Keys.resumeEntryId)
            }
            if (section.isNotBlank()) {
                prefs[Keys.resumeSection] = section
            } else {
                prefs.remove(Keys.resumeSection)
            }
        }
    }

    suspend fun setStreakRiskEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.streakRiskEnabled] = enabled }
    }

    suspend fun setStreakRiskHour(hour: Int) {
        context.dataStore.edit { it[Keys.streakRiskHour] = hour }
    }

    suspend fun cacheVerseOfTheDay(reference: String, text: String, epochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.cachedVotdReference] = reference
            prefs[Keys.cachedVotdText] = text
            prefs[Keys.cachedVotdEpochDay] = epochDay
        }
    }

    /**
     * Records a completed SOAP entry for [date] and rolls the streak forward.
     * A single missed day per ISO week can be covered by a "freeze" so one bad day
     * doesn't erase the habit. Returns the milestone (7/30/100/365) just reached, or
     * null if this completion didn't cross a new one.
     */
    suspend fun recordDailyCompletion(date: LocalDate = LocalDate.now()): Int? {
        var crossedMilestone: Int? = null
        context.dataStore.edit { prefs ->
            val day = date.toEpochDay()
            val last = prefs[Keys.lastCompleted] ?: -1L
            if (last == day) return@edit
            // Never move lastCompleted backward (e.g. saving an older entry).
            if (day < last) return@edit

            val current = prefs[Keys.currentStreak] ?: 0
            val longest = prefs[Keys.longestStreak] ?: 0
            val gapDays = day - last - 1
            val next = when {
                last == day - 1L -> current + 1
                last >= 0 && gapDays == 1L && tryConsumeFreeze(prefs, missedDay = last + 1) -> current + 1
                else -> 1
            }
            prefs[Keys.currentStreak] = next
            prefs[Keys.longestStreak] = maxOf(longest, next)
            prefs[Keys.lastCompleted] = day

            val highestAck = prefs[Keys.highestMilestoneAcknowledged] ?: 0
            if (next in STREAK_MILESTONES && next > highestAck) {
                prefs[Keys.highestMilestoneAcknowledged] = next
                crossedMilestone = next
            }
        }
        return crossedMilestone
    }

    /** Attempts to spend this week's single streak-freeze on [missedDay]; true if consumed. */
    private fun tryConsumeFreeze(prefs: MutablePreferences, missedDay: Long): Boolean {
        val weekKey = weekKeyForEpochDay(missedDay)
        val storedWeek = prefs[Keys.freezeWeekKey]
        val usedThisWeek = if (storedWeek == weekKey) prefs[Keys.freezesUsedInWeek] ?: 0 else 0
        if (usedThisWeek >= 1) return false
        prefs[Keys.freezeWeekKey] = weekKey
        prefs[Keys.freezesUsedInWeek] = usedThisWeek + 1
        return true
    }
}

/** Freezes left for the ISO week containing [today]; 0 or 1. */
fun freezesAvailable(prefs: UserPreferences, today: LocalDate = LocalDate.now()): Int {
    val weekKey = weekKeyForEpochDay(today.toEpochDay())
    val used = if (prefs.freezeWeekKey == weekKey) prefs.freezesUsedInWeek else 0
    return (1 - used).coerceIn(0, 1)
}
