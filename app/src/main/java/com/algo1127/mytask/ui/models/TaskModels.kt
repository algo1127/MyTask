package com.algo1127.mytask.ui.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ==================== NEW TASK SYSTEM ====================
enum class Priority { Low, Medium, High, Critical }
enum class FocusState { Active, Paused, Archived }
enum class VerificationStatus { Pending, Verified, Flagged }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: Long = System.nanoTime(),
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
@Entity(tableName = "reminders")
data class ReminderItem(
    val title: String,
    val time: String,
    val category: com.algo1127.mytask.ui.TaskCategory, // ✅ Use existing
    val date: LocalDate,
    @PrimaryKey val id: Long = System.nanoTime(),
    val done: Boolean = false
)

@Entity(tableName = "events")
data class EventItem(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val date: LocalDate,
    @PrimaryKey val id: Long = System.nanoTime(),
    val notes: String = ""
)

@Entity(tableName = "countdowns")
data class CountdownItem(
    @PrimaryKey val id: Long = System.nanoTime(),
    val title: String,
    val optionalTitle: String? = null,
    val targetDateTime: LocalDateTime,
    val color: Int = 0xFF4DFFD2.toInt(), // Default Teal
    val linkedItemId: Long? = null,
    val linkedItemType: String? = null, // "TASK", "EVENT"
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val remindWhenUp: Boolean = false
)
// ✅ REMOVED: TaskCategory enum (use the one in ui/ package)