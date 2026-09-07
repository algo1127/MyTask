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
        val targetTime = task.dueDate?.atTime(
            (task.timePreference as? com.algo1127.mytask.ui.models.TimePreference.Fixed)?.time 
            ?: java.time.LocalTime.of(9, 0)
        ) ?: return

        val now = LocalDateTime.now()
        if (targetTime.isBefore(now)) {
            Log.d("ReminderScheduler", "Target time $targetTime is in the past, skipping alarm.")
            return
        }

        val intent = Intent(context, Receiver::class.java).apply {
            action = "com.algo1127.mytask.ACTION_TRIGGER_REMINDER"
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
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
        
        Log.d("ReminderScheduler", "Scheduled alarm for ${task.title} at $targetTime")
    }

    fun cancel(taskId: Long) {
        val intent = Intent(context, Receiver::class.java).apply {
            action = "com.algo1127.mytask.ACTION_TRIGGER_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        alarmManager.cancel(pendingIntent)
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
