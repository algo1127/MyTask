
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
    fun addTaskToCalendar(context: Context, task: TaskItem): Long? {
        return try {
            val time = LocalTime.parse(task.time)
            val dateTime = LocalDateTime.of(task.date, time)
            val startMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId(context))
                put(CalendarContract.Events.TITLE, task.title)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 60 * 60 * 1000) // 1 hour duration
                put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
                put(CalendarContract.Events.DESCRIPTION, "Category: ${task.category.label}")
                put(CalendarContract.Events.HAS_ALARM, 1) // Enable reminder
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLong()

            // Add reminder (15 minutes before)
            if (eventId != null) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    put(CalendarContract.Reminders.MINUTES, 15)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            eventId
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
                put(CalendarContract.Events.DESCRIPTION, "Location: ${event.location}")
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

    private fun getDefaultCalendarId(context: Context): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.IS_PRIMARY} = 1",
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        // Fallback: Get first available calendar
        val fallbackCursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null
        )
        fallbackCursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return 1L // Default to 1 if no calendar found
    }

    fun generateRRule(frequency: Frequency, untilDate: LocalDate, selectedDays: Set<DayOfWeek>? = null): String {
        val until = untilDate.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'235959'Z'"))
        return when (frequency) {
            Frequency.Daily -> "FREQ=DAILY;UNTIL=$until"
            Frequency.Weekly -> {
                val days = selectedDays?.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase() }
                    ?.joinToString(",") ?: ""
                "FREQ=WEEKLY;UNTIL=$until;BYDAY=$days"
            }
            Frequency.Monthly -> "FREQ=MONTHLY;UNTIL=$until;BYMONTHDAY=${untilDate.dayOfMonth}"
        }
    }
}


