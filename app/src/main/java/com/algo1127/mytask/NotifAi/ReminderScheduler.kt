package com.algo1127.mytask.NotifAi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.algo1127.mytask.ui.models.CountdownItem
import com.algo1127.mytask.ui.models.Task
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(task: Task) {
        val baseTime = task.reminderDateTime ?: task.dueDate?.atTime(
            (task.timePreference as? com.algo1127.mytask.ui.models.TimePreference.Fixed)?.time 
            ?: java.time.LocalTime.of(9, 0)
        ) ?: return

        // For explicit reminders or tasks with a specific reminder time, schedule multiple pings.
        val hasExplicitReminder = task.isReminder || task.reminderDateTime != null
        val offsets = if (hasExplicitReminder) listOf(60, 30, 15, 0) else listOf(0)

        offsets.forEach { minsBefore ->
            val targetTime = baseTime.minusMinutes(minsBefore.toLong())
            val now = LocalDateTime.now()
            
            if (targetTime.isBefore(now)) {
                return@forEach
            }

            val intent = Intent(context, Receiver::class.java).apply {
                action = "com.algo1127.mytask.ACTION_TRIGGER_REMINDER"
                putExtra("taskId", task.id)
                putExtra("taskTitle", task.title)
                putExtra("minutesBefore", minsBefore)
            }

            val requestCode = (task.id.toInt() * 100) + minsBefore
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val triggerAt = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
            Log.d("ReminderScheduler", "Scheduled alarm for ${task.title} at $targetTime (offset: $minsBefore)")
        }
    }

    fun cancel(taskId: Long) {
        listOf(60, 30, 15, 0).forEach { minsBefore ->
            val intent = Intent(context, Receiver::class.java).apply {
                action = "com.algo1127.mytask.ACTION_TRIGGER_REMINDER"
            }
            val requestCode = (taskId.toInt() * 100) + minsBefore
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleEventReminder(event: com.algo1127.mytask.ui.models.EventItem) {
        val baseTime = event.reminderDateTime ?: return
        val offsets = listOf(60, 30, 15, 0)

        offsets.forEach { minsBefore ->
            val targetTime = baseTime.minusMinutes(minsBefore.toLong())
            val now = LocalDateTime.now()
            if (targetTime.isBefore(now)) return@forEach

            val intent = Intent(context, Receiver::class.java).apply {
                action = "com.algo1127.mytask.ACTION_TRIGGER_EVENT_REMINDER"
                putExtra("eventId", event.id)
                putExtra("eventTitle", event.title)
                putExtra("minutesBefore", minsBefore)
            }

            val requestCode = (event.id.toInt() * 100) + minsBefore + 500000 // offset to avoid task collision
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            val triggerAt = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Log.d("ReminderScheduler", "Scheduled event alarm for ${event.title} at $targetTime")
        }
    }

    fun cancelEventReminder(eventId: Long) {
        listOf(60, 30, 15, 0).forEach { minsBefore ->
            val intent = Intent(context, Receiver::class.java).apply {
                action = "com.algo1127.mytask.ACTION_TRIGGER_EVENT_REMINDER"
            }
            val requestCode = (eventId.toInt() * 100) + minsBefore + 500000
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleCountdown(item: CountdownItem) {
        if (!item.remindWhenUp) return

        val targetTime = item.targetDateTime
        val now = LocalDateTime.now()
        
        if (targetTime.isBefore(now)) {
            Log.d("ReminderScheduler", "Countdown $targetTime is in the past, skipping.")
            return
        }

        val intent = Intent(context, Receiver::class.java).apply {
            action = "com.algo1127.mytask.ACTION_TRIGGER_COUNTDOWN"
            putExtra("countdownId", item.id)
            putExtra("countdownTitle", item.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (item.id % Int.MAX_VALUE).toInt(), // Ensure unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val triggerAt = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
        Log.d("ReminderScheduler", "Scheduled countdown alarm for ${item.title} at $targetTime")
    }

    fun cancelCountdown(countdownId: Long) {
        val intent = Intent(context, Receiver::class.java).apply {
            action = "com.algo1127.mytask.ACTION_TRIGGER_COUNTDOWN"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (countdownId % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
