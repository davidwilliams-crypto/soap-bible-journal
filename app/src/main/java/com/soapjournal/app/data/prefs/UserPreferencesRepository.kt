package com.soapjournal.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soapjournal.app.data.bible.BibleVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore("soap_prefs")

data class UserPreferences(
    val darkTheme: Boolean = false,
    val bibleVersion: BibleVersion = BibleVersion.KJV,
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val followThroughEnabled: Boolean = true,
    val followThroughHour: Int = 20,
    val planStartEpochDay: Long = LocalDate.now().toEpochDay(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedEpochDay: Long = -1L
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
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            darkTheme = prefs[Keys.darkTheme] ?: false,
            bibleVersion = BibleVersion.fromName(prefs[Keys.bibleVersion] ?: BibleVersion.KJV.name),
            remindersEnabled = prefs[Keys.remindersEnabled] ?: true,
            reminderHour = prefs[Keys.reminderHour] ?: 8,
            reminderMinute = prefs[Keys.reminderMinute] ?: 0,
            followThroughEnabled = prefs[Keys.followThroughEnabled] ?: true,
            followThroughHour = prefs[Keys.followThroughHour] ?: 20,
            planStartEpochDay = prefs[Keys.planStart] ?: LocalDate.now().toEpochDay(),
            currentStreak = prefs[Keys.currentStreak] ?: 0,
            longestStreak = prefs[Keys.longestStreak] ?: 0,
            lastCompletedEpochDay = prefs[Keys.lastCompleted] ?: -1L
        )
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

    suspend fun recordDailyCompletion(date: LocalDate = LocalDate.now()) {
        context.dataStore.edit { prefs ->
            val day = date.toEpochDay()
            val last = prefs[Keys.lastCompleted] ?: -1L
            if (last == day) return@edit
            // Never move lastCompleted backward (e.g. saving an older entry).
            if (day < last) return@edit

            val current = prefs[Keys.currentStreak] ?: 0
            val longest = prefs[Keys.longestStreak] ?: 0
            val next = when {
                last == day - 1L -> current + 1
                else -> 1
            }
            prefs[Keys.currentStreak] = next
            prefs[Keys.longestStreak] = maxOf(longest, next)
            prefs[Keys.lastCompleted] = day
        }
    }
}
