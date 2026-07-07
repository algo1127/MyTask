package com.algo1127.mytask.ui

import android.content.Context
import android.provider.CalendarContract
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

class CalendarReader(private val context: Context) {

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getItemsForDate(date: LocalDate): Pair<List<TaskItem>, List<EventItem>> {
        if (!hasCalendarPermission()) {
            android.util.Log.e("CalendarReader", "No calendar permission!")
            return Pair(emptyList(), emptyList())
        }

        val tasks = mutableListOf<TaskItem>()
        val events = mutableListOf<EventItem>()

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // ✅ Use Instances table to correctly expand repeating events
        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(uriBuilder, startOfDay)
        android.content.ContentUris.appendId(uriBuilder, endOfDay)
        val uri = uriBuilder.build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION
        )

        context.contentResolver.query(uri, projection, null, null, CalendarContract.Instances.BEGIN + " ASC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val eventId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: "Untitled"
                val begin = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN))
                val end = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END))
                val startTime = getTimestampTime(begin)
                val endTime = getTimestampTime(end)
                
                val locationIndex = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val location = if (locationIndex >= 0) cursor.getString(locationIndex) ?: "" else ""
                val descriptionIndex = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val description = if (descriptionIndex >= 0) cursor.getString(descriptionIndex) ?: "" else ""

                val isTask = description.contains("TYPE:TASK")
                val isReminder = description.contains("TYPE:REMINDER")
                val isUrgent = description.contains("URGENT:TRUE")
                val isImportant = description.contains("IMPORTANT:TRUE")

                if (isTask || isReminder) {
                    val category = extractCategory(description)
                    tasks.add(
                        TaskItem(
                            title = title,
                            time = startTime,
                            category = category,
                            date = date,
                            isReminder = isReminder,
                            id = eventId,
                            isUrgent = isUrgent,
                            isImportant = isImportant
                        )
                    )
                } else {
                    events.add(
                        EventItem(
                            title = title,
                            startTime = startTime,
                            endTime = endTime,
                            location = location,
                            notes = description,
                            date = date,
                            id = eventId // ✅ Use real Event ID
                        )
                    )
                }
            }
        }

        android.util.Log.d("CalendarReader", "Found ${tasks.size} tasks, ${events.size} events for $date")
        return Pair(tasks, events)
    }

    // For backwards compatibility
    fun getEventsForDate(date: LocalDate): List<EventItem> {
        return getItemsForDate(date).second
    }

    private fun extractCategory(description: String): TaskCategory {
        return when {
            description.contains("Category: Study") -> TaskCategory.Study
            description.contains("Category: Personal") -> TaskCategory.Personal
            description.contains("Category: Design") -> TaskCategory.Design
            else -> TaskCategory.Work
        }
    }

    private fun getTimestampTime(epochMs: Long): String {
        val instant = Instant.ofEpochMilli(epochMs)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return localTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
}