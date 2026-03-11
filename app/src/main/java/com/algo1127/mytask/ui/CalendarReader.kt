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

    // ✅ Returns BOTH tasks and events separately
    private val idGenerator = java.util.concurrent.atomic.AtomicLong(1000000L)

    fun getItemsForDate(date: LocalDate): Pair<List<TaskItem>, List<EventItem>> {
        if (!hasCalendarPermission()) {
            android.util.Log.e("CalendarReader", "No calendar permission!")
            return Pair(emptyList(), emptyList())
        }

        val tasks = mutableListOf<TaskItem>()
        val events = mutableListOf<EventItem>()
        val seenIds = mutableSetOf<String>()  // ✅ Prevent duplicates

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION
        )
        val selection = "((${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} < ?))"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val calendarId = cursor.getString(
                    cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                )

                // ✅ Skip if we've seen this calendar ID
                if (seenIds.contains(calendarId)) continue
                seenIds.add(calendarId)

                val title = cursor.getString(
                    cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                )
                val startTime = getTimestampTime(
                    cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                )
                val endTime = getTimestampTime(
                    cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                )
                val locationIndex = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val location = if (locationIndex >= 0) cursor.getString(locationIndex) ?: "" else ""
                val descriptionIndex = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val description = if (descriptionIndex >= 0) cursor.getString(descriptionIndex) ?: "" else ""

                // ✅ Check marker to determine type
                if (description.contains("TYPE:TASK")) {
                    val category = extractCategory(description)
                    tasks.add(TaskItem(title, startTime, category, date, id = idGenerator.getAndIncrement()))
                } else {
                    events.add(EventItem(title, startTime, endTime, location, date, id = idGenerator.getAndIncrement()))
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