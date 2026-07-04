package com.algo1127.mytask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.algo1127.mytask.data.CompletionRecord
import com.algo1127.mytask.data.MyTaskDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val tasks: List<TaskItem> = emptyList(),
    val events: List<EventItem> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MyTaskDatabase.getDatabase(application)
    private val completionDao = database.completionDao()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    private val calendarReader = CalendarReader(application)

    val uiState: StateFlow<DashboardUiState> = combine(_selectedDate, _refreshTrigger) { date, _ -> date }
        .flatMapLatest { date ->
            flow {
                emit(DashboardUiState(isLoading = true))
                val data = calendarReader.getItemsForDate(date)
                emit(DashboardUiState(
                    tasks = data.first,
                    events = data.second,
                    isLoading = false
                ))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(isLoading = true))

    val completedIds: StateFlow<Set<Long>> = _selectedDate
        .flatMapLatest { date ->
            completionDao.getRecordsForDate(date.toString())
        }
        .map { records ->
            records.filter { it.isDone }.map { it.itemId }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setSelectedDate(date: LocalDate) {
        if (_selectedDate.value != date) {
            _selectedDate.value = date
        }
    }

    fun refresh() {
        _refreshTrigger.value += 1
    }

    fun toggleCompletion(itemId: Long, isDone: Boolean) {
        viewModelScope.launch {
            val date = _selectedDate.value.toString()
            val record = CompletionRecord(
                id = "$itemId:$date",
                itemId = itemId,
                date = date,
                isDone = isDone
            )
            completionDao.insertOrUpdate(record)
        }
    }
}
