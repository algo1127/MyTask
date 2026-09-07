package com.algo1127.mytask.ui

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.NotifAi.PermissionManager

@Composable
fun PermissionsScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val hasNotifications = remember(refreshTrigger) { PermissionManager.hasNotificationPermission(context) }
    val hasUsage = remember(refreshTrigger) { PermissionManager.hasUsageAccess(context) }
    val hasExactAlarms = remember(refreshTrigger) { PermissionManager.hasExactAlarmPermission(context) }
    val isBatteryOptimized = remember(refreshTrigger) { PermissionManager.isBatteryOptimizationIgnored(context) }

    Box(modifier = Modifier.fillMaxSize().background(Theme.BgDeep)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Theme.White06)) {
                    Icon(Icons.Default.Close, null, tint = Theme.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("Setup Permissions", color = Theme.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "To provide the best AI guidance and ensure your reminders are always on time, MyTask requires a few special permissions.",
                color = Theme.White30, fontSize = 14.sp, lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            PermissionCard(
                title = "Usage Access",
                description = "Allows the AI to learn from your device usage patterns (offline) to find your perfect focus windows.",
                icon = Icons.Default.Psychology,
                color = Theme.Teal,
                isGranted = hasUsage,
                onGrant = { PermissionManager.requestUsageAccess(context); refreshTrigger++ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                title = "Exact Alarms",
                description = "Ensures your reminders fire at the exact millisecond, even if the device is in deep sleep.",
                icon = Icons.Default.Timer,
                color = Theme.Orange,
                isGranted = hasExactAlarms,
                onGrant = { PermissionManager.requestExactAlarmPermission(context); refreshTrigger++ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                title = "Battery Optimization",
                description = "Exempts MyTask from system sleep rules so it can continue learning and reminding you in the background.",
                icon = Icons.Default.BatteryChargingFull,
                color = Theme.Emerald,
                isGranted = isBatteryOptimized,
                onGrant = { PermissionManager.requestBatteryOptimizationExemption(context); refreshTrigger++ }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(16.dp))
                PermissionCard(
                    title = "Notifications",
                    description = "Required to send you reminders and AI insights.",
                    icon = Icons.Default.Notifications,
                    color = Theme.Blue,
                    isGranted = hasNotifications,
                    onGrant = { /* Handled by standard launcher if needed, or just link to settings */ }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Theme.Teal)
            ) {
                Text("All Done", color = Theme.BgDeep, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Surface(
        color = Theme.White03,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Theme.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(description, color = Theme.White30, fontSize = 12.sp, lineHeight = 18.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, null, tint = Theme.Teal, modifier = Modifier.size(28.dp))
            } else {
                IconButton(
                    onClick = onGrant,
                    modifier = Modifier.clip(CircleShape).background(Theme.White06)
                ) {
                    Icon(Icons.Default.ArrowForward, null, tint = Theme.White)
                }
            }
        }
    }
}
