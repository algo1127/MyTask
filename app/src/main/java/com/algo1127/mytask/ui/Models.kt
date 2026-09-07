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
    val isReminder: Boolean = false,
    val id: Long = System.nanoTime(),
    val done: Boolean = false,
    val isUrgent: Boolean = false,
    val isImportant: Boolean = false,
    val notes: String = ""
)

data class EventItem(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val notes: String = "",
    val date: LocalDate,
    val id: Long = System.nanoTime()
)

data class TaskCategory(
    val id: Long = 0,
    val label: String,
    val iconName: String,
    val colorHex: String,
    val position: Int = 0
) {
    val icon: ImageVector get() = CategoryUtils.getIcon(iconName)
    val color: Color get() = CategoryUtils.hexToColor(colorHex)

    companion object {
        // Default instances for compatibility and initial state
        val Design = TaskCategory(1, "Design", "Brush", "#FFFFBD2E", 0)
        val Study = TaskCategory(2, "Study", "School", "#FF4DFFD2", 1)
        val Personal = TaskCategory(3, "Personal", "Favorite", "#FFB57BFF", 2)
        val Work = TaskCategory(4, "Work", "Business", "#FF3FC3F7", 3)

        fun values() = listOf(Design, Study, Personal, Work)
        
        fun valueOf(name: String): TaskCategory {
            return values().find { it.label == name } ?: Work
        }
    }
}

data class AiKnowledge(
    val trustScore: Float,
    val dataPoints: Int,
    val effectiveness: Float,
    val focusEntropy: Float,
    val mood: String,
    val categoryInsights: List<CategoryInsight>
)

data class CategoryInsight(
    val label: String,
    val iconName: String,
    val colorHex: String,
    val summary: String,
    val hourlyProfile: FloatArray // 24 floats (0-1)
)
