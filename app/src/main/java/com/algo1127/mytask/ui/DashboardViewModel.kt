package com.algo1127.mytask.ui

import androidx.lifecycle.ViewModel
import com.algo1127.mytask.model.Task
import com.algo1127.mytask.model.TaskType

class DashboardViewModel : ViewModel() {

    // Sample data for now
    val todayTasks = listOf(
        Task(1, "Doctor’s Appointment", "09:00 AM", TaskType.EVENT),
        Task(2, "Finish homework", "02:00 PM", TaskType.DEADLINE),
        Task(3, "Buy groceries", "06:00 PM", TaskType.REMINDER)
    )
}
