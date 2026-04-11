package com.algo1127.mytask.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import java.time.LocalDate

data class TaskItem(
     val title: String,
     val time: String,
    val category: TaskCategory,
     val date: LocalDate,
     val isReminder: Boolean = false,  // ← ADD THIS
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

enum class TaskCategory(
    val icon: ImageVector,
    val color: Color,
    val label: String
) {
    Design(Icons.Default.Brush, Color(0xFFFFBD2E), "Design"),
    Study(Icons.Default.School, Color(0xFF4DFFD2), "Study"),
    Personal(Icons.Default.Favorite, Color(0xFFB57BFF), "Personal"),
    Work(Icons.Default.Business, Color(0xFF3FC3F7), "Work")
}
