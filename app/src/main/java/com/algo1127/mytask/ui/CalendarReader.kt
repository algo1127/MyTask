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

    fun getItemsForRange(start: LocalDate, end: LocalDate): Pair<List<TaskItem>, List<EventItem>> {
        if (!hasCalendarPermission()) return Pair(emptyList(), emptyList())

        val tasks = mutableListOf<TaskItem>()
        val events = mutableListOf<EventItem>()

        val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(uriBuilder, startMillis)
        android.content.ContentUris.appendId(uriBuilder, endMillis)
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
                
                val itemDate = Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault()).toLocalDate()
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
                    // Strip markers from notes
                    val cleanNotes = description
                        .replace(Regex("\\|\\|TYPE:(TASK|REMINDER)"), "")
                        .replace(Regex("\\|\\|CATEGORY:.*?(\\|\\||$)"), "")
                        .replace("||URGENT:TRUE", "")
                        .replace("||IMPORTANT:TRUE", "")
                        .trim()

                    tasks.add(
                        TaskItem(
                            title = title,
                            time = startTime,
                            category = category,
                            date = itemDate,
                            isReminder = isReminder,
                            id = eventId,
                            isUrgent = isUrgent,
                            isImportant = isImportant,
                            notes = cleanNotes
                        )
                    )
                } else {
                    // Strip the TYPE:EVENT marker from notes
                    var cleanNotes = description.replace("||TYPE:EVENT", "").trim()
                    var finalLocation = location

                    // Fallback: If location field is empty, try to extract it from the old format in notes
                    if (finalLocation.isBlank() && cleanNotes.contains("Location:")) {
                        val regex = Regex("Location: (.*?)(?:\\|\\||$)")
                        val match = regex.find(cleanNotes)
                        match?.let {
                            finalLocation = it.groupValues[1].trim()
                            cleanNotes = cleanNotes.replace(it.value, "").trim()
                        }
                    }

                    events.add(
                        EventItem(
                            title = title,
                            startTime = startTime,
                            endTime = endTime,
                            location = finalLocation,
                            notes = cleanNotes,
                            date = itemDate,
                            id = eventId
                        )
                    )
                }
            }
        }
        return Pair(tasks, events)
    }

    fun getItemsForDate(date: LocalDate): Pair<List<TaskItem>, List<EventItem>> {
        val result = getItemsForRange(date, date)
        return Pair(result.first.filter { it.date == date }, result.second.filter { it.date == date })
    }

    // For backwards compatibility
    fun getEventsForDate(date: LocalDate): List<EventItem> {
        return getItemsForDate(date).second
    }

    private fun extractCategory(description: String): TaskCategory {
        return when {
            description.contains("||CATEGORY:Study") -> TaskCategory.Study
            description.contains("||CATEGORY:Personal") -> TaskCategory.Personal
            description.contains("||CATEGORY:Design") -> TaskCategory.Design
            description.contains("||CATEGORY:Work") -> TaskCategory.Work
            // Legacy fallbacks
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