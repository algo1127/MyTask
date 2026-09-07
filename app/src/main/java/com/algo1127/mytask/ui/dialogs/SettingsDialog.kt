package com.algo1127.mytask.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.BuildConfig
import com.algo1127.mytask.ui.Theme

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onOpenAiKnowledge: () -> Unit,
    onOpenCategoryManager: () -> Unit,
    onOpenPermissions: () -> Unit,
    highReliabilityEnabled: Boolean,
    onToggleHighReliability: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text("Settings", color = Theme.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                SettingsNavigationItem(
                    label = "AI Knowledge Base",
                    subtitle = "See what the AI knows about you",
                    icon = Icons.Default.Psychology,
                    color = Theme.Teal,
                    onClick = { onOpenAiKnowledge(); onDismiss() }
                )

                SettingsNavigationItem(
                    label = "Manage Categories",
                    subtitle = "Customize icons and colors",
                    icon = Icons.Default.Category,
                    color = Theme.Purple,
                    onClick = { onOpenCategoryManager(); onDismiss() }
                )
                
                SettingsNavigationItem(
                    label = "Setup Permissions",
                    subtitle = "Ensure all features are active",
                    icon = Icons.Default.Security,
                    color = Theme.Emerald,
                    onClick = { onOpenPermissions(); onDismiss() }
                )

                HorizontalDivider(color = Theme.White06, modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High Reliability", color = Theme.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Fire reminders even in deep sleep", color = Theme.White30, fontSize = 12.sp)
                    }
                    Switch(
                        checked = highReliabilityEnabled,
                        onCheckedChange = { onToggleHighReliability(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Theme.Teal, checkedTrackColor = Theme.Teal.copy(alpha = 0.5f))
                    )
                }

                HorizontalDivider(color = Theme.White06, modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    "App Version: ${BuildConfig.VERSION_NAME}",
                    color = Theme.White10,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Theme.White60)
            }
        }
    )
}

@Composable
private fun SettingsNavigationItem(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Theme.White06,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Theme.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Theme.White30, fontSize = 12.sp)
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = Theme.White10, modifier = Modifier.size(20.dp))
        }
    }
}
