package com.algo1127.mytask.NotifAi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.algo1127.mytask.NotifAi.persistence.Persistence
import java.time.LocalDate

class DailySummaryWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tomorrow = LocalDate.now().plusDays(1)

        // Pull tomorrow's events from persistence
        // You'll need to add getEvents() to your Persistence class (see note below)
        val events = try {
            Persistence(context).getEvents()
                .filter { it.date == tomorrow }
                .sortedBy { it.startTime }
        } catch (e: Exception) { emptyList() }

        setupChannel()

        val (title, body) = when {
            events.isEmpty() -> "Tomorrow" to "No events — enjoy the free day 🌿"
            events.size == 1 -> {
                val e = events[0]
                "📅 Tomorrow: 1 event" to "${e.startTime} · ${e.title}"
            }
            else -> {
                val first = events[0]
                "📅 Tomorrow: ${events.size} events" to
                        "${first.startTime} · ${first.title} and ${events.size - 1} more"
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Expand to show all events if more than 1
        if (events.size > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()
            events.take(6).forEach { inboxStyle.addLine("${it.startTime}  ${it.title}") }
            if (events.size > 6) inboxStyle.setSummaryText("+${events.size - 6} more")
            builder.setStyle(inboxStyle)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, builder.build())

        // Self-reschedule for the next night
        EventScheduler.scheduleDailySummary(context)
        return Result.success()
    }

    private fun setupChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily Summary", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    companion object {
        const val CHANNEL_ID = "daily_summary"
        const val NOTIF_ID   = 88888
    }
}