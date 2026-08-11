package com.soapjournal.app.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soapjournal.app.data.JournalRepository
import com.soapjournal.app.data.SoapEntryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    private val queryFlow = kotlinx.coroutines.flow.MutableStateFlow("")

    val entries: StateFlow<List<SoapEntryEntity>> = queryFlow
        .flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(value: String) {
        query = value
        queryFlow.value = value
    }

    fun delete(entryId: Long) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
        }
    }

    class Factory(
        private val repository: JournalRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
