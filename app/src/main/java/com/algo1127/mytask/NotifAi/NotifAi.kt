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

    private val persistence    = Persistence(context)
    private val phraseList     = PhraseList()
    private val learningEngine = LearningEngine(context)
    private val rewardSystem   = RewardSystem(context)
    private val scope          = CoroutineScope(Dispatchers.Default)

    // AI state
    private var trustScore     = 0.5f
    private var mood           = Mood.HAPPY
    private var aiPreferences  = mutableMapOf<String, String>()

    init {
        scope.launch {
            val state  = persistence.getAiState()
            trustScore = state.first
            aiPreferences = state.second.toMutableMap()
            learningEngine.recordAppOpened()
            // Start background usage collection
            UsageAccessCollector.schedule(context)
        }
    }

    // ── Public API for AddTaskDialog "AI Decide" ──────────────────────

    /**
     * Returns a real data-driven Fixed time for this category.
     * NOT hardcoded — uses actual usage history.
     */
    fun suggestTime(category: TaskCategory): TimePreference.Fixed {
        val best = learningEngine.getBestTime(category)
        return TimePreference.Fixed(best)
    }

    // ── Notification sending ──────────────────────────────────────────

    fun sendTaskNotification(task: Task, intensity: Float) {
        scope.launch {
            try {
                // Don't notify if the AI says this is a bad slot AND intensity isn't critical
                if (!learningEngine.shouldNotifyNow(task.category) && intensity < 0.85f) {
                    android.util.Log.d("NotifAi", "Skipping notification — bad slot / fatigue detected")
                    return@launch
                }

                val slotQuality = learningEngine.currentSlotScore(task.category)

                val text = if (aiPreferences["soulless"] == "true") {
                    "Reminder: ${task.title}"
                } else {
                    phraseList.pickPhrase(
                        task,
                        intensity,
                        mood,
                        forgetCount = getForgetCount(task.id)
                    )
                }

                // Priority is influenced by both intensity AND slot quality
                val effectivePriority = when {
                    intensity >= 0.85f || slotQuality >= 0.75 ->
                        NotificationCompat.PRIORITY_HIGH
                    else ->
                        NotificationCompat.PRIORITY_DEFAULT
                }

                val channelId = "mytask_channel"
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId, "MyTask Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    manager.createNotificationChannel(channel)
                }

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("MyTask")
                    .setContentText(text)
                    .setPriority(effectivePriority)
                    .addAction(0, "✓ Done",      createIntent(task.id, NotificationAction.COMPLETED))
                    .addAction(0, "✗ Forgot",    createIntent(task.id, NotificationAction.FORGOT))
                    .addAction(0, "→ Skip",      createIntent(task.id, NotificationAction.SKIPPED))
                    .setAutoCancel(true)

                if (intensity >= 0.8f) {
                    builder.setVibrate(longArrayOf(0, 500, 200, 500))
                }

                manager.notify(task.id.toInt(), builder.build())

                // Record that we sent this — used for response time calculation
                learningEngine.recordNotificationSent(task.id, task.category)
                logNotificationSent(task.id, intensity)

            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Failed to send notification: ${e.message}", e)
            }
        }
    }

    // ── Intensity calculation ─────────────────────────────────────────

    private fun calculateIntensity(task: Task, now: LocalDateTime): Float {
        val dueDate = task.dueDate ?: return 0.3f
        val daysUntilDue = ChronoUnit.DAYS.between(now.toLocalDate(), dueDate).toInt()

        val deadlineFactor = when {
            daysUntilDue < 0  -> 1.0f
            daysUntilDue == 0 -> 0.9f
            daysUntilDue <= 2 -> 0.7f
            else              -> 0.4f
        }

        val progressFactor = 1.0f - task.progress

        // Trust modifier now also considers real notification effectiveness
        val effectiveness = if (learningEngine.hasEnoughData()) {
            PatternAnalyzer.effectivenessScore(learningEngine.tracker.getAll()).toFloat()
        } else trustScore

        val trustModifier = when {
            effectiveness < 0.25f -> 1.35f   // very low effectiveness → be more aggressive
            effectiveness < 0.4f  -> 1.20f
            effectiveness > 0.7f  -> 0.90f   // doing well → back off a little
            else                  -> 1.0f
        }

        val categoryModifier = when (task.category) {
            TaskCategory.Work, TaskCategory.Study -> 1.1f
            else                                  -> 1.0f
        }

        return (deadlineFactor * progressFactor * trustModifier * categoryModifier)
            .coerceIn(0.0f, 1.0f)
    }

    // ── Task evaluation ───────────────────────────────────────────────

    fun evaluateTask(task: Task) {
        scope.launch {
            try {
                val now       = LocalDateTime.now()
                val intensity = calculateIntensity(task, now)

                if (intensity >= 0.5 ||
                    (task.dueDate == now.toLocalDate() && task.progress < 1.0f)
                ) {
                    sendTaskNotification(task, intensity)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Evaluation failed: ${e.message}", e)
            }
        }
    }

    // ── User action handlers ──────────────────────────────────────────

    fun onTaskAction(taskId: Long, action: NotificationAction) {
        scope.launch {
            try {
                val tasks = persistence.getTasks()
                val task  = tasks.find { it.id == taskId } ?: return@launch

                when (action) {
                    NotificationAction.COMPLETED -> {
                        val updated = task.copy(progress = 1.0f, focusState = FocusState.Archived)
                        persistence.saveTask(updated)
                        trustScore = (trustScore + 0.05f).coerceAtMost(1.0f)
                        // Pass taskId so LearningEngine can compute response time
                        learningEngine.recordPositive(taskId, task.category)
                        rewardSystem.onCompleted()
                        showCompletionNotification(task)
                    }
                    NotificationAction.FORGOT -> {
                        val forgetCount = getForgetCount(taskId) + 1
                        aiPreferences["forget_${taskId}"] = forgetCount.toString()
                        trustScore = (trustScore - 0.03f).coerceAtLeast(0.0f)
                        learningEngine.recordForget(taskId, task.category)
                        rewardSystem.onForgotten()
                    }
                    NotificationAction.IGNORED -> {
                        trustScore = (trustScore - 0.02f).coerceAtLeast(0.0f)
                        learningEngine.recordIgnore(taskId, task.category)
                        rewardSystem.onIgnored()
                    }
                    NotificationAction.SKIPPED -> {
                        learningEngine.recordSkip(taskId, task.category)
                        android.util.Log.d("NotifAi", "Task $taskId skipped")
                    }
                }

                persistence.saveAiState(trustScore, aiPreferences)

            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Action handler failed: ${e.message}", e)
            }
        }
    }

    // ── Schedule flexible tasks ───────────────────────────────────────

    fun scheduleFlexible(task: Task) {
        scope.launch {
            try {
                when (val pref = task.timePreference) {
                    is TimePreference.Fixed -> {
                        android.util.Log.d("NotifAi", "Scheduling ${task.title} at ${pref.time}")
                    }
                    TimePreference.LaterToday -> {
                        android.util.Log.d("NotifAi", "Scheduling ${task.title} for later today")
                    }
                    TimePreference.Tomorrow -> {
                        android.util.Log.d("NotifAi", "Scheduling ${task.title} for tomorrow")
                    }
                    is TimePreference.Window -> {
                        android.util.Log.d("NotifAi", "Scheduling ${task.title} in window")
                    }
                    TimePreference.AiDecide -> {
                        // Resolve to real time using learned patterns
                        val suggested = suggestTime(task.category)
                        android.util.Log.d("NotifAi",
                            "AI decided ${task.title} → ${suggested.time}")
                    }
                }
                evaluateTask(task)
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "scheduleFlexible failed: ${e.message}", e)
            }
        }
    }

    // ── Compat bridge ─────────────────────────────────────────────────

    fun onTaskCreated(item: com.algo1127.mytask.ui.TaskItem) {
        scope.launch {
            try {
                val task = Task(
                    title          = item.title,
                    startDate      = item.date,
                    dueDate        = item.date,
                    category       = item.category,
                    progress       = if (item.done) 1.0f else 0.0f,
                    timePreference = TimePreference.Fixed(LocalTime.parse(item.time))
                )
                persistence.saveTask(task)
                learningEngine.recordTaskCreated(task.id, task.category)
                scheduleFlexible(task)
                android.util.Log.d("NotifAi", "Task created (compat): ${item.title}")
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Compat onTaskCreated failed: ${e.message}", e)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun getForgetCount(taskId: Long): Int =
        aiPreferences["forget_$taskId"]?.toIntOrNull() ?: 0

    private fun logNotificationSent(taskId: Long, intensity: Float) {
        android.util.Log.d("NotifAi",
            "Notification sent for task $taskId (intensity=$intensity, " +
                    "dataPoints=${learningEngine.dataPointCount()}, " +
                    "fatigued=${learningEngine.isFatigued()})")
    }

    private fun showCompletionNotification(task: Task) {
        scope.launch {
            try {
                val text    = phraseList.getCompletionPhrase(task)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager
                val builder = NotificationCompat.Builder(context, "mytask_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("✓ Done!")
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                manager.notify((task.id + 900_000L).toInt(), builder.build())
            } catch (e: Exception) {
                android.util.Log.e("NotifAi", "Completion notification failed: ${e.message}", e)
            }
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