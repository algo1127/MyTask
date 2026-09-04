package com.algo1127.mytask.NotifAi

import com.algo1127.mytask.ui.TaskCategory
import java.time.LocalDateTime
import java.util.Random
import kotlin.math.*

/**
 * Analytical Engine — uses Bayesian inference (Thompson Sampling) and 
 * Contextual Entropy to predict optimal engagement windows.
 */
enum class AnalysisMode {
    TASK_PLACEMENT, // Finding time to actually do the task (slacking or off-phone)
    REMINDER_SENT   // Finding time to send notif (phone in hand, not sleeping)
}

object PatternAnalyzer {

    // ── Decay constant: events older than ~45 days half their weight ──
    private const val DECAY_HALF_LIFE_DAYS = 45.0
    private val DECAY_LAMBDA = ln(2.0) / DECAY_HALF_LIFE_DAYS

    // ── Prior strength: equivalent to ~4 "neutral" observations ───────
    private const val PRIOR_ALPHA = 2.0
    private const val PRIOR_BETA  = 2.0

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
        records:      List<UsageRecord>,
        category:     TaskCategory,
        searchDays:   Int = 2,
        mode:         AnalysisMode = AnalysisMode.TASK_PLACEMENT,
        anchor:       LocalDateTime = LocalDateTime.now(),
        aroundHour:   Int? = null,
        betweenHours: Pair<Int, Int>? = null
    ): LocalDateTime {
        val now = LocalDateTime.now()
        val startPoint = if (anchor.isBefore(now)) now else anchor

        // Score every candidate slot in the search window (15-min steps)
        data class Candidate(val dt: LocalDateTime, val score: Double)
        val candidates = mutableListOf<Candidate>()

        // 15-minute resolution
        repeat(searchDays * 24 * 4) { offset ->
            val candidate = startPoint.plusMinutes(offset.toLong() * 15)
                .withSecond(0).withNano(0)
            
            // Ensure suggestion is strictly in the future
            if (candidate.isBefore(now.plusMinutes(5))) return@repeat 

            // --- Apply Guided Constraints ---
            if (betweenHours != null) {
                val (start, end) = betweenHours
                if (candidate.hour < start || candidate.hour >= end) return@repeat
            }

            var score = when (mode) {
                AnalysisMode.TASK_PLACEMENT -> {
                    val completionScore = slotScore(records, category, candidate.hour, candidate.dayOfWeek.value, useSampling = true)
                    val density = usageDensityScore(records, candidate.hour, candidate.dayOfWeek.value)
                    val slack = slackScore(records, candidate.hour, candidate.dayOfWeek.value)
                    val dead = deadSpaceScore(records, candidate.hour, candidate.dayOfWeek.value)
                    
                    // Prioritize (Good completion) AND (Not on phone OR Slacking) AND (Not dead space)
                    (completionScore * 0.4 + (1.0 - density) * 0.3 + slack * 0.3) * (1.0 - dead)
                }
                AnalysisMode.REMINDER_SENT -> {
                    val density = usageDensityScore(records, candidate.hour, candidate.dayOfWeek.value)
                    val dead = deadSpaceScore(records, candidate.hour, candidate.dayOfWeek.value)
                    
                    // Prioritize High Density (Visibility) AND NOT Dead Space
                    density * (1.0 - dead)
                }
            }

            // Around Hour Penalty: -0.1 per hour distance
            aroundHour?.let { target ->
                val distance = abs(candidate.hour - target)
                val penalty = (distance * 0.1).coerceAtMost(0.5)
                score -= penalty
            }

            candidates.add(Candidate(candidate, score))
        }

        return candidates.maxByOrNull { it.score }?.dt ?: now.plusHours(1).withMinute(0)
    }

    /**
     * 0–1 score for a specific hour+dayOfWeek slot for a category.
     * Higher = better time to send a notification.
     */
    fun slotScore(
        records:    List<UsageRecord>,
        category:   TaskCategory,
        hour:       Int,
        dayOfWeek:  Int,
        useSampling: Boolean = false
    ): Double {
        val relevant = records.filter {
            it.hour == hour &&
                    it.dayOfWeek == dayOfWeek &&
                    (it.category == category ||
                            categoryGroup(it.category) == categoryGroup(category))
        }

        val now = System.currentTimeMillis()

        // ── Bayesian Update (Beta Distribution) ───────────────────────
        var alpha = PRIOR_ALPHA
        var beta  = PRIOR_BETA

        relevant.forEach { r ->
            val ageDays = (now - r.timestampMs) / 86_400_000.0
            val w       = exp(-DECAY_LAMBDA * ageDays)
            val catW    = if (r.category == category) 1.0 else 0.4

            val outcome = outcomeScore(r)
            // Map outcome to pseudo-counts
            alpha += outcome * w * catW
            beta  += (1.0 - outcome) * w * catW
        }

        val score = if (useSampling) {
            thompsonSample(alpha, beta)
        } else {
            alpha / (alpha + beta) // Expected value
        }

        // ── Fatigue penalty: recent ignores in this slot ──────────────
        val recentIgnores = records.count {
            it.hour == hour &&
                    it.dayOfWeek == dayOfWeek &&
                    it.event == UsageEvent.IGNORED &&
                    (now - it.timestampMs) < 7 * 86_400_000L  // last 7 days
        }
        val fatiguePenalty = min(recentIgnores * 0.08, 0.35)

        // ── Dead Space Penalty (Learned Sleep/Inactivity) ─────────────
        val dead = deadSpaceScore(records, hour, dayOfWeek)
        
        // ── Night penalty (fallback hard — 23:00–06:00) ───────────────
        val nightPenalty = if (hour in 23..23 || hour in 0..6) 0.80 else 0.0

        return (score - fatiguePenalty - max(dead, nightPenalty)).coerceIn(0.0, 1.0)
    }

    /**
     * Thompson Sampling from a Beta(alpha, beta) distribution.
     * Used for balanced exploration/exploitation.
     */
    private fun thompsonSample(alpha: Double, beta: Double): Double {
        val random = Random()
        val mean = alpha / (alpha + beta)
        // Variance of Beta distribution
        val variance = (alpha * beta) / ((alpha + beta) * (alpha + beta) * (alpha + beta + 1.0))
        val stdDev = sqrt(variance)
        
        // Simple Gaussian approximation for sampling
        return (mean + random.nextGaussian() * stdDev).coerceIn(0.0, 1.0)
    }

    /**
     * Calculates a real-time focus score based on device usage entropy.
     * High entropy = distraction, Low entropy + high depth = flow.
     */
    fun focusEntropyScore(records: List<UsageRecord>): Double {
        val now = System.currentTimeMillis()
        val recent = records.filter { (now - it.timestampMs) < 2 * 3600_000L && it.entropy >= 0 }
        
        if (recent.isEmpty()) return 0.5 // Neutral

        val avgEntropy = recent.map { it.entropy }.average()
        val avgDepth   = recent.map { it.sessionDepth }.filter { it >= 0 }.average()

        // 0.0 entropy = perfect focus, > 4.0 = extreme switching
        val entropyPenalty = (avgEntropy / 4.0).coerceIn(0.0, 1.0)
        
        // < 10s depth = shallow, > 120s = deep
        val depthBonus = if (avgDepth.isNaN()) 0.0 else (avgDepth / 120.0).coerceIn(0.0, 0.3)

        return (0.7 - entropyPenalty + depthBonus).coerceIn(0.0, 1.0)
    }

    /**
     * Probability that the user is actively using the device at this hour/dow.
     */
    fun usageDensityScore(records: List<UsageRecord>, hour: Int, dayOfWeek: Int): Double {
        val relevant = records.filter { it.hour == hour && it.dayOfWeek == dayOfWeek }
        if (relevant.isEmpty()) return 0.5
        
        val active = relevant.count { it.screenOnMinutes > 0 || it.unlockCount > 0 }
        return active.toDouble() / relevant.size
    }

    /**
     * Probability that the user is using entertainment/slack apps at this time.
     */
    fun slackScore(records: List<UsageRecord>, hour: Int, dayOfWeek: Int): Double {
        val relevant = records.filter { it.hour == hour && it.dayOfWeek == dayOfWeek }
        if (relevant.isEmpty()) return 0.0
        
        val slackEvents = relevant.count { isSlackApp(it.activeApp) }
        return slackEvents.toDouble() / relevant.size
    }

    /**
     * Probability that this is a "Dead Space" (e.g. sleep) based on historical inactivity.
     */
    fun deadSpaceScore(records: List<UsageRecord>, hour: Int, dayOfWeek: Int): Double {
        val relevant = records.filter { it.hour == hour && it.dayOfWeek == dayOfWeek }
        if (relevant.size < 3) return 0.0
        
        // Consistent zero activity across multiple days for this slot
        val inactiveDays = relevant.count { 
            (it.screenOnMinutes == 0 && it.unlockCount == 0) || it.wasIdle 
        }
        return (inactiveDays.toDouble() / relevant.size).coerceIn(0.0, 1.0)
    }

    private fun isSlackApp(pkg: String): Boolean {
        val slackApps = setOf(
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.spotify.music",
            "com.valvesoftware.android.steam.community",
            "com.instagram.android",
            "com.twitter.android",
            "com.whatsapp",
            "org.telegram.messenger",
            "com.skype.raider",
            "com.discord"
        )
        return pkg in slackApps
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
        val fatigued      = isFatigued(records)
        val avgResp       = avgResponseMinutes(records, category)

        val sb = StringBuilder()
        sb.appendLine("Best time: ${formatHour(bestHour)}")
        sb.appendLine("Worst time: ${formatHour(worstHour)}")
        sb.appendLine("Response rate: ${(effectiveness * 100).toInt()}%")
        
        // --- Analytical Insights ---
        val highSlackHour = profile.indices.maxByOrNull { slackScore(records, it, dow) } ?: -1
        if (highSlackHour >= 0 && slackScore(records, highSlackHour, dow) > 0.4) {
            sb.appendLine("Engagement peak: ${formatHour(highSlackHour)} (Slack window)")
        }
        
        val focusScore = focusEntropyScore(records)
        if (focusScore > 0.7) sb.appendLine("Currently in flow state 🧠")
        else if (focusScore < 0.3) sb.appendLine("High distraction detected ⚡")

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
    private fun categoryGroup(cat: TaskCategory): Int = when (cat.label) {
        "Work", "Study" -> 1
        "Personal"      -> 2
        else            -> 3
    }

    private fun formatHour(h: Int): String {
        val suffix = if (h < 12) "AM" else "PM"
        val display = if (h % 12 == 0) 12 else h % 12
        return "$display $suffix"
    }
}