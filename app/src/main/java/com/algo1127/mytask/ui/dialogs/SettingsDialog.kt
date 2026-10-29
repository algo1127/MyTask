package com.algo1127.mytask.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
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
    onOpenTimetable: () -> Unit = {},
    highReliabilityEnabled: Boolean,
    onToggleHighReliability: (Boolean) -> Unit,
    aiPreferences: Map<String, String>,
    onPreferenceChanged: (String, String) -> Unit
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
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsNavigationItem(
                    label = "AI Knowledge Base",
                    subtitle = "See what the AI knows about you",
                    icon = Icons.Default.Psychology,
                    color = Theme.Teal,
                    onClick = { onOpenAiKnowledge(); onDismiss() }
                )

                SettingsNavigationItem(
                    label = "Timetable Schedule",
                    subtitle = "Manage recurring weekly class schedule",
                    icon = Icons.Default.Schedule,
                    color = Theme.Teal,
                    onClick = { onOpenTimetable(); onDismiss() }
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

                Text("Task Swipe Gestures", color = Theme.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                GestureSettingItem(
                    label = "Swipe Right",
                    currentAction = aiPreferences["gesture_right_action"] ?: "complete",
                    onActionSelected = { onPreferenceChanged("gesture_right_action", it) }
                )

                GestureSettingItem(
                    label = "Swipe Left",
                    currentAction = aiPreferences["gesture_left_action"] ?: "delete",
                    onActionSelected = { onPreferenceChanged("gesture_left_action", it) }
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
private fun GestureSettingItem(
    label: String,
    currentAction: String,
    onActionSelected: (String) -> Unit
) {
    val options = listOf("complete" to "Complete", "delete" to "Delete", "edit" to "Edit")
    val selectedIndex = options.indexOfFirst { it.first == currentAction }.coerceAtLeast(0)
    
    val getOptionColor = { action: String ->
        when (action) {
            "complete" -> Theme.PastelGreen
            "delete" -> Color.Red
            "edit" -> Theme.Blue
            else -> Theme.White60
        }
    }

    Column {
        Text(label, color = Theme.White60, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Theme.White06)
                .padding(4.dp)
        ) {
            val totalWidth = maxWidth
            val itemWidth = totalWidth / options.size
            
            val indicatorStart by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
                label = "gestureIndicatorStart"
            )
            
            val accentColor by animateColorAsState(
                targetValue = getOptionColor(currentAction),
                animationSpec = tween(400),
                label = "gestureAccentColor"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorStart)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                options.forEach { (value, name) ->
                    val isSelected = currentAction == value
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onActionSelected(value) }
                    ) {
                        Text(
                            name,
                            color = if (isSelected) accentColor else Theme.White30,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
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
