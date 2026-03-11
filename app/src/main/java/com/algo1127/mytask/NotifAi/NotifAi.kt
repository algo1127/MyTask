package com.algo1127.mytask.NotifAi

import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.NotifAi.Receiver
import com.algo1127.mytask.ui.CalendarReader
import com.algo1127.mytask.ui.TaskItem
import com.algo1127.mytask.ui.TaskCategory

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.algo1127.mytask.NotifAi.model.AiProfile
import com.algo1127.mytask.NotifAi.model.NotificationAction
import com.algo1127.mytask.NotifAi.model.TaskPattern
import com.algo1127.mytask.NotifAi.persistence.Persistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class NotifAi(private val context: Context) {
    private val persistence = Persistence(context)
    private val phraseList = PhraseList()
    private var profile: AiProfile = persistence.loadProfile()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var mood = Mood.HAPPY

    init {
        scope.launch {
            try {
                setupPeriodicEvaluation()
                android.util.Log.d("NotifAi", "Initialization successful")
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Initialization failed: ${e.message}", e)
            }
        }
    }

    fun onTaskCreated(item: TaskItem) {
        scope.launch {
            try {
                val pattern = createInitialPattern(item)
                profile = profile.copy(taskPatterns = profile.taskPatterns + (item.id to pattern))
                persistence.saveProfile(profile)
                android.util.Log.d("NotifAi", "Task created: ${item.title}, ID: ${item.id}")
                evaluate(item)
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Error in onTaskCreated: ${e.message}", e)
            }
        }
    }

    fun onTap(action: NotificationAction, id: Long) {
        scope.launch {
            try {
                val pattern = profile.taskPatterns[id] ?: return@launch
                val newPattern = when (action) {
                    NotificationAction.COMPLETED -> logCompletion(pattern)
                    NotificationAction.FORGOT -> logForgot(pattern)
                    NotificationAction.IGNORED -> logIgnore(pattern)
                    NotificationAction.SKIPPED -> logSkip(pattern)
                }
                profile = profile.copy(taskPatterns = profile.taskPatterns + (id to newPattern))
                persistence.saveProfile(profile)
                android.util.Log.d("NotifAi", "Action $action for task ID: $id")
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Error in onTap: ${e.message}", e)
            }
        }
    }

    // ✅ NEW: Evaluate ALL pending tasks from calendar
    fun evaluateAllPendingTasks() {
        scope.launch {
            try {
                val calendarReader = CalendarReader(context)
                val today = LocalDate.now()
                val events = calendarReader.getEventsForDate(today)

                android.util.Log.d("NotifAi", "Found ${events.size} events to evaluate")

                for (event in events) {
                    val task = TaskItem(
                        title = event.title,
                        time = event.startTime,
                        category = TaskCategory.Work,
                        date = event.date
                    )
                    evaluate(task)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Error in evaluateAllPendingTasks: ${e.message}", e)
            }
        }
    }

    private suspend fun evaluate(item: TaskItem) {
        try {
            val pattern = profile.taskPatterns[item.id] ?: run {
                // Create pattern if doesn't exist (for calendar events)
                val newPattern = createInitialPattern(item)
                profile = profile.copy(taskPatterns = profile.taskPatterns + (item.id to newPattern))
                persistence.saveProfile(profile)
                newPattern
            }
            val now = LocalDateTime.now()
            val due = LocalDateTime.of(item.date, LocalTime.parse(item.time))

            // Only notify if task is due soon or overdue
            val minutesUntilDue = ChronoUnit.MINUTES.between(now, due).toInt()
            if (minutesUntilDue > 30) {
                android.util.Log.d("NotifAi", "Task ${item.title} not due yet (${minutesUntilDue} min)")
                return
            }

            val scale = calculateScale(item, pattern, now)
            val text = if (profile.toggles["soulless"] == true) {
                "Reminder: ${item.title}"
            } else {
                phraseList.pickPhrase(item, scale, mood)
            }
            sendNotification(item.id, text, scale)
            android.util.Log.d("NotifAi", "Evaluated task ${item.title}: Scale $scale, Text: $text")
        } catch (e: Exception) {
            android.util.Log.e("NotifAi", "Error in evaluate: ${e.message}", e)
        }
    }

    private fun calculateScale(item: TaskItem, pattern: TaskPattern, now: LocalDateTime): Int {
        val due = LocalDateTime.of(item.date, LocalTime.parse(item.time))
        val hoursMissed = ChronoUnit.HOURS.between(due, now).toInt()
        val base = when {
            now.isBefore(due) -> 3 // Nudge
            hoursMissed <= 0 -> 5 // Due now
            hoursMissed <= 2 -> 7 // Late
            else -> 9 // Very late
        }
        return (base + pattern.forgetCount).coerceAtMost(10)
    }

    private fun sendNotification(id: Long, text: String, scale: Int) {
        val channelId = "mytask_channel"
        val channel = NotificationChannel(channelId, "MyTask Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MyTask AI")
            .setContentText(text)
            .setPriority(if (scale >= 7) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Completed", createIntent(id, NotificationAction.COMPLETED))
            .addAction(0, "Forgot", createIntent(id, NotificationAction.FORGOT))
            .addAction(0, "Ignore", createIntent(id, NotificationAction.IGNORED))
            .addAction(0, "Skip Today", createIntent(id, NotificationAction.SKIPPED))

        if (scale >= 8) builder.setVibrate(longArrayOf(0, 500, 500))

        manager.notify(id.toInt(), builder.build())
    }

    private fun createIntent(id: Long, action: NotificationAction): PendingIntent {
        val intent = Intent(context, Receiver::class.java).apply {
            // ✅ Set action that matches manifest
            this.action = "com.algo1127.mytask.ACTION_NOTIFICATION"
            putExtra("taskId", id)
            putExtra("action", action.name)
        }

        // ✅ Unique request code for each button (avoids collisions)
        val requestCode = (id.toInt() * 1000) + action.ordinal

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE  // ✅ FLAG_MUTABLE
        )
    }

    private fun createInitialPattern(item: TaskItem): TaskPattern {
        try {
            val parsedTime = LocalTime.parse(item.time)
            return TaskPattern(
                taskId = item.id,
                fuzzyWindowStart = parsedTime.minusMinutes(5),
                fuzzyWindowEnd = parsedTime.plusMinutes(5),
                completionTimes = emptyList(),
                forgetCount = 0,
                ignoreCount = 0,
                skipCount = 0
            )
        } catch (e: Exception) {
            android.util.Log.e("NotifAi", "Error creating initial pattern: ${e.message}", e)
            return TaskPattern(
                taskId = item.id,
                fuzzyWindowStart = LocalTime.now(),
                fuzzyWindowEnd = LocalTime.now().plusMinutes(10),
                completionTimes = emptyList(),
                forgetCount = 0,
                ignoreCount = 0,
                skipCount = 0
            )
        }
    }

    private fun logCompletion(pattern: TaskPattern): TaskPattern {
        val newTimes = pattern.completionTimes + LocalDateTime.now()
        val newStart = calculateWindowStart(newTimes)
        val newEnd = calculateWindowEnd(newTimes)
        return pattern.copy(
            completionTimes = newTimes,
            fuzzyWindowStart = newStart,
            fuzzyWindowEnd = newEnd,
            forgetCount = 0
        )
    }

    private fun logForgot(pattern: TaskPattern): TaskPattern {
        return pattern.copy(forgetCount = pattern.forgetCount + 1)
    }

    private fun logIgnore(pattern: TaskPattern): TaskPattern {
        return pattern.copy(ignoreCount = pattern.ignoreCount + 1)
    }

    private fun logSkip(pattern: TaskPattern): TaskPattern {
        return pattern.copy(skipCount = pattern.skipCount + 1)
    }

    private fun calculateWindowStart(times: List<LocalDateTime>): LocalTime {
        if (times.size < 5) return times.lastOrNull()?.toLocalTime()?.minusMinutes(5) ?: LocalTime.now()
        val avgSeconds = times.map { it.toLocalTime().toSecondOfDay() }.average().toLong()
        return LocalTime.ofSecondOfDay(avgSeconds).minusMinutes(5)
    }

    private fun calculateWindowEnd(times: List<LocalDateTime>): LocalTime {
        if (times.size < 5) return times.lastOrNull()?.toLocalTime()?.plusMinutes(5) ?: LocalTime.now()
        val avgSeconds = times.map { it.toLocalTime().toSecondOfDay() }.average().toLong()
        return LocalTime.ofSecondOfDay(avgSeconds).plusMinutes(5)
    }

    // In NotifAi.kt - Replace setupPeriodicEvaluation()

    private fun setupPeriodicEvaluation() {
        // ✅ Cancel ALL existing work first
        WorkManager.getInstance(context).cancelAllWorkByTag("evaluation_work")

        // ✅ Use enqueueUniquePeriodicWork instead of enqueue
        val workRequest = PeriodicWorkRequestBuilder<EvaluationWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES  // ✅ Add flex time to prevent exact scheduling
        )
            .addTag("evaluation_work")
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                5,
                TimeUnit.MINUTES
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "evaluation_work",  // ✅ Unique name
            ExistingPeriodicWorkPolicy.REPLACE,  // ✅ Replace existing
            workRequest
        )

        android.util.Log.d("NotifAi", "EvaluationWorker scheduled uniquely")
    }
}

// ✅ FIXED: Actually evaluates all pending tasks!
class EvaluationWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        try {
            val notifAi = (applicationContext as MyTaskApplication).notifAi
            android.util.Log.d("NotifAi", "Periodic evaluation triggered")

            // ✅ Call the new method that reads from calendar and evaluates
            notifAi.evaluateAllPendingTasks()

            android.util.Log.d("NotifAi", "Periodic evaluation completed")
            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotifAi", "Error in EvaluationWorker: ${e.message}", e)
            return Result.retry()
        }
    }
}

// In NotifAi.kt:
private fun calculateMood(completionRate: Float): Mood {
    return when {
        completionRate >= 0.8 -> Mood.EXCITED
        completionRate >= 0.5 -> Mood.HAPPY
        completionRate >= 0.3 -> Mood.DISAPPOINTED
        else -> Mood.MAD
    }
}