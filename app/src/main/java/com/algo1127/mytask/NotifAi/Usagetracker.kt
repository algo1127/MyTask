package com.algo1127.mytask.NotifAi

import android.content.Context
import android.content.SharedPreferences
import com.algo1127.mytask.ui.TaskCategory
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// ─── Data model ───────────────────────────────────────────────────────────────

enum class UsageEvent {
    NOTIFICATION_SENT,
    COMPLETED,          // tapped Complete on notification
    IGNORED,            // swiped away / no response within window
    FORGOT,             // tapped Forgot
    SKIPPED,
    APP_OPENED,         // user opened the app
    TASK_CREATED        // user created a task at this time
}

data class UsageRecord(
    val timestampMs:    Long,
    val event:          UsageEvent,
    val category:       TaskCategory,
    val taskId:         Long,
    val hour:           Int,            // 0-23
    val dayOfWeek:      Int,            // 1=Mon … 7=Sun
    val responseTimeMs: Long = -1L      // ms between SENT and action; -1 if not applicable
)

// ─── Tracker ─────────────────────────────────────────────────────────────────

/**
 * Persistent rolling buffer of the last MAX_RECORDS interactions.
 * Stored as a JSON array in SharedPreferences — no Room dep needed.
 */
class UsageTracker(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("usage_tracker", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RECORDS   = "records"
        private const val MAX_RECORDS   = 800   // ~3-6 months of normal use
        private const val KEY_LAST_SENT = "last_sent_"  // prefix + taskId → sentTimeMs
    }

    // ── Write ─────────────────────────────────────────────────────────

    fun record(
        event:    UsageEvent,
        category: TaskCategory,
        taskId:   Long,
        responseTimeMs: Long = -1L
    ) {
        val now = LocalDateTime.now()
        val rec = UsageRecord(
            timestampMs    = System.currentTimeMillis(),
            event          = event,
            category       = category,
            taskId         = taskId,
            hour           = now.hour,
            dayOfWeek      = now.dayOfWeek.value,
            responseTimeMs = responseTimeMs
        )
        append(rec)
    }

    /** Mark when a notification was sent so we can compute responseTime on action */
    fun markNotificationSent(taskId: Long) {
        prefs.edit().putLong("$KEY_LAST_SENT$taskId", System.currentTimeMillis()).apply()
    }

    /** Compute response time since last notification for this task */
    fun getResponseTime(taskId: Long): Long {
        val sent = prefs.getLong("$KEY_LAST_SENT$taskId", -1L)
        return if (sent > 0) System.currentTimeMillis() - sent else -1L
    }

    // ── Read ──────────────────────────────────────────────────────────

    fun getAll(): List<UsageRecord> = load()

    /** Only records within the last [days] days */
    fun getRecent(days: Int): List<UsageRecord> {
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        return load().filter { it.timestampMs >= cutoff }
    }

    fun getForCategory(category: TaskCategory): List<UsageRecord> =
        load().filter { it.category == category }

    fun countFor(event: UsageEvent, since: Long = 0L): Int =
        load().count { it.event == event && it.timestampMs >= since }

    // ── Internal ──────────────────────────────────────────────────────

    private fun append(rec: UsageRecord) {
        val list = load().toMutableList()
        list.add(rec)
        // Trim to rolling window — drop oldest
        val trimmed = if (list.size > MAX_RECORDS) list.takeLast(MAX_RECORDS) else list
        save(trimmed)
    }

    private fun load(): List<UsageRecord> {
        val raw = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val obj = arr.getJSONObject(i)
                    UsageRecord(
                        timestampMs    = obj.getLong("ts"),
                        event          = UsageEvent.valueOf(obj.getString("ev")),
                        category       = TaskCategory.valueOf(obj.getString("cat")),
                        taskId         = obj.getLong("tid"),
                        hour           = obj.getInt("h"),
                        dayOfWeek      = obj.getInt("dow"),
                        responseTimeMs = obj.optLong("rt", -1L)
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun save(list: List<UsageRecord>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("ts",  r.timestampMs)
                put("ev",  r.event.name)
                put("cat", r.category.name)
                put("tid", r.taskId)
                put("h",   r.hour)
                put("dow", r.dayOfWeek)
                put("rt",  r.responseTimeMs)
            })
        }
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply()
    }
}