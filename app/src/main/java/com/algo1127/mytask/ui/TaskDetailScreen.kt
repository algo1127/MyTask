package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.max

@Composable
fun TaskDetailScreen(
    task: TaskItem,
    isCompleted: Boolean,
    onDismiss: () -> Unit,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = task.category.color
    val scrollState = rememberScrollState()
    
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.BgDeep)
    ) {
        // Unique Task-specific background: Mesh-like moving gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center1 = Offset(
                x = size.width * (0.2f + 0.3f * cos(phase)),
                y = size.height * (0.3f + 0.2f * sin(phase * 0.5f))
            )
            val center2 = Offset(
                x = size.width * (0.8f + 0.2f * sin(phase * 0.8f)),
                y = size.height * (0.7f + 0.3f * cos(phase))
            )
            
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(themeColor.copy(alpha = 0.15f), Color.Transparent),
                    center = center1,
                    radius = max(size.width, size.height) * 0.7f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Theme.Purple.copy(alpha = 0.05f), Color.Transparent),
                    center = center2,
                    radius = max(size.width, size.height) * 0.6f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.clip(CircleShape).background(Theme.White06)
                ) {
                    Icon(Icons.Default.Close, null, tint = Theme.White)
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Category Badge
            Surface(
                color = themeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(task.category.icon, null, tint = themeColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(task.category.label, color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task Title
            Text(
                text = task.title,
                color = Theme.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Completion Toggle Card (The Hero Action)
            Surface(
                onClick = onToggleCompletion,
                color = if (isCompleted) themeColor.copy(alpha = 0.1f) else Theme.White03,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = if (isCompleted) themeColor else Theme.White06,
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) themeColor else Theme.White06)
                            .shadow(if (isCompleted) 12.dp else 0.dp, CircleShape, spotColor = themeColor)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isCompleted) Theme.BgDeep else Theme.White30,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column {
                        Text(
                            text = if (isCompleted) "Completed" else "Mark as Done",
                            color = if (isCompleted) themeColor else Theme.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCompleted) "Great job! Tap to undo." else "Tap to complete this task",
                            color = Theme.White30,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Card
            Surface(
                color = Theme.White03,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailRow(
                        icon = Icons.Default.Event,
                        label = "Scheduled for",
                        value = task.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
                    )
                    DetailRow(
                        icon = Icons.Default.Schedule,
                        label = "Time",
                        value = task.time
                    )
                    DetailRow(
                        icon = Icons.Default.PriorityHigh,
                        label = "Priority",
                        value = when {
                            task.isUrgent && task.isImportant -> "Critical (Do Now)"
                            task.isImportant -> "Important (Schedule)"
                            task.isUrgent -> "Urgent (Delegate)"
                            else -> "Routine"
                        },
                        valueColor = if (task.isUrgent) Color(0xFFFF6B6B) else Theme.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notes Section
            if (task.notes.isNotBlank()) {
                Surface(
                    color = Theme.White03,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Notes, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Notes", color = Theme.White30, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = task.notes,
                            color = Theme.White,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Theme.White
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Theme.White30, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
