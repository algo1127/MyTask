package com.algo1127.mytask.NotifAi

import com.algo1127.mytask.ui.TaskCategory
import java.time.LocalDateTime
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Stateless analyzer — takes a list of UsageRecords and produces
 * actionable insights. All scoring is data-driven; nothing is hardcoded
 * except sensible priors that fade as real data accumulates.
 */
object PatternAnalyzer {

    // ── Decay constant: events older than ~45 days half their weight ──
    private const val DECAY_HALF_LIFE_DAYS = 45.0
    private val DECAY_LAMBDA = ln(2.0) / DECAY_HALF_LIFE_DAYS

    // ── Prior strength: equivalent to ~8 "neutral" observations ───────
    // Fades as real data accumulates, so data dominates quickly.
    private const val PRIOR_STRENGTH = 8.0
    private const val PRIOR_SCORE    = 0.45   // slightly below neutral

    // ── Response time thresholds (ms) ─────────────────────────────────
    private const val RESPONSE_FAST   = 2 * 60_000L   // < 2 min  → excellent
    private const val RESPONSE_MEDIUM = 10 * 60_000L  // < 10 min → ok

    // ═════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═════════════════════════════════════════════════════════════════

    /**
     * Returns the best [LocalDateTime] to notify for a task, considering:
     *  - Hour-of-day × day-of-week completion rates (168 slots)
     *  - Category-specific affinity
     *  - Notification fatigue (recent ignores in a slot)
     *  - Response-time quality per slot
     *  - Night-time hard penalty
     *
     * [afterHour] — earliest acceptable hour (default = now+1)
     * [searchDays] — how many days ahead to search (default 2)
     */
    fun bestTimeForCategory(
        records:    List<UsageRecord>,
        category:   TaskCategory,
        afterHour:  Int = (LocalDateTime.now().hour + 1) % 24,
        searchDays: Int = 2
    ): LocalDateTime {
        val now = LocalDateTime.now()

        // Score every candidate slot in the search window
        data class Candidate(val dt: LocalDateTime, val score: Double)
        val candidates = mutableListOf<Candidate>()

        repeat(searchDays * 24) { offset ->
            val candidate = now.plusHours(offset.toLong())
            if (candidate.hour < afterHour && offset < 24) return@repeat  // skip past hours today

            val score = slotScore(records, category, candidate.hour, candidate.dayOfWeek.value)
            candidates.add(Candidate(candidate.withMinute(0).withSecond(0).withNano(0), score))
        }

        return candidates.maxByOrNull { it.score }?.dt ?: now.plusHours(2)
    }

    /**
     * 0–1 score for a specific hour+dayOfWeek slot for a category.
     * Higher = better time to send a notification.
     */
    fun slotScore(
        records:    List<UsageRecord>,
        category:   TaskCategory,
        hour:       Int,
        dayOfWeek:  Int
    ): Double {
        val relevant = records.filter {
            it.hour == hour &&
                    it.dayOfWeek == dayOfWeek &&
                    (it.category == category ||
                            // borrow signal from similar categories if sparse
                            categoryGroup(it.category) == categoryGroup(category))
        }

        val now = System.currentTimeMillis()

        // ── Weighted outcome sum ──────────────────────────────────────
        var weightedPositive = PRIOR_STRENGTH * PRIOR_SCORE
        var totalWeight      = PRIOR_STRENGTH

        relevant.forEach { r ->
            val ageDays = (now - r.timestampMs) / 86_400_000.0
            val w       = exp(-DECAY_LAMBDA * ageDays)   // recency weight

            // Category match gets full weight; group match gets 0.4
            val categoryWeight = if (r.category == category) 1.0 else 0.4

            val outcome = outcomeScore(r)
            weightedPositive += w * categoryWeight * outcome
            totalWeight      += w * categoryWeight
        }

        val baseScore = weightedPositive / totalWeight

        // ── Fatigue penalty: recent ignores in this slot ──────────────
        val recentIgnores = records.count {
            it.hour == hour &&
                    it.dayOfWeek == dayOfWeek &&
                    it.event == UsageEvent.IGNORED &&
                    (now - it.timestampMs) < 7 * 86_400_000L  // last 7 days
        }
        val fatiguePenalty = min(recentIgnores * 0.08, 0.35)

        // ── Night penalty (hard — 23:00–06:00) ───────────────────────
        val nightPenalty = if (hour in 23..23 || hour in 0..6) 0.70 else 0.0

        // ── Early morning soft penalty (07:00) ────────────────────────
        val earlyPenalty = if (hour == 7) 0.10 else 0.0

        return (baseScore - fatiguePenalty - nightPenalty - earlyPenalty).coerceIn(0.0, 1.0)
    }

