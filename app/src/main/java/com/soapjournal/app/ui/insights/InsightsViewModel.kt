package com.soapjournal.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soapjournal.app.AppContainer
import com.soapjournal.app.data.prefs.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Weeks of history shown in the journaling heatmap. */
const val INSIGHTS_HEATMAP_WEEKS = 18

class InsightsViewModel(container: AppContainer) : ViewModel() {
    private val repository = container.repository

    val preferences: StateFlow<UserPreferences> = container.prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    val completedEntryCount: StateFlow<Int> = repository.observeCompletedEntryCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val daysJournaledCount: StateFlow<Int> = repository.observeDaysJournaledCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val memoryVerseCount: StateFlow<Int> = repository.observeMemoryVerseCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val heatmapDays: StateFlow<Set<Long>> = run {
        val today = LocalDate.now()
        // Fetch 6 extra days so the grid's Sunday-aligned first column (which can
        // reach back past the nominal window) never renders real entries as missed.
        val since = today.minusWeeks(INSIGHTS_HEATMAP_WEEKS.toLong()).minusDays(6)
        repository.observeHeatmapDays(since.toEpochDay())
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InsightsViewModel(container) as T
        }
    }
}
