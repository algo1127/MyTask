package com.algo1127.mytask.NotifAi

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DeviceContext(
    val timestampMs:      Long,
    val screenOnMinutes:  Int,      // screen-on time in the last hour
    val unlockCount:      Int,      // unlocks in the last hour
    val activeAppPackage: String,   // most recent foreground app
    val isIdle:           Boolean,  // no unlocks in 30+ min
    val inFocusApp:       Boolean   // user in call/meeting/video
) {
    companion object {
        val FOCUS_APPS = setOf(
            "com.google.android.apps.meetings",
            "us.zoom.videomeetings",
            "com.microsoft.teams",
            "com.discord",
            "com.skype.raider"
        )

        fun fromPrefs(prefs: SharedPreferences, key: String): DeviceContext? {
            val raw = prefs.getString(key, null) ?: return null
            return try {
                val o = JSONObject(raw)
                DeviceContext(
                    timestampMs      = o.getLong("ts"),
                    screenOnMinutes  = o.getInt("som"),
                    unlockCount      = o.getInt("uc"),
                    activeAppPackage = o.getString("app"),
                    isIdle           = o.getBoolean("idle"),
                    inFocusApp       = o.getBoolean("focus")
                )
            } catch (e: Exception) { null }
        }
    }

    fun isFocusApp(pkg: String) = pkg in FOCUS_APPS

    fun toJson(): JSONObject = JSONObject().apply {
        put("ts",    timestampMs)
        put("som",   screenOnMinutes)
        put("uc",    unlockCount)
        put("app",   activeAppPackage)
        put("idle",  isIdle)
        put("focus", inFocusApp)
    }
}

class UsageAccessCollector(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val ctx = applicationContext
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val oneHourAgo = now - 3_600_000L

            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                oneHourAgo, now
            ) ?: emptyList()

            // Screen-on time: sum of foreground time across all apps (proxy)
            val screenOnMs = stats.sumOf { it.totalTimeInForeground }
            val screenOnMinutes = (screenOnMs / 60_000L).toInt().coerceAtMost(60)

            // Most recently used app
            val mostRecent = stats
                .filter { it.totalTimeInForeground > 0 }
                .maxByOrNull { it.lastTimeUsed }
            val activeApp = mostRecent?.packageName ?: ""

            // Unlock count: approximate via querying interactive events
            val events = usm.queryEvents(oneHourAgo, now)
            var unlocks = 0
            var lastEventTime = 0L
            val eventObj = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(eventObj)
                if (eventObj.eventType == android.app.usage.UsageEvents.Event.KEYGUARD_HIDDEN) {
                    unlocks++
                }
                lastEventTime = maxOf(lastEventTime, eventObj.timeStamp)
            }

            val idleThreshold = 30 * 60_000L
            val isIdle = (now - lastEventTime) > idleThreshold

            val inFocusApp = DeviceContext.FOCUS_APPS.any { pkg ->
                stats.any { it.packageName == pkg && (now - it.lastTimeUsed) < 600_000L }
            }

            val dc = DeviceContext(
                timestampMs      = now,
                screenOnMinutes  = screenOnMinutes,
                unlockCount      = unlocks,
                activeAppPackage = activeApp,
                isIdle           = isIdle,
                inFocusApp       = inFocusApp
            )

            // Persist latest snapshot
            ctx.getSharedPreferences("device_context", Context.MODE_PRIVATE)
                .edit()
                .putString("latest", dc.toJson().toString())
                .apply()

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("UsageAccessCollector", "Failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<UsageAccessCollector>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "usage_access_collection",
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }

        fun getLatest(context: Context): DeviceContext? {
            val prefs = context.getSharedPreferences("device_context", Context.MODE_PRIVATE)
            return DeviceContext.fromPrefs(prefs, "latest")
        }
    }
}