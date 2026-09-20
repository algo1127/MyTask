package com.algo1127.mytask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.algo1127.mytask.data.CompletionRecord
import com.algo1127.mytask.data.MyTaskDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.algo1127.mytask.ui.models.TimePreference
import java.time.LocalDate
import java.time.LocalDateTime

data class DashboardUiState(
    val tasks: List<TaskItem> = emptyList(),
    val persistentTasks: List<com.algo1127.mytask.ui.models.Task> = emptyList(),
    val events: List<EventItem> = emptyList(),
    val taskCounts: Map<LocalDate, Int> = emptyMap(),
    val eventCounts: Map<LocalDate, Int> = emptyMap(),
    val badgeCounts: Map<Int, Int> = emptyMap(), // PageIndex -> Count
    val calendarReminders: List<TaskItem> = emptyList(),
    val eventReminders: List<EventItem> = emptyList(),
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
    private var lastDate: LocalDate? = null

    val uiState: StateFlow<DashboardUiState> = combine(_selectedDate, _refreshTrigger) { date, trigger -> date to trigger }
        .flatMapLatest { (date, _) ->
            flow {
                // Smooth Transition Fix: Never wipe out the screen logic with an empty loading state.
                // Keep the current state visible and fetch fresh data seamlessly in the background.
                lastDate = date
                
                // 1. Fetch items occurring on the selected date for main tabs
                val dailyData = calendarReader.getItemsForDate(date)
                val calendarTasks = dailyData.first
                val calendarEvents = dailyData.second
                
                // 2. Fetch items for a wider range to catch all reminders/alerts for this day
                val widerData = calendarReader.getItemsForRange(date.minusDays(7), date.plusDays(7))
                val calendarReminders = widerData.first.filter { it.isReminder && it.date == date }
                val eventReminders = widerData.second.filter { it.reminderDateTime?.toLocalDate() == date }

                val pTasks = database.taskDao().getAllTasks()
                
                // Calculate correct badge counts for the selected date
                val reminderCount = calendarReminders.size + 
                        pTasks.count { it.reminderDateTime?.toLocalDate() == date } +
                        eventReminders.size
                
                val taskCount = pTasks.count { it.focusState != com.algo1127.mytask.ui.models.FocusState.Archived } +
                        calendarTasks.count { !it.isReminder }
                
                val badgeCounts = mapOf(
                    0 to reminderCount,
                    1 to taskCount,
                    2 to calendarEvents.size
                )

                emit(DashboardUiState(
                    tasks = calendarTasks,
                    persistentTasks = pTasks,
                    events = calendarEvents,
                    calendarReminders = calendarReminders,
                    eventReminders = eventReminders,
                    badgeCounts = badgeCounts,
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
                
                val pTasks = database.taskDao().getAllTasks().filter { it.focusState != com.algo1127.mytask.ui.models.FocusState.Archived }
                
                val tCounts = data.first.groupBy { it.date }.mapValues { it.value.size }.toMutableMap()
                // Add persistent tasks with due dates to counts
                pTasks.forEach { pt ->
                    pt.dueDate?.let { d ->
                        tCounts[d] = (tCounts[d] ?: 0) + 1
                    }
                }
                
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

    fun updateTaskItem(task: TaskItem) {
        viewModelScope.launch {
            val success = CalendarUtils.updateTaskInCalendar(getApplication(), task)
            if (success) {
                getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.onTaskUpdated(task)
                refresh()
            }
        }
    }

    fun addPersistentTask(
        title: String,
        description: String?,
        category: TaskCategory,
        dueDate: LocalDate?,
        estimatedDuration: java.time.Duration?,
        reminderDateTime: LocalDateTime? = null
    ) {
        viewModelScope.launch {
            val task = com.algo1127.mytask.ui.models.Task(
                title = title,
                description = description ?: "",
                category = category,
                startDate = LocalDate.now(),
                dueDate = dueDate,
                estimatedEffort = estimatedDuration,
                progress = 0f,
                isReminder = false,
                reminderDateTime = reminderDateTime
            )
            taskDao.insertTask(task)
            getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.scheduleFlexible(task)

            // If an explicit reminder date-time is set, create a real matching dedicated Reminder item too!
            if (reminderDateTime != null) {
                val timeString = String.format(java.util.Locale.US, "%02d:%02d", reminderDateTime.toLocalTime().hour, reminderDateTime.toLocalTime().minute)
                val reminderTaskItem = TaskItem(
                    title = "Reminder: $title",
                    time = timeString,
                    category = category,
                    date = reminderDateTime.toLocalDate(),
                    isReminder = true,
                    notes = description ?: ""
                )
                CalendarUtils.addTaskToCalendar(getApplication(), reminderTaskItem)
            }

            refresh()
        }
    }

    fun updatePersistentTask(task: com.algo1127.mytask.ui.models.Task) {
        viewModelScope.launch {
            taskDao.insertTask(task)
            getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.scheduleFlexible(task)
            refresh()
        }
    }

    fun updateEvent(item: EventItem) {
        viewModelScope.launch {
            val success = CalendarUtils.updateEventInCalendar(getApplication(), item)
            if (success) {
                val entity = com.algo1127.mytask.ui.models.EventItem(
                    id = item.id,
                    title = item.title,
                    startTime = item.startTime,
                    endTime = item.endTime,
                    location = item.location,
                    date = item.date,
                    notes = item.notes,
                    reminderDateTime = item.reminderDateTime
                )
                eventDao.insertEvent(entity)
                getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.onEventUpdated(item)
                refresh()
            }
        }
    }

    fun removeReminderFromTask(task: com.algo1127.mytask.ui.models.Task) {
        viewModelScope.launch {
            val updated = task.copy(reminderDateTime = null)
            database.taskDao().insertTask(updated)
            refresh()
        }
    }

    fun removeReminderFromEvent(event: EventItem) {
        viewModelScope.launch {
            val updated = event.copy(reminderDateTime = null)
            updateEvent(updated)
        }
    }

    fun deletePersistentTask(taskId: Long) {
        viewModelScope.launch {
            taskDao.deleteTaskById(taskId)
            getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.onTaskDeleted(taskId)
            refresh()
        }
    }

    fun togglePersistentTaskCompletion(taskId: Long, isDone: Boolean) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedSubtasks = task.subtasks.map { it.copy(isCompleted = isDone) }
            updateParentTaskProgress(task, updatedSubtasks)
        }
    }

    fun toggleSubtaskCompletion(taskId: Long, subtaskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedSubtasks = task.subtasks.map { 
                if (it.id == subtaskId) it.copy(isCompleted = isCompleted) else it
            }
            updateParentTaskProgress(task, updatedSubtasks)
        }
    }

    fun addSubtask(
        taskId: Long, 
        title: String,
        description: String?,
        category: TaskCategory,
        dueDate: LocalDate?,
        timePreference: TimePreference,
        reminderDateTime: LocalDateTime?,
        estimatedEffort: java.time.Duration?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val newSubtask = com.algo1127.mytask.ui.models.Subtask(
                title = title.trim(),
                description = description ?: "",
                category = category,
                dueDate = dueDate,
                timePreference = timePreference,
                reminderDateTime = reminderDateTime,
                estimatedEffort = estimatedEffort
            )
            val updatedSubtasks = task.subtasks + newSubtask
            updateParentTaskProgress(task, updatedSubtasks)
        }
    }

    fun updateSubtask(taskId: Long, updatedSubtask: com.algo1127.mytask.ui.models.Subtask) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedSubtasks = task.subtasks.map { 
                if (it.id == updatedSubtask.id) updatedSubtask else it 
            }
            updateParentTaskProgress(task, updatedSubtasks)
        }
    }

    fun deleteSubtask(taskId: Long, subtaskId: Long) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
            updateParentTaskProgress(task, updatedSubtasks)
        }
    }

    private suspend fun updateParentTaskProgress(parentTask: com.algo1127.mytask.ui.models.Task, subtasks: List<com.algo1127.mytask.ui.models.Subtask>) {
        val total = subtasks.size
        val completed = subtasks.count { it.isCompleted }
        val progressVal = if (total > 0) completed.toFloat() / total else 0.0f
        
        // Sum durations of all subtasks that are NOT yet completed
        val totalRemainingMinutes = subtasks.filter { !it.isCompleted }.sumOf { it.estimatedEffort?.toMinutes() ?: 0L }
        val newDuration = if (total > 0) {
             if (totalRemainingMinutes > 0) java.time.Duration.ofMinutes(totalRemainingMinutes) else null
        } else {
            parentTask.estimatedEffort
        }

        val updated = parentTask.copy(
            subtasks = subtasks,
            progress = progressVal,
            estimatedEffort = newDuration
        )
        taskDao.insertTask(updated)
        refresh()
    }

    fun addEvent(
        title: String,
        date: LocalDate,
        startTime: String,
        endTime: String,
        location: String,
        notes: String,
        reminderDateTime: LocalDateTime? = null
    ) {
        viewModelScope.launch {
            val event = com.algo1127.mytask.ui.models.EventItem(
                title = title,
                date = date,
                startTime = startTime,
                endTime = endTime,
                location = location,
                notes = notes,
                reminderDateTime = reminderDateTime
            )
            eventDao.insertEvent(event)
            
            // Sync with device Calendar contract provider first!
            val calendarId = CalendarUtils.addEventToCalendar(getApplication(), EventItem(title, startTime, endTime, location, notes, date, event.id, reminderDateTime))
            
            // If a reminder is set for the event, create a dedicated Reminder item!
            if (reminderDateTime != null) {
                val timeString = String.format(java.util.Locale.US, "%02d:%02d", reminderDateTime.toLocalTime().hour, reminderDateTime.toLocalTime().minute)
                val reminderTaskItem = TaskItem(
                    title = "Event Reminder: $title",
                    time = timeString,
                    category = TaskCategory.Personal,
                    date = reminderDateTime.toLocalDate(),
                    isReminder = true,
                    notes = "Event starts at $startTime at $location"
                )
                CalendarUtils.addTaskToCalendar(getApplication(), reminderTaskItem)
            }

            // Sync with NotifAi
            getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.onEventUpdated(EventItem(title, startTime, endTime, location, notes, date, event.id, reminderDateTime))
            refresh()
        }
    }

    fun deleteEvent(event: EventItem) {
        viewModelScope.launch {
            val success = CalendarUtils.deleteEventFromCalendar(getApplication(), event.id)
            if (success) {
                getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.onEventDeleted(event.id)
                refresh()
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val success = CalendarUtils.deleteEventFromCalendar(getApplication(), taskId)
            if (success) {
                getApplication<com.algo1127.mytask.MyTaskApplication>().notifAi.onTaskDeleted(taskId)
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
