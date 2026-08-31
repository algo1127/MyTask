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
    private val appContext: Context = context

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
     */
    fun getBestTime(
        category:     TaskCategory,
        mode:         AnalysisMode = AnalysisMode.TASK_PLACEMENT,
        anchor:       LocalDateTime = LocalDateTime.now(),
        aroundHour:   Int? = null,
        betweenHours: Pair<Int, Int>? = null
    ): LocalTime {
        val records = tracker.getAll()

        if (records.isEmpty()) {
            val defaultHour = coldStartHour[category] ?: 10
            // Fallback for empty data: ensuring it's future if anchor is today
            val suggestion = LocalTime.of(defaultHour.coerceAtMost(22), 0)
            return if (anchor.toLocalDate() == LocalDateTime.now().toLocalDate() && 
                suggestion.isBefore(LocalTime.now().plusMinutes(5))) {
                LocalTime.now().plusHours(1).withMinute(0)
            } else suggestion
        }

        val bestDt = PatternAnalyzer.bestTimeForCategory(
            records, 
            category, 
            mode = mode,
            anchor = anchor,
            aroundHour = aroundHour,
            betweenHours = betweenHours
        )
        return bestDt.toLocalTime()
    }

    /**
     * Should we send a notification right now?
     * Returns false if we detect fatigue or if the current slot score is low.
     */
    fun shouldNotifyNow(category: TaskCategory): Boolean {
        val records = tracker.getAll()
        val deviceCtx = UsageAccessCollector.getLatest(appContext)

        // Hard block from device context (idle, in call, etc.)
        if (ContextScorer.shouldHardBlock(deviceCtx)) return false

        if (records.size < MIN_RECORDS_FOR_REAL_LEARNING) return true
        if (PatternAnalyzer.isFatigued(records)) return false

        val now = LocalDateTime.now()
        
        // ── Reminder Mode Score ──────────────────────────────────────
        // We want times where they ARE using the device (Density) 
        // AND it's a historically good slot.
        val baseScore = PatternAnalyzer.slotScore(records, category, now.hour, now.dayOfWeek.value)
        val usageDensity = PatternAnalyzer.usageDensityScore(records, now.hour, now.dayOfWeek.value)
        
        val entropyScore = PatternAnalyzer.focusEntropyScore(records)
        val contextBonus = ContextScorer.contextDelta(deviceCtx)
        
        // Final probability: blend historical completion with current usage density
        val analyticalScore = (baseScore * 0.6 + usageDensity * 0.4)
        
        val threshold = if (entropyScore < 0.3) 0.65 else 0.35
        
        return (analyticalScore + contextBonus + (entropyScore - 0.5) * 0.4) >= threshold
    }

    /**
     * 0–1 quality score for notifying RIGHT NOW for a category.
     * Callers can use this to adjust notification priority / tone.
     */
    fun currentSlotScore(category: TaskCategory): Double {
        val records = tracker.getAll()
        val deviceCtx = UsageAccessCollector.getLatest(appContext)
        val entropyScore = PatternAnalyzer.focusEntropyScore(records)
        
        if (records.size < MIN_RECORDS_FOR_REAL_LEARNING) return 0.5 + ContextScorer.contextDelta(deviceCtx)
        val now = LocalDateTime.now()
        val base = PatternAnalyzer.slotScore(records, category, now.hour, now.dayOfWeek.value)
        
        return (base + ContextScorer.contextDelta(deviceCtx) + (entropyScore - 0.5) * 0.3).coerceIn(0.0, 1.0)
    }

    // ═════════════════════════════════════════════════════════════════
    // FEEDBACK — called by NotifAi when user acts
    // ═════════════════════════════════════════════════════════════════

    fun recordPositive(taskId: Long, category: TaskCategory) {
        val responseTime = tracker.getResponseTime(taskId)
        val deviceCtx = UsageAccessCollector.getLatest(appContext)
        tracker.record(UsageEvent.COMPLETED, category, taskId, responseTime, deviceCtx)
    }

    fun recordIgnore(taskId: Long, category: TaskCategory) {
        val deviceCtx = UsageAccessCollector.getLatest(appContext)
        tracker.record(UsageEvent.IGNORED, category, taskId, deviceCtx = deviceCtx)
    }

    fun recordForget(taskId: Long, category: TaskCategory) {
        val deviceCtx = UsageAccessCollector.getLatest(appContext)
        tracker.record(UsageEvent.FORGOT, category, taskId, deviceCtx = deviceCtx)
    }

    fun recordSkip(taskId: Long, category: TaskCategory) {
        val deviceCtx = UsageAccessCollector.getLatest(appContext)
        tracker.record(UsageEvent.SKIPPED, category, taskId, deviceCtx = deviceCtx)
    }

    fun recordNotificationSent(taskId: Long, category: TaskCategory) {
        tracker.markNotificationSent(taskId)
        val deviceCtx = UsageAccessCollector.getLatest(appContext)
        tracker.record(UsageEvent.NOTIFICATION_SENT, category, taskId, deviceCtx = deviceCtx)
    }

    fun recordAppOpened(category: TaskCategory = TaskCategory.Personal) {
        val deviceCtx = UsageAccessCollector.getLatest(appContext)
        tracker.record(UsageEvent.APP_OPENED, category, -1L, deviceCtx = deviceCtx)
    }

    fun recordTaskCreated(taskId: Long, category: TaskCategory) {
        val deviceCtx = UsageAccessCollector.getLatest(appContext)
        tracker.record(UsageEvent.TASK_CREATED, category, taskId, deviceCtx = deviceCtx)
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