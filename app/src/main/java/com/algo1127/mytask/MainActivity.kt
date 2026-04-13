package com.algo1127.mytask

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.algo1127.mytask.NotifAi.EvaluationWorker
import com.algo1127.mytask.NotifAi.EventScheduler
import com.algo1127.mytask.ui.DashboardScreen
import com.algo1127.mytask.ui.theme.MyTaskTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            android.util.Log.d("MainActivity", "✅ All permissions granted")
            initNotificationSystem()
        } else {
            android.util.Log.w("MainActivity", "⚠️ Some permissions denied — limited functionality")
            // Still init with whatever we have — daily summary doesn't need calendar
            initNotificationSystem()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()
        setContent {
            MyTaskTheme {
                DashboardScreen()
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            add(Manifest.permission.READ_CALENDAR)
            add(Manifest.permission.WRITE_CALENDAR)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            android.util.Log.d("MainActivity", "✅ Permissions already granted")
            initNotificationSystem()
        } else {
            android.util.Log.d("MainActivity", "Requesting permissions...")
            requestPermissionLauncher.launch(permissions)
        }
    }

    /**
     * Called once permissions are resolved (granted OR denied).
     * Sets up all three pillars of the notification system:
     *   1. Periodic task evaluation (every 15 min)
     *   2. Daily 9 PM event summary
     *   3. Per-event countdown reminders (called from wherever events are saved)
     */
    private fun initNotificationSystem() {
        // ── 1. Periodic task evaluator ─────────────────────────────────
        // enqueueUniquePeriodicWork prevents duplicate workers on every launch
        val evaluationRequest = PeriodicWorkRequestBuilder<EvaluationWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "task_evaluation",                  // unique name — won't duplicate
            ExistingPeriodicWorkPolicy.KEEP,    // if already running, leave it
            evaluationRequest
        )
        android.util.Log.d("MainActivity", "✅ EvaluationWorker scheduled (unique, 15 min)")

        // ── 2. Daily 9 PM summary ──────────────────────────────────────
        // Self-reschedules every night inside DailySummaryWorker
        EventScheduler.scheduleDailySummary(this)
        android.util.Log.d("MainActivity", "✅ Daily summary scheduled")

        // ── 3. Event countdown reminders ───────────────────────────────
        // These are scheduled per-event when an event is created/updated.
        // Call EventScheduler.scheduleReminders(context, event) wherever
        // you save an EventItem — e.g. in your ViewModel or repository:
        //
        //   viewModelScope.launch {
        //       repository.saveEvent(event)
        //       EventScheduler.scheduleReminders(applicationContext, event)
        //   }
        //
        // And cancel on delete:
        //   EventScheduler.cancelReminders(applicationContext, event.id)
    }
}