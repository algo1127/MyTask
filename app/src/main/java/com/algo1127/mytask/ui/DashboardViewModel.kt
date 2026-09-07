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
    val taskCounts: Map<LocalDate, Int> = emptyMap(),
    val eventCounts: Map<LocalDate, Int> = emptyMap(),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MyTaskDatabase.getDatabase(application)
    private val completionDao = database.completionDao()
    private val countdownDao = database.countdownDao()
    private val taskDao = database.taskDao()
    private val eventDao = database.eventDao()
    private val categoryDao = database.categoryDao()

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

    val calendarData: StateFlow<Triple<Map<LocalDate, Int>, Map<LocalDate, Int>, Map<LocalDate, List<TaskItem>>>> = combine(_selectedDate, _refreshTrigger) { date, _ -> date }
        .flatMapLatest { date ->
            flow {
                val start = date.minusMonths(1).withDayOfMonth(1)
                val end = date.plusMonths(1).withDayOfMonth(date.plusMonths(1).lengthOfMonth())
                val data = calendarReader.getItemsForRange(start, end)
                
                val tCounts = data.first.groupBy { it.date }.mapValues { it.value.size }
                val eCounts = data.second.groupBy { it.date }.mapValues { it.value.size }
                val tAgendas = data.first.groupBy { it.date }
                
                emit(Triple(tCounts, eCounts, tAgendas))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyMap(), emptyMap(), emptyMap()))

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

    val categories: StateFlow<List<TaskCategory>> = categoryDao.getAllCategories()
        .map { list ->
            if (list.isEmpty()) {
                // Seed initial categories if none exist
                seedDefaultCategories()
                TaskCategory.values()
            } else {
                list.map { it.toUiModel() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskCategory.values())

    val aiKnowledge: StateFlow<AiKnowledge?> = combine(categories, _refreshTrigger) { cats, _ -> 
        getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.getAiKnowledge(cats) 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiPreferences: StateFlow<Map<String, String>> = getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.aiPreferences

    fun getAiPreference(key: String): String? {
        return getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.getAiPreference(key)
    }

    fun setAiPreference(key: String, value: String) {
        getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.setAiPreference(key, value)
        refresh()
    }

    private fun seedDefaultCategories() {
        viewModelScope.launch {
            TaskCategory.values().forEach {
                categoryDao.insert(it.toEntity())
            }
        }
    }

    private fun com.algo1127.mytask.data.Category.toUiModel() = TaskCategory(
        id = id,
        label = label,
        iconName = iconName,
        colorHex = colorHex,
        position = position
    )

    private fun TaskCategory.toEntity() = com.algo1127.mytask.data.Category(
        id = id,
        label = label,
        iconName = iconName,
        colorHex = colorHex,
        position = position
    )

    fun addCategory(label: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val currentMax = categories.value.maxOfOrNull { it.position } ?: 0
            categoryDao.insert(com.algo1127.mytask.data.Category(label = label, iconName = iconName, colorHex = colorHex, position = currentMax + 1))
        }
    }

    fun updateCategory(category: TaskCategory) {
        viewModelScope.launch {
            categoryDao.update(category.toEntity())
        }
    }

    fun deleteCategory(category: TaskCategory) {
        viewModelScope.launch {
            categoryDao.delete(category.toEntity())
        }
    }

    fun reorderCategories(newList: List<TaskCategory>) {
        viewModelScope.launch {
            newList.forEachIndexed { index, category ->
                categoryDao.update(category.toEntity().copy(position = index))
            }
        }
    }

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
            refresh() // Trigger UI update
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

    fun deleteEvent(event: EventItem) {
        viewModelScope.launch {
            val success = CalendarUtils.deleteEventFromCalendar(getApplication(), event.id)
            if (success) {
                refresh()
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val success = CalendarUtils.deleteEventFromCalendar(getApplication(), taskId)
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
                isReminder = false,
                notes = task.description
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
        optionalTitle: String? = null,
        remindWhenUp: Boolean = false
    ) {
        viewModelScope.launch {
            val item = com.algo1127.mytask.ui.models.CountdownItem(
                title = title,
                targetDateTime = target,
                color = color,
                linkedItemId = linkedId,
                linkedItemType = linkedType,
                optionalTitle = optionalTitle,
                remindWhenUp = remindWhenUp
            )
            countdownDao.insertCountdown(item)
            if (remindWhenUp) {
                getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.scheduleCountdown(item)
            }
        }
    }

    fun deleteCountdown(item: com.algo1127.mytask.ui.models.CountdownItem) {
        viewModelScope.launch {
            countdownDao.deleteCountdown(item)
            getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.cancelCountdown(item.id)
        }
    }

    fun updateCountdown(item: com.algo1127.mytask.ui.models.CountdownItem) {
        viewModelScope.launch {
            countdownDao.updateCountdown(item)
            if (item.remindWhenUp) {
                getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.scheduleCountdown(item)
            } else {
                getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.cancelCountdown(item.id)
            }
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
