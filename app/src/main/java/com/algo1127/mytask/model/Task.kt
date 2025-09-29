package com.algo1127.mytask.model

data class Task(
    val id: Int,
    val title: String,
    val time: String, // e.g. "09:00 AM"
    val type: TaskType
)

enum class TaskType {
    REMINDER,
    EVENT,
    DEADLINE
}
