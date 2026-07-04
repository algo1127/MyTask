package com.algo1127.mytask

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.algo1127.mytask.NotifAi.NotifAi
import com.algo1127.mytask.NotifAi.UsageAccessCollector

class MyTaskApplication : Application(), Configuration.Provider {

    // 👇 Override the PROPERTY (not a function, not a constructor param)
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            // .setWorkerFactory(...) // Uncomment if using custom factory
            .build()

    lateinit var notifAi: NotifAi

    override fun onCreate() {
        super.onCreate()
        Log.d("AppInit", "🚀 MyTaskApplication onCreate() called")

        try {
            notifAi = NotifAi(this)
            Log.d("AppInit", "🔔 NotifAi initialized")
        } catch (e: Exception) {
            Log.e("AppInit", "❌ Failed to initialize NotifAi: ${e.message}", e)
        }

        try {
            UsageAccessCollector.schedule(this)
            Log.d("AppInit", "✅ UsageAccessCollector scheduled")
        } catch (e: Exception) {
            Log.e("AppInit", "❌ Failed to schedule collector", e)
        }
    }
}