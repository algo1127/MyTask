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
    val hour:           Int,
    val minute:         Int,
    val dayOfWeek:      Int,
    val responseTimeMs: Long = -1L,
    // New — null until UsageAccessCollector has run at least once
    val screenOnMinutes: Int = -1,
    val unlockCount:     Int = -1,
    val activeApp:       String = "",
    val wasIdle:         Boolean = false,
    val wasInFocusApp:   Boolean = false,
    val entropy:         Float = -1f,
    val sessionDepth:    Int = -1
)

// ─── Tracker ─────────────────────────────────────────────────────────────────

/**
 * Persistent rolling buffer of the last MAX_RECORDS interactions.
 * Stored as a JSON array in SharedPreferences — no Room dep needed.
 */
class UsageTracker(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("usage_tracker", Context.MODE_PRIVATE)

    init {
        seedIfEmpty()
    }

    private fun seedIfEmpty() {
        if (load().isEmpty()) {
            android.util.Log.d("UsageTracker", "Seeding initial AI data patterns...")
            save(SeedData.getPresetRecords())
        }
    }

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
        responseTimeMs: Long = -1L,
        deviceCtx: DeviceContext? = null
    ) {
        val now = LocalDateTime.now()
        val rec = UsageRecord(
            timestampMs     = System.currentTimeMillis(),
            event           = event,
            category        = category,
            taskId          = taskId,
            hour            = now.hour,
            minute          = now.minute,
            dayOfWeek       = now.dayOfWeek.value,
            responseTimeMs  = responseTimeMs,
            screenOnMinutes = deviceCtx?.screenOnMinutes ?: -1,
            unlockCount     = deviceCtx?.unlockCount ?: -1,
            activeApp       = deviceCtx?.activeAppPackage ?: "",
            wasIdle         = deviceCtx?.isIdle ?: false,
            wasInFocusApp   = deviceCtx?.inFocusApp ?: false,
            entropy         = deviceCtx?.entropy ?: -1f,
            sessionDepth    = deviceCtx?.sessionDepth ?: -1
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
                        category       = try {
                            val catRaw = obj.getString("cat")
                            if (catRaw.startsWith("{")) {
                                com.google.gson.Gson().fromJson(catRaw, TaskCategory::class.java)
                            } else {
                                TaskCategory.valueOf(catRaw)
                            }
                        } catch (e: Exception) {
                            TaskCategory.Work
                        },
                        taskId         = obj.getLong("tid"),
                        hour           = obj.getInt("h"),
                        minute         = obj.optInt("m", 0),
                        dayOfWeek      = obj.getInt("dow"),
                        responseTimeMs = obj.optLong("rt", -1L),
                        screenOnMinutes = obj.optInt("som", -1),
                        unlockCount     = obj.optInt("uc", -1),
                        activeApp       = obj.optString("app", ""),
                        wasIdle         = obj.optBoolean("idle", false),
                        wasInFocusApp   = obj.optBoolean("focus", false),
                        entropy         = obj.optDouble("ent", -1.0).toFloat(),
                        sessionDepth    = obj.optInt("sdp", -1)
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
                put("cat", com.google.gson.Gson().toJson(r.category))
                put("tid", r.taskId)
                put("h",   r.hour)
                put("m",   r.minute)
                put("dow", r.dayOfWeek)
                put("rt",  r.responseTimeMs)
                put("som",   r.screenOnMinutes)
                put("uc",    r.unlockCount)
                put("app",   r.activeApp)
                put("idle",  r.wasIdle)
                put("focus", r.wasInFocusApp)
                put("ent",   r.entropy)
                put("sdp",   r.sessionDepth)
            })
        }
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply()
    }
}