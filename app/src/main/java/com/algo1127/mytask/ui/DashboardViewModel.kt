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
import java.time.LocalDateTime

data class DashboardUiState(
    val tasks: List<TaskItem> = emptyList(),
    val events: List<EventItem> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MyTaskDatabase.getDatabase(application)
    private val completionDao = database.completionDao()
    private val countdownDao = database.countdownDao()
    private val taskDao = database.taskDao()
    private val eventDao = database.eventDao()

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

    val unscheduledTasks: StateFlow<List<com.algo1127.mytask.ui.models.Task>> = _refreshTrigger
        .flatMapLatest { 
            flow {
                emit(database.taskDao().getAllTasks())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedIds: StateFlow<Set<Long>> = _selectedDate
        .flatMapLatest { date ->
            completionDao.getRecordsForDate(date.toString())
        }
        .map { records ->
            records.filter { it.isDone }.map { it.itemId }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val activeCountdowns: StateFlow<List<com.algo1127.mytask.ui.models.CountdownItem>> = countdownDao.getAllCountdowns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updateTask(task: TaskItem) {
        viewModelScope.launch {
            val success = CalendarUtils.updateTaskInCalendar(getApplication(), task)
            if (success) {
                refresh()
            }
        }
    }

    fun scheduleTask(task: com.algo1127.mytask.ui.models.Task, date: LocalDate) {
        viewModelScope.launch {
            val taskItem = TaskItem(
                title = task.title,
                time = "09:00",
                category = task.category,
                date = date,
                isReminder = false
            )
            val id = CalendarUtils.addTaskToCalendar(getApplication(), taskItem)
            if (id != null) {
                database.taskDao().deleteTask(task)
                refresh()
            }
        }
    }

    // --- COUNTDOWN CRUD ---

    fun addCountdown(
        title: String, 
        target: LocalDateTime, 
        color: Int,
        linkedId: Long? = null,
        linkedType: String? = null,
        optionalTitle: String? = null
    ) {
        viewModelScope.launch {
            val item = com.algo1127.mytask.ui.models.CountdownItem(
                title = title,
                targetDateTime = target,
                color = color,
                linkedItemId = linkedId,
                linkedItemType = linkedType,
                optionalTitle = optionalTitle
            )
            countdownDao.insertCountdown(item)
        }
    }

    fun deleteCountdown(item: com.algo1127.mytask.ui.models.CountdownItem) {
        viewModelScope.launch {
            countdownDao.deleteCountdown(item)
        }
    }

    fun updateCountdown(item: com.algo1127.mytask.ui.models.CountdownItem) {
        viewModelScope.launch {
            countdownDao.updateCountdown(item)
        }
    }

    suspend fun getLinkedItemInfo(id: Long, type: String): Pair<String, com.algo1127.mytask.ui.TaskCategory>? {
        return when (type) {
            "TASK" -> {
                val task = taskDao.getTaskById(id)
                task?.let { it.title to it.category }
            }
            "EVENT" -> {
                val event = eventDao.getEventById(id)
                // Events don't have TaskCategory in the current model, but let's assume a default or map it
                event?.let { it.title to com.algo1127.mytask.ui.TaskCategory.Personal }
            }
            else -> null
        }
    }
}
