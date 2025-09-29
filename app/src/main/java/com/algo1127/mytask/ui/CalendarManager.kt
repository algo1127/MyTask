package com.algo1127.mytask.ui

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class CalendarManager(private val context: Context) {
    // Read events for a specific date
    fun getEventsForDate(date: LocalDate): List<EventItem> {
        val events = mutableListOf<EventItem>()
        val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTEND} <= ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: ""
                val startMillis = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                val endMillis = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                val location = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)) ?: ""

                val startTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(startMillis),
                    ZoneId.systemDefault()
                ).toLocalTime().toString()
                val endTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(endMillis),
                    ZoneId.systemDefault()
                ).toLocalTime().toString()

                events.add(EventItem(title, startTime, endTime, location, date))
            }
        }

        return events
    }

    // Write an event to the calendar
    fun insertEvent(event: EventItem): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DTSTART, event.date.atTime(LocalTime.parse(event.startTime)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND, event.date.atTime(LocalTime.parse(event.endTime)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.CALENDAR_ID, 1) // Default calendar ID; query Calendars for user calendars
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            put(CalendarContract.Events.ALL_DAY, false)
            put(CalendarContract.Events.HAS_ALARM, false) // Add alarm if needed
        }

        val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return uri?.lastPathSegment?.toLongOrNull()
    }

    // Get default calendar ID
    fun getDefaultCalendarId(): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf("com.google"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            }
        }
        return 1L // Fallback
    }
}