package com.algo1127.mytask.NotifAi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.algo1127.mytask.NotifAi.model.NotificationAction
import com.algo1127.mytask.NotifAi.persistence.Persistence
import com.algo1127.mytask.ui.TaskCategory
import com.algo1127.mytask.ui.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class NotifAi(private val context: Context) {
    private val persistence = Persistence(context)
    private val phraseList = PhraseList()
    private val scope = CoroutineScope(Dispatchers.Default)

    // AI state
    private var trustScore = 0.5f
    private var mood = Mood.HAPPY
    private var aiPreferences = mutableMapOf<String, String>()

    init {
        scope.launch {
            val state = persistence.getAiState()
            trustScore = state.first
            aiPreferences = state.second.toMutableMap()
        }
    }

    // ==================== NOTIFICATION SENDING (Core Method) ====================
    fun sendTaskNotification(task: Task, intensity: Float) {
        scope.launch {
            try {
                // 1. Generate phrase using PhraseList
                val text = if (aiPreferences["soulless"] == "true") {
                    "Reminder: ${task.title}"
                } else {
                    phraseList.pickPhrase(task, intensity, mood, forgetCount = getForgetCount(task.id))
                }

                // 2. Build notification
                val channelId = "mytask_channel"
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Create channel if needed
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId, "MyTask Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    manager.createNotificationChannel(channel)
                }

                // Build notification
                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("MyTask AI")
                    .setContentText(text)
                    .setPriority(if (intensity >= 0.7) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                    .addAction(0, "Completed", createIntent(task.id, NotificationAction.COMPLETED))
                    .addAction(0, "Forgot", createIntent(task.id, NotificationAction.FORGOT))
                    .addAction(0, "Ignore", createIntent(task.id, NotificationAction.IGNORED))
                    .addAction(0, "Skip Today", createIntent(task.id, NotificationAction.SKIPPED))

                // Vibrate for high intensity
                if (intensity >= 0.8) {
                    builder.setVibrate(longArrayOf(0, 500, 500))
                }

                manager.notify(task.id.toInt(), builder.build())

            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Failed to send notification: ${e.message}", e)
            }
        }
    }

    // ==================== INTENSITY CALCULATION (The "Roast Logic") ====================
    private fun calculateIntensity(task: Task, now: LocalDateTime): Float {
        // Base intensity from deadline proximity
        val dueDate = task.dueDate ?: return 0.3f // Open-ended tasks = low intensity
        val daysUntilDue = ChronoUnit.DAYS.between(now.toLocalDate(), dueDate).toInt()

        val deadlineFactor = when {
            daysUntilDue < 0 -> 1.0f // Overdue = max intensity
            daysUntilDue == 0 -> 0.9f // Due today
            daysUntilDue <= 2 -> 0.7f // Due soon
            else -> 0.4f // Plenty of time
        }

        // Progress factor (less progress = higher intensity)
        val progressFactor = 1.0f - task.progress

        // Trust score modifier (low trust = AI is more pushy)
        val trustModifier = if (trustScore < 0.4) 1.2f else 1.0f

        // Category modifier (Study/Work = slightly higher intensity)
        val categoryModifier = when(task.category) {
            TaskCategory.Study, TaskCategory.Work -> 1.1f
            else -> 1.0f
        }

        // Combine factors (clamp to 0.0-1.0)
        return (deadlineFactor * progressFactor * trustModifier * categoryModifier).coerceIn(0.0f, 1.0f)
    }

    // ==================== TASK EVALUATION (When to Notify) ====================
    fun evaluateTask(task: Task) {
        scope.launch {
            try {
                val now = LocalDateTime.now()
                val intensity = calculateIntensity(task, now)

                // Only notify if intensity is high enough OR task is due today
                if (intensity >= 0.5 || (task.dueDate == now.toLocalDate() && task.progress < 1.0f)) {
                    sendTaskNotification(task, intensity)

                    // Log for learning (v2: plugs into UsageAnalyzer)
                    logNotificationSent(task.id, intensity)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Evaluation failed: ${e.message}", e)
            }
        }
    }

    // ==================== USER ACTION HANDLERS ====================
    fun onTaskAction(taskId: Long, action: NotificationAction) {
        scope.launch {
            try {
                val tasks = persistence.getTasks()
                val task = tasks.find { it.id == taskId } ?: return@launch

                when (action) {
                    NotificationAction.COMPLETED -> {
                        // Mark complete + praise
                        val updated = task.copy(progress = 1.0f, focusState = FocusState.Archived)
                        persistence.saveTask(updated)
                        trustScore = (trustScore + 0.05f).coerceAtMost(1.0f)
                        showCompletionNotification(task)
                    }
                    NotificationAction.FORGOT -> {
                        // Log forget + increase intensity next time
                        val forgetCount = getForgetCount(taskId) + 1
                        aiPreferences["forget_${taskId}"] = forgetCount.toString()
                        trustScore = (trustScore - 0.03f).coerceAtLeast(0.0f)
                    }
                    NotificationAction.IGNORED -> {
                        // Log ignore + slight trust decrease
                        trustScore = (trustScore - 0.02f).coerceAtLeast(0.0f)
                    }
                    NotificationAction.SKIPPED -> {
                        // Reschedule logic (v2)
                        android.util.Log.d("NotifAi", "Task $taskId skipped - reschedule pending")
                    }
                }

                // Save AI state
                persistence.saveAiState(trustScore, aiPreferences)

            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Action handler failed: ${e.message}", e)
            }
        }
    }

    // ✅ SCHEDULE FLEXIBLE TASKS based on time preference
    fun scheduleFlexible(task: Task) {
        scope.launch {
            try {
                when (val pref = task.timePreference) {
                    is TimePreference.Fixed -> {
                        // Schedule at exact time
                        android.util.Log.d("NotifAi", "Scheduling ${task.title} at ${pref.time}")
                        // v2: Use WorkManager to schedule exact time notification
                    }
                    TimePreference.LaterToday -> {
                        // Schedule for later today (2 hours from now)
                        android.util.Log.d("NotifAi", "Scheduling ${task.title} for later today")
                        // v2: Use WorkManager with initial delay
                    }
                    TimePreference.Tomorrow -> {
                        // Schedule for tomorrow morning (9 AM)
                        android.util.Log.d("NotifAi", "Scheduling ${task.title} for tomorrow")
                        // v2: Use WorkManager with calculated delay
                    }
                    is TimePreference.Window -> {
                        // Schedule within window (e.g., 7-9 AM)
                        android.util.Log.d("NotifAi", "Scheduling ${task.title} between ${pref.startHour}:00-${pref.endHour}:00")
                        // v2: Use WorkManager with flexible timing
                    }
                    TimePreference.AiDecide -> {
                        // Let AI decide based on usage patterns
                        android.util.Log.d("NotifAi", "AI will decide timing for ${task.title}")
                        // v2: Use UsageAnalyzer to find optimal time
                    }
                }
                // For v1: Just evaluate immediately for testing
                evaluateTask(task)
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "scheduleFlexible failed: ${e.message}", e)
            }
        }
    }

    // ✅ COMPATIBILITY METHOD for old TaskItem → new Task
    fun onTaskCreated(item: com.algo1127.mytask.ui.TaskItem) {
        scope.launch {
            try {
                // Convert old TaskItem to new Task model
                val task = com.algo1127.mytask.ui.models.Task(
                    title = item.title,
                    startDate = item.date,
                    dueDate = item.date,
                    category = item.category,
                    progress = if (item.done) 1.0f else 0.0f,
                    timePreference = com.algo1127.mytask.ui.models.TimePreference.Fixed(
                        LocalTime.parse(item.time)
                    )
                )

                // Save and schedule
                persistence.saveTask(task)
                scheduleFlexible(task)

                android.util.Log.d("NotifAi", "Task created (compat): ${item.title}")
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Compat onTaskCreated failed: ${e.message}", e)
            }
        }
    }

    // ==================== HELPER METHODS ====================
    private fun getForgetCount(taskId: Long): Int {
        return aiPreferences["forget_$taskId"]?.toIntOrNull() ?: 0
    }

    private fun logNotificationSent(taskId: Long, intensity: Float) {
        // v2: logs to UsageAnalyzer for effectiveness tracking
        android.util.Log.d("NotifAi", "Notification sent for task $taskId (intensity: $intensity)")
    }

    private fun showCompletionNotification(task: Task) {
        scope.launch {
            val text = phraseList.getCompletionPhrase(task)
            // Build simple completion notification (same pattern as sendTaskNotification)
            // ... implementation omitted for brevity ...
        }
    }

    private fun createIntent(taskId: Long, action: NotificationAction): PendingIntent {
        val intent = Intent(context, Receiver::class.java).apply {
            this.action = "com.algo1127.mytask.ACTION_NOTIFICATION"
            putExtra("taskId", taskId)
            putExtra("action", action.name)
        }
        val requestCode = (taskId.toInt() * 1000) + action.ordinal
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}