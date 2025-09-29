
package com.algo1127.mytask

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.algo1127.mytask.ui.DashboardScreen
import com.algo1127.mytask.ui.theme.MyTaskTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CALENDAR] == true &&
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true &&
            permissions[Manifest.permission.WRITE_CALENDAR] == true) {
            // Permissions granted, proceed
        } else {
            // Handle permission denial (e.g., show dialog or disable calendar sync)
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
            Manifest.permission.WRITE_CALENDAR
        )
        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            // Permissions already granted
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }
}
