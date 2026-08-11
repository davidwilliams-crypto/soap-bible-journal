package com.soapjournal.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.SoapEntryEntity
import com.soapjournal.app.data.bible.BibleVersion
import com.soapjournal.app.data.bible.VerseOfTheDay
import com.soapjournal.app.data.plan.ReadingPlan
import com.soapjournal.app.data.prefs.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val repository = container.repository
    private val bible = container.bible
    private val prefsRepo = container.prefs

    val entries: StateFlow<List<SoapEntryEntity>> = repository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    private val _verseOfTheDay = MutableStateFlow<VerseOfTheDay?>(null)
    val verseOfTheDay: StateFlow<VerseOfTheDay?> = _verseOfTheDay

    private val _votdLoading = MutableStateFlow(true)
    val votdLoading: StateFlow<Boolean> = _votdLoading

    init {
        viewModelScope.launch {
            preferences.collectLatest { prefs ->
                refreshVerseOfTheDay(prefs.bibleVersion)
            }
        }
    }

    fun refreshVerseOfTheDay(preferredVersion: BibleVersion = preferences.value.bibleVersion) {
        viewModelScope.launch {
            _votdLoading.value = true
            _verseOfTheDay.value = bible.verseOfTheDay(
                date = LocalDate.now(),
                preferredVersion = preferredVersion
            )
            _votdLoading.value = false
        }
    }

    fun todayReading(prefs: UserPreferences): ReadingPlan.DayReading {
        val start = LocalDate.ofEpochDay(prefs.planStartEpochDay)
        return ReadingPlan.dayFor(start)
    }

    fun openTodayFromPlan(onReady: (Long) -> Unit) {
        viewModelScope.launch {
            val prefs = preferences.value
            val reading = todayReading(prefs)
            val text = bible.passageText(reading.passages, prefs.bibleVersion)
            val entry = repository.getOrCreateTodayEntry(
                scriptureReference = reading.label,
                scriptureText = text,
                readingPlanDay = reading.dayIndex
            )
            onReady(entry.id)
        }
    }

    fun openToday(onReady: (Long) -> Unit) {
        viewModelScope.launch {
            onReady(repository.getOrCreateTodayEntry().id)
        }
    }

    fun createNew(onReady: (Long) -> Unit) {
        viewModelScope.launch {
            onReady(repository.createEntry().id)
        }
    }

    fun addVotdToMemory() {
        viewModelScope.launch {
            val v = verseOfTheDay.value?.verse ?: return@launch
            if (v.text.isBlank() || v.text.startsWith("Unable to load")) return@launch
            repository.addMemoryVerse(v.reference, v.text, source = "verse_of_the_day")
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(container) as T
        }
    }
}
