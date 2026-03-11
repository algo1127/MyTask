package com.algo1127.mytask

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.algo1127.mytask.NotifAi.EvaluationWorker
import com.algo1127.mytask.ui.DashboardScreen
import com.algo1127.mytask.ui.theme.MyTaskTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CALENDAR] == true &&
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true &&
            permissions[Manifest.permission.WRITE_CALENDAR] == true) {
            // ✅ All permissions granted
            android.util.Log.d("MainActivity", "All permissions granted!")
            scheduleTestEvaluation()  // Schedule test evaluation after permissions granted
        } else {
            // ⚠️ Handle permission denial
            android.util.Log.w("MainActivity", "Some permissions denied:")
            permissions.forEach { (perm, granted) ->
                if (!granted) android.util.Log.w("MainActivity", "  - $perm: DENIED")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCalendarPermissions()
        setContent {
            MyTaskTheme {
                DashboardScreen()
            }
        }
    }

    private fun requestCalendarPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.POST_NOTIFICATIONS  // ✅ Added for Android 13+
        )
        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            // ✅ Permissions already granted
            android.util.Log.d("MainActivity", "Permissions already granted!")
            scheduleTestEvaluation()  // Schedule test evaluation
        } else {
            // 📋 Request permissions
            android.util.Log.d("MainActivity", "Requesting permissions...")
            requestPermissionLauncher.launch(permissions)
        }
    }

    // ✅ NEW: Force EvaluationWorker to run 5 seconds after app launch (for testing)
    private fun scheduleTestEvaluation() {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val workRequest = PeriodicWorkRequestBuilder<EvaluationWorker>(15, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(this).enqueue(workRequest)
                android.util.Log.d("MainActivity", "✅ EvaluationWorker scheduled! Check logs in ~1 min")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error scheduling EvaluationWorker: ${e.message}", e)
            }
        }, 5000)  // 5 second delay
    }
}