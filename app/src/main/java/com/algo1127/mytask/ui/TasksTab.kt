package com.algo1127.mytask.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun TasksTab(
    tasks: List<TaskItem>,        // ✅ now receives tasks from DashboardScreen
    selectedDate: LocalDate,
    completedTasks: MutableList<Long>,
    onTaskCompleted: (Long) -> Unit
) {
    // ✅ Only show items that were created from the Tasks tab (isReminder == false)
    val visibleTasks = tasks.filter { it.date == selectedDate && !it.isReminder }

    if (visibleTasks.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Task,
            title = "No tasks",
            subtitle = "Tap + to add a task",
            color = Theme.Purple
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visibleTasks, key = { it.id }) { item ->
                val isCompleted = completedTasks.contains(item.id)
                TaskRow(
                    item = item,
                    isCompleted = isCompleted,
                    onToggle = { if (!isCompleted) onTaskCompleted(item.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun TaskRow(
    item: TaskItem,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    var isTapped by remember { mutableStateOf(false) }
    LaunchedEffect(isTapped) { if (isTapped) { delay(150); isTapped = false } }

    val scale by animateFloatAsState(
        targetValue = if (isTapped) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "taskScale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isCompleted)
                        Brush.linearGradient(colors = listOf(Theme.Purple.copy(alpha = 0.12f), Theme.Purple.copy(alpha = 0.06f)))
                    else
                        Brush.linearGradient(colors = listOf(Theme.CardBg, Theme.CardBg))
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    isTapped = true; onToggle()
                }
        ) {
            // Left accent bar — purple for tasks
            Box(
                modifier = Modifier
                    .width(3.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .background(if (isCompleted) Theme.Purple else item.category.color.copy(alpha = 0.8f))
                    .align(Alignment.CenterStart)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox-style icon bubble
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                        .background(if (isCompleted) Theme.Purple.copy(alpha = 0.2f) else item.category.color.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.TaskAlt else item.category.icon,
                        contentDescription = null,
                        tint = if (isCompleted) Theme.Purple else item.category.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = if (isCompleted) Theme.White30 else Theme.White,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(item.category.color.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(item.category.label, color = item.category.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.Schedule, null, tint = Theme.White30, modifier = Modifier.size(12.dp))
                        Text(item.time, color = Theme.White60, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (isCompleted) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).clip(CircleShape).background(Theme.Purple)) {
                        Icon(Icons.Default.Check, null, tint = Theme.BgDeep, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}