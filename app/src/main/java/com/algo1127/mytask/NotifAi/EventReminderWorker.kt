package com.algo1127.mytask.NotifAi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class EventReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId      = inputData.getLong("eventId", -1)
        val title        = inputData.getString("eventTitle") ?: return Result.failure()
        val time         = inputData.getString("eventTime") ?: ""
        val minsBefore   = inputData.getLong("minutesBefore", 0)

        if (eventId < 0) return Result.failure()

        setupChannel()

        val (notifTitle, body, highPriority) = when (minsBefore) {
            60L -> Triple("📅 In 1 hour",    "$title · $time",            false)
            30L -> Triple("⏰ 30 min",        "$title coming up",          false)
            20L -> Triple("🔔 20 minutes",    "Get ready for $title",      false)
            10L -> Triple("⚡ 10 minutes!",   "$title starts very soon",   true)
            5L  -> Triple("🚨 5 minutes!",    "$title — time to go!",      true)
            else-> Triple("Reminder",         title,                       false)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notifTitle)
            .setContentText(body)
            .setPriority(
                if (highPriority) NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .apply { if (highPriority) setVibrate(longArrayOf(0, 250, 150, 250)) }
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Unique notification ID per event+offset so they don't overwrite each other
        manager.notify("evt_${eventId}_$minsBefore".hashCode(), notification)
        return Result.success()
    }

    private fun setupChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Event Reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { enableVibration(true) }
        )
    }

    companion object { const val CHANNEL_ID = "event_reminders" }
}