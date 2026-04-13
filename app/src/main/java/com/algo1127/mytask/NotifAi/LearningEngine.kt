package com.algo1127.mytask.NotifAi

import android.content.Context
import com.algo1127.mytask.ui.TaskCategory
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * LearningEngine — the single public API the rest of the app talks to.
 *
 * Replaces the old EMA-per-24-slots approach entirely.
 * All intelligence now lives in PatternAnalyzer which works over
 * real timestamped interaction data from UsageTracker.
 *
 * Nothing here is hardcoded except the minimum data threshold below
 * which we fall back to a sensible category-aware default.
 */
class LearningEngine(context: Context) {

    val tracker = UsageTracker(context)

    // ── Cold-start category defaults (used ONLY until we have real data) ──
    // These are conservative — they'll be overridden once ~15 records exist.
    private val coldStartHour: Map<TaskCategory, Int> = mapOf(
        TaskCategory.Work    to 9,
        TaskCategory.Study   to 10,
        TaskCategory.Personal to 11,

    )
    private val MIN_RECORDS_FOR_REAL_LEARNING = 15

    // ═════════════════════════════════════════════════════════════════
    // CORE DECISION: when should we notify?
    // ═════════════════════════════════════════════════════════════════

    /**
     * Returns the best [LocalTime] to notify for a given category.
     * This is what AddTaskDialog calls for "AI Decide."
     *
     * Uses real usage data if available; falls back to category default.
     */
    fun getBestTime(
        category:  TaskCategory,
        afterHour: Int = (LocalDateTime.now().hour + 1) % 24
    ): LocalTime {
        val records = tracker.getAll()

        // Not enough data yet — use cold-start defaults
        if (records.size < MIN_RECORDS_FOR_REAL_LEARNING) {
            val defaultHour = coldStartHour[category] ?: 10
            val clamped = if (defaultHour <= afterHour) afterHour + 1 else defaultHour
            return LocalTime.of(clamped.coerceAtMost(22), 0)
        }

        val bestDt = PatternAnalyzer.bestTimeForCategory(records, category, afterHour)
        return bestDt.toLocalTime()
    }

    /**
     * Should we send a notification right now?
     * Returns false if we detect fatigue or if the current slot score is low.
     */
    fun shouldNotifyNow(category: TaskCategory): Boolean {
        val records = tracker.getAll()
        if (records.size < MIN_RECORDS_FOR_REAL_LEARNING) return true  // no data → always allow

        val fatigued = PatternAnalyzer.isFatigued(records)
        if (fatigued) return false

        val now = LocalDateTime.now()
        val score = PatternAnalyzer.slotScore(records, category, now.hour, now.dayOfWeek.value)
        return score >= 0.30  // minimum viable slot score
    }

    /**
     * 0–1 quality score for notifying RIGHT NOW for a category.
     * Callers can use this to adjust notification priority / tone.
     */
    fun currentSlotScore(category: TaskCategory): Double {
        val records = tracker.getAll()
        if (records.size < MIN_RECORDS_FOR_REAL_LEARNING) return 0.5
        val now = LocalDateTime.now()
        return PatternAnalyzer.slotScore(records, category, now.hour, now.dayOfWeek.value)
    }

    // ═════════════════════════════════════════════════════════════════
    // FEEDBACK — called by NotifAi when user acts
    // ═════════════════════════════════════════════════════════════════

    fun recordPositive(taskId: Long, category: TaskCategory) {
        val responseTime = tracker.getResponseTime(taskId)
        tracker.record(UsageEvent.COMPLETED, category, taskId, responseTime)
    }

    fun recordIgnore(taskId: Long, category: TaskCategory) {
        tracker.record(UsageEvent.IGNORED, category, taskId)
    }

    fun recordForget(taskId: Long, category: TaskCategory) {
        tracker.record(UsageEvent.FORGOT, category, taskId)
    }

    fun recordSkip(taskId: Long, category: TaskCategory) {
        tracker.record(UsageEvent.SKIPPED, category, taskId)
    }

    fun recordNotificationSent(taskId: Long, category: TaskCategory) {
        tracker.markNotificationSent(taskId)
        tracker.record(UsageEvent.NOTIFICATION_SENT, category, taskId)
    }

    fun recordAppOpened(category: TaskCategory = TaskCategory.Personal) {
        tracker.record(UsageEvent.APP_OPENED, category, -1L)
    }

    fun recordTaskCreated(taskId: Long, category: TaskCategory) {
        tracker.record(UsageEvent.TASK_CREATED, category, taskId)
    }

    // ═════════════════════════════════════════════════════════════════
    // INSIGHTS — for UI display
    // ═════════════════════════════════════════════════════════════════

    fun insightSummary(category: TaskCategory): String =
        PatternAnalyzer.insightSummary(tracker.getAll(), category)

    fun hourlyProfile(category: TaskCategory): FloatArray =
        PatternAnalyzer.hourlyProfile(tracker.getAll(), category)

    fun isFatigued(): Boolean =
        PatternAnalyzer.isFatigued(tracker.getAll())

    fun dataPointCount(): Int = tracker.getAll().size

    fun hasEnoughData(): Boolean = tracker.getAll().size >= MIN_RECORDS_FOR_REAL_LEARNING
}