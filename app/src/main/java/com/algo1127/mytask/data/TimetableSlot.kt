package com.algo1127.mytask.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.algo1127.mytask.ui.TaskCategory
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(tableName = "timetable_slots")
data class TimetableSlot(
    @PrimaryKey val id: Long = System.nanoTime(),
    val subjectName: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String = "",
    val teacher: String = "",
    val colorHex: Long = 0xFF4DFFD2, // Default Teal accent
    val category: TaskCategory = TaskCategory.Study
)
