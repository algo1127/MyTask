package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
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
import java.time.format.DateTimeFormatter

@Composable
fun RemindersTab(
    calendarTasks: List<TaskItem>,
    persistentTasks: List<com.algo1127.mytask.ui.models.Task>,
    events: List<EventItem>,
    selectedDate: LocalDate,
    completedIds: Set<Long>,
    onCompleteLegacyReminder: (Long, Boolean) -> Unit,
    onDismissTaskReminder: (com.algo1127.mytask.ui.models.Task) -> Unit,
    onDismissEventReminder: (EventItem) -> Unit,
    onCalendarTaskClick: (TaskItem) -> Unit,
    onPersistentTaskClick: (com.algo1127.mytask.ui.models.Task) -> Unit,
    onEventClick: (EventItem) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val allReminders = remember(calendarTasks, persistentTasks, events, selectedDate) {
        val list = mutableListOf<ReminderViewItem>()
        
        // 1. Calendar Tasks (Legacy Reminders) - Trust the ViewModel's list
        calendarTasks.filter { it.isReminder }.forEach {
            list.add(ReminderViewItem.CalendarTask(it))
        }
        
        // 2. Persistent Tasks with alerts - Alerts are separate from the task itself
        persistentTasks.filter { it.reminderDateTime?.toLocalDate() == selectedDate }.forEach {
            list.add(ReminderViewItem.PersistentTask(it))
        }
        
        // 3. Events with alerts
        events.filter { it.reminderDateTime?.toLocalDate() == selectedDate }.forEach {
            list.add(ReminderViewItem.Event(it))
        }
        
        list.sortedBy { it.sortTime }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp + bottomPadding, top = 12.dp)
    ) {
        if (allReminders.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = "All clear",
                    subtitle = "No alerts for this day",
                    color = Theme.Teal
                )
            }
        } else {
            items(allReminders, key = { it.uniqueId }) { item ->
                ReminderRow(
                    item = item,
                    isCompleted = when(item) {
                        is ReminderViewItem.CalendarTask -> completedIds.contains(item.task.id) || item.task.done
                        is ReminderViewItem.PersistentTask -> false // Task reminders are alerts, not tasks
                        is ReminderViewItem.Event -> false
                    },
                    onToggle = {
                        when(item) {
                            is ReminderViewItem.CalendarTask -> onCompleteLegacyReminder(item.task.id, !(completedIds.contains(item.task.id) || item.task.done))
                            is ReminderViewItem.PersistentTask -> onDismissTaskReminder(item.task)
                            is ReminderViewItem.Event -> onDismissEventReminder(item.event)
                        }
                    },
                    onClick = {
                        when(item) {
                            is ReminderViewItem.CalendarTask -> onCalendarTaskClick(item.task)
                            is ReminderViewItem.PersistentTask -> onPersistentTaskClick(item.task)
                            is ReminderViewItem.Event -> onEventClick(item.event)
                        }
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

private sealed class ReminderViewItem {
    abstract val uniqueId: String
    abstract val title: String
    abstract val timeLabel: String
    abstract val sortTime: String
    abstract val category: TaskCategory
    abstract val typeLabel: String
    abstract val typeColor: Color
    abstract val actionIcon: androidx.compose.ui.graphics.vector.ImageVector

    data class CalendarTask(val task: TaskItem) : ReminderViewItem() {
        override val uniqueId = "cal_${task.id}"
        override val title = task.title
        override val timeLabel = task.time
        override val sortTime = task.time
        override val category = task.category
        override val typeLabel = "Reminder"
        override val typeColor = Theme.Teal
        override val actionIcon = Icons.Default.CheckCircle
    }

    data class PersistentTask(val task: com.algo1127.mytask.ui.models.Task) : ReminderViewItem() {
        override val uniqueId = "pers_${task.id}"
        override val title = task.title
        override val timeLabel = task.reminderDateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
        override val sortTime = timeLabel
        override val category = task.category
        override val typeLabel = "Task Alert"
        override val typeColor = Theme.Purple
        override val actionIcon = Icons.Default.NotificationsOff
    }

    data class Event(val event: EventItem) : ReminderViewItem() {
        override val uniqueId = "evt_${event.id}"
        override val title = event.title
        override val timeLabel = event.reminderDateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
        override val sortTime = timeLabel
        override val category = TaskCategory.Personal
        override val typeLabel = "Event Alert"
        override val typeColor = Theme.Blue
        override val actionIcon = Icons.Default.NotificationsOff
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderRow(
    item: ReminderViewItem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTapped by remember { mutableStateOf(false) }
    LaunchedEffect(isTapped) { if (isTapped) { delay(150); isTapped = false } }

    val scale by animateFloatAsState(
        targetValue = if (isTapped) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "reminderScale"
    )
    
    val accentColor = item.typeColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { isTapped = true; onClick() }
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isCompleted)
                        Brush.linearGradient(colors = listOf(accentColor.copy(alpha = 0.12f), accentColor.copy(alpha = 0.08f)))
                    else
                        Brush.linearGradient(colors = listOf(Theme.CardBg, Theme.CardBg))
                )
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .background(if (isCompleted) accentColor else item.category.color.copy(alpha = 0.8f))
                    .align(Alignment.CenterStart)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                        .background(if (isCompleted) accentColor.copy(alpha = 0.2f) else item.category.color.copy(alpha = 0.12f))
                        .clickable { onToggle() }
                ) {
                    AnimatedContent(isCompleted, label = "iconAnim") { completed ->
                        Icon(
                            imageVector = if (completed) Icons.Default.CheckCircle else item.actionIcon,
                            contentDescription = null,
                            tint = if (completed) accentColor else item.category.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title, color = if (isCompleted) Theme.White30 else Theme.White,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(item.typeLabel, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.Schedule, null, tint = Theme.White30, modifier = Modifier.size(12.dp))
                        Text(item.timeLabel, color = Theme.White60, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                if (isCompleted) {
                    Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(20.dp))
                } else if (item !is ReminderViewItem.CalendarTask) {
                    IconButton(onClick = onToggle) {
                        Icon(Icons.Default.Close, "Dismiss", tint = Theme.White30, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
