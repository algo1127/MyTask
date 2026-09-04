
package com.algo1127.mytask.ui

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.time.DayOfWeek

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.collections.map

object CalendarUtils {
    fun addTaskToCalendar(context: Context, task: TaskItem, rrule: String? = null): Long? {
        return try {
            val time = LocalTime.parse(task.time)
            val dateTime = LocalDateTime.of(task.date, time)
            val startMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val calId = getDefaultCalendarId(context)
            android.util.Log.d("CalendarUtils", "Using Calendar ID: $calId for task ${task.title}")

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.TITLE, task.title)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 30 * 60 * 1000) // 30 min duration
                put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)

                val typeMarker = if (task.isReminder) "REMINDER" else "TASK"
                var description = if (task.notes.isNotBlank()) "${task.notes}||TYPE:$typeMarker" else "||TYPE:$typeMarker"
                description += "||CATEGORY:${task.category.label}"
                if (task.isUrgent) description += "||URGENT:TRUE"
                if (task.isImportant) description += "||IMPORTANT:TRUE"
                put(CalendarContract.Events.DESCRIPTION, description)

                put(CalendarContract.Events.HAS_ALARM, 1)
                if (rrule != null) {
                    put(CalendarContract.Events.RRULE, rrule)
                }
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri == null) {
                android.util.Log.e("CalendarUtils", "ContentResolver.insert returned NULL")
                return null
            }
            
            val eventId = uri.lastPathSegment?.toLong()
            android.util.Log.d("CalendarUtils", "Created Event ID: $eventId")
            
            // Add reminder (15 minutes before)
            if (eventId != null) {
                try {
                    val reminderValues = ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, eventId)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                        put(CalendarContract.Reminders.MINUTES, 15)
                    }
                    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                } catch (re: Exception) {
                    android.util.Log.w("CalendarUtils", "Failed to add reminder, but event was created: ${re.message}")
                }
            }

            return eventId
        } catch (e: Exception) {
            android.util.Log.e("CalendarUtils", "Error adding task to calendar: ${e.message}", e)
            null
        }
    }

    fun addEventToCalendar(context: Context, event: EventItem, rrule: String? = null): Long? {
        return try {
            val startTime = LocalTime.parse(event.startTime)
            val endTime = LocalTime.parse(event.endTime)
            val startDateTime = LocalDateTime.of(event.date, startTime)
            val endDateTime = LocalDateTime.of(event.date, endTime)
            val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId(context))
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                
                // We use ||TYPE:EVENT to distinguish our app's events
                val description = if (event.notes.isNotBlank()) "${event.notes}||TYPE:EVENT" else "||TYPE:EVENT"
                put(CalendarContract.Events.DESCRIPTION, description)
                
                if (rrule != null) {
                    put(CalendarContract.Events.RRULE, rrule)
                }
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLong()
        } catch (e: Exception) {
            android.util.Log.e("CalendarUtils", "Error adding event to calendar: ${e.message}", e)
            null
        }
    }

    fun updateTaskInCalendar(context: Context, task: TaskItem): Boolean {
        return try {
            val time = LocalTime.parse(task.time)
            val dateTime = LocalDateTime.of(task.date, time)
            val startMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, task.title)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 30 * 60 * 1000)
                
                val typeMarker = if (task.isReminder) "REMINDER" else "TASK"
                var description = if (task.notes.isNotBlank()) "${task.notes}||TYPE:$typeMarker" else "||TYPE:$typeMarker"
                description += "||CATEGORY:${task.category.label}"
                if (task.isUrgent) description += "||URGENT:TRUE"
                if (task.isImportant) description += "||IMPORTANT:TRUE"
                put(CalendarContract.Events.DESCRIPTION, description)
            }

            val uri = android.content.ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, task.id)
            val rows = context.contentResolver.update(uri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            android.util.Log.e("CalendarUtils", "Error updating task: ${e.message}")
            false
        }
    }

    fun deleteEventFromCalendar(context: Context, eventId: Long): Boolean {
        return try {
            val uri = android.content.ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            android.util.Log.e("CalendarUtils", "Error deleting event: ${e.message}")
            false
        }
    }

    private fun getDefaultCalendarId(context: Context): Long {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )
        
        // 1. Log all available calendars for debugging
        try {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { cursor ->
                android.util.Log.d("CalendarUtils", "--- Available Calendars ---")
                while (cursor.moveToNext()) {
                    android.util.Log.d("CalendarUtils", "ID: ${cursor.getLong(0)}, Name: ${cursor.getString(2)}, Account: ${cursor.getString(1)}")
                }
                android.util.Log.d("CalendarUtils", "---------------------------")
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarUtils", "Failed to query calendars: ${e.message}")
        }

        // 2. Try to find the best candidate (Primary, or matching account)
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                android.util.Log.d("CalendarUtils", "Selected Calendar ID: $id")
                return id
            }
        }
        
        // 3. Last fallback: any calendar at all
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        android.util.Log.e("CalendarUtils", "No calendars found. Returning default ID 1.")
        return 1L
    }

    fun generateRRule(frequency: String, untilDate: LocalDate? = null, interval: Int = 1, selectedDays: Set<DayOfWeek>? = null): String {
        val freq = when (frequency) {
            "Daily" -> "DAILY"
            "Weekly" -> "WEEKLY"
            "Monthly" -> "MONTHLY"
            "Yearly" -> "YEARLY"
            else -> "DAILY"
        }
        
        var rrule = "FREQ=$freq;INTERVAL=$interval"
        
        if (frequency == "Weekly" && selectedDays != null && selectedDays.isNotEmpty()) {
            val days = selectedDays.joinToString(",") {
                it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase().substring(0, 2)
            }
            rrule += ";BYDAY=$days"
        }
        
        if (untilDate != null) {
            val until = untilDate.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'235959'Z'"))
            rrule += ";UNTIL=$until"
        }
        
        return rrule
    }
}


