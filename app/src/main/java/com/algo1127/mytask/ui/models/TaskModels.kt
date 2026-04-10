package com.algo1127.mytask.ui.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ==================== NEW TASK SYSTEM ====================
enum class Priority { Low, Medium, High, Critical }
enum class FocusState { Active, Paused, Archived }
enum class VerificationStatus { Pending, Verified, Flagged }

data class Task(
    val id: Long = System.nanoTime(),
    val title: String,
    val description: String = "",
    val startDate: LocalDate,
    val dueDate: LocalDate?,          // null = open-ended
    val priority: Priority = Priority.Medium,
    val progress: Float = 0f,         // 0.0 → 1.0
    val subtasks: List<Subtask> = emptyList(),
    val estimatedEffort: Duration? = null,
    val category: com.algo1127.mytask.ui.TaskCategory, // ✅ Use existing TaskCategory
    val focusState: FocusState = FocusState.Active,
    val timePreference: TimePreference = TimePreference.Fixed(LocalTime.NOON),
    val verificationStatus: VerificationStatus = VerificationStatus.Pending,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class Subtask(
    val id: Long = System.nanoTime(),
    val title: String,
    val isCompleted: Boolean = false
)

// Flexible timing preferences
sealed class TimePreference {
    data class Fixed(val time: LocalTime) : TimePreference()
    data object LaterToday : TimePreference()
    data object Tomorrow : TimePreference()
    data class Window(val startHour: Int, val endHour: Int) : TimePreference()
    data object AiDecide : TimePreference()
}

// ==================== EXISTING SYSTEMS (Renamed for clarity) ====================
data class ReminderItem(
    val title: String,
    val time: String,
    val category: com.algo1127.mytask.ui.TaskCategory, // ✅ Use existing
    val date: LocalDate,
    val id: Long = System.nanoTime(),
    val done: Boolean = false
)

data class EventItem(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val date: LocalDate,
    val id: Long = System.nanoTime()
)
// ✅ REMOVED: TaskCategory enum (use the one in ui/ package)