    /**
     * Full 24-hour profile for a category on a given day of week.
     * Returns FloatArray[24] of scores — useful for UI visualisation.
     */
    fun hourlyProfile(
        records:   List<UsageRecord>,
        category:  TaskCategory,
        dayOfWeek: Int = LocalDateTime.now().dayOfWeek.value
    ): FloatArray = FloatArray(24) { h ->
        slotScore(records, category, h, dayOfWeek).toFloat()
    }

    /**
     * Notification effectiveness: ratio of (completed+skipped) / sent
     * over the last [days] days.  Returns 0–1.
     */
    fun effectivenessScore(records: List<UsageRecord>, days: Int = 30): Double {
        val cutoff  = System.currentTimeMillis() - days * 86_400_000L
        val recent  = records.filter { it.timestampMs >= cutoff }
        val sent    = recent.count { it.event == UsageEvent.NOTIFICATION_SENT }.toDouble()
        val acted   = recent.count { it.event == UsageEvent.COMPLETED || it.event == UsageEvent.SKIPPED }.toDouble()
        return if (sent < 5) 0.5 else (acted / sent).coerceIn(0.0, 1.0)
    }

    /**
     * Detects notification fatigue: true if user ignored > 60 % of
     * notifications in the last 7 days.
     */
    fun isFatigued(records: List<UsageRecord>): Boolean {
        val cutoff  = System.currentTimeMillis() - 7 * 86_400_000L
        val recent  = records.filter { it.timestampMs >= cutoff }
        val sent    = recent.count { it.event == UsageEvent.NOTIFICATION_SENT }
        val ignored = recent.count { it.event == UsageEvent.IGNORED }
        return sent >= 5 && (ignored.toDouble() / sent) > 0.60
    }

    /**
     * Best average response time for a category, in minutes.
     * -1 if not enough data.
     */
    fun avgResponseMinutes(records: List<UsageRecord>, category: TaskCategory): Double {
        val times = records
            .filter { it.category == category && it.responseTimeMs > 0 }
            .map { it.responseTimeMs / 60_000.0 }
        return if (times.size < 3) -1.0 else times.average()
    }

    /**
     * Returns a human-readable summary of what the AI has learned.
     * Used for UI display ("AI Insights").
     */
    fun insightSummary(records: List<UsageRecord>, category: TaskCategory): String {
        if (records.size < 10) return "Still learning your patterns — keep using the app."

        val now = LocalDateTime.now()
        val dow = now.dayOfWeek.value
        val profile = hourlyProfile(records, category, dow)
        val bestHour = profile.indices.maxByOrNull { profile[it] } ?: 9
        val worstHour = profile.indices
            .filter { it in 8..22 }  // only consider waking hours for worst
            .minByOrNull { profile[it] } ?: 20

        val effectiveness = effectivenessScore(records)
        val fatigued = isFatigued(records)
        val avgResp = avgResponseMinutes(records, category)

        val sb = StringBuilder()
        sb.appendLine("Best time: ${formatHour(bestHour)}")
        sb.appendLine("Worst time: ${formatHour(worstHour)}")
        sb.appendLine("Response rate: ${(effectiveness * 100).toInt()}%")
        if (avgResp > 0) sb.appendLine("Avg response: ${avgResp.toInt()} min")
        if (fatigued) sb.appendLine("⚠ Reducing frequency — too many ignored")
        return sb.toString().trim()
    }

    // ═════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════

    /** Converts a UsageRecord into a 0–1 outcome score */
    private fun outcomeScore(r: UsageRecord): Double = when (r.event) {
        UsageEvent.COMPLETED -> {
            // Fast completion = better signal than slow
            when {
                r.responseTimeMs in 1..RESPONSE_FAST   -> 1.0
                r.responseTimeMs in 1..RESPONSE_MEDIUM -> 0.85
                r.responseTimeMs > 0                   -> 0.70
                else                                   -> 0.80  // completed, unknown time
            }
        }
        UsageEvent.NOTIFICATION_SENT -> 0.50  // neutral — we don't know outcome yet
        UsageEvent.APP_OPENED        -> 0.60  // phone was in hand, mild positive
        UsageEvent.TASK_CREATED      -> 0.65  // user was active enough to add tasks
        UsageEvent.SKIPPED           -> 0.35  // acknowledged but deferred
        UsageEvent.FORGOT            -> 0.10  // strong negative
        UsageEvent.IGNORED           -> 0.05  // strong negative
    }

    /**
     * Groups categories so sparse-data categories can borrow signal
     * from related ones.  E.g. Work & Study share patterns.
     */
    private fun categoryGroup(cat: TaskCategory): Int = when (cat) {
        TaskCategory.Work, TaskCategory.Study -> 1
        TaskCategory.Personal                  -> 2
        else                                   -> 3
    }

    private fun formatHour(h: Int): String {
        val suffix = if (h < 12) "AM" else "PM"
        val display = if (h % 12 == 0) 12 else h % 12
        return "$display $suffix"
    }
}