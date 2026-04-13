package com.algo1127.mytask.NotifAi

import android.content.Context
import androidx.work.*
import com.algo1127.mytask.ui.EventItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object EventScheduler {

    // Minutes-before thresholds for countdown
    val COUNTDOWN_MINUTES = listOf(60L, 30L, 20L, 10L, 5L)

    // ── Event reminders ───────────────────────────────────────────────

    fun scheduleReminders(context: Context, event: EventItem) {
        cancelReminders(context, event.id)  // always cancel first (idempotent)

        val startTime = runCatching { LocalTime.parse(event.startTime) }.getOrNull() ?: return
        val eventStart = LocalDateTime.of(event.date, startTime)
        val now = LocalDateTime.now()

        COUNTDOWN_MINUTES.forEach { minsBefore ->
            val fireAt = eventStart.minusMinutes(minsBefore)
            val delayMs = ChronoUnit.MILLIS.between(now, fireAt)
            if (delayMs <= 0) return@forEach  // already in the past

            val data = workDataOf(
                "eventId"       to event.id,
                "eventTitle"    to event.title,
                "eventTime"     to event.startTime,
                "minutesBefore" to minsBefore
            )

            val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tagFor(event.id))
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }

    fun cancelReminders(context: Context, eventId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagFor(eventId))
    }

    private fun tagFor(eventId: Long) = "event_reminders_$eventId"

    // ── Daily 9 PM summary ────────────────────────────────────────────

    /**
     * Call once on app start — self-reschedules every night.
     */
    fun scheduleDailySummary(context: Context) {
        val now = LocalDateTime.now()
        var fireAt = now.toLocalDate().atTime(21, 0)
        if (!now.isBefore(fireAt.minusMinutes(1))) {
            fireAt = fireAt.plusDays(1)  // too close or past → schedule tomorrow
        }
        val delayMs = ChronoUnit.MILLIS.between(now, fireAt)

        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag("daily_summary")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "daily_summary",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}