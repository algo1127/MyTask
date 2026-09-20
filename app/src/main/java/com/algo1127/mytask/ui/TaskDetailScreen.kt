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
import com.algo1127.mytask.ui.dialogs.AddTaskDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
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
    onDelete: () -> Unit,
    onUpdate: (TaskItem) -> Unit = {}
) {
    val themeColor = task.category.color
    val scrollState = rememberScrollState()
    
    Box(modifier = Modifier.fillMaxSize().background(Theme.BgDeep)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scrollState).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Theme.White06)) { Icon(Icons.Default.Close, null, tint = Theme.White) }
                IconButton(onClick = onDelete, modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Surface(color = themeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(task.category.icon, null, tint = themeColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(task.category.label, color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = task.title, color = Theme.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(32.dp))
            Surface(onClick = onToggleCompletion, color = if (isCompleted) themeColor.copy(alpha = 0.1f) else Theme.White03, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().border(width = 2.dp, color = if (isCompleted) themeColor else Theme.White06, shape = RoundedCornerShape(28.dp))) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isCompleted) themeColor else Theme.White06).shadow(if (isCompleted) 12.dp else 0.dp, CircleShape, spotColor = themeColor)) {
                        Icon(imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isCompleted) Theme.BgDeep else Theme.White30, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(text = if (isCompleted) "Completed" else "Mark as Done", color = if (isCompleted) themeColor else Theme.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = if (isCompleted) "Great job! Tap to undo." else "Tap to complete this task", color = Theme.White30, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            // Reminder Section
            var showReminderPicker by remember { mutableStateOf(false) }
            Surface(color = if (task.reminderDateTime != null) Theme.Teal.copy(alpha = 0.1f) else Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().clickable { showReminderPicker = true }) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = if (task.reminderDateTime != null) Theme.Teal else Theme.White30, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Reminder", color = Theme.White30, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = task.reminderDateTime?.format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) ?: "No reminder set", color = if (task.reminderDateTime != null) Theme.Teal else Theme.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (showReminderPicker) {
                com.algo1127.mytask.ui.dialogs.ReminderPickerOverlay(initialDateTime = task.reminderDateTime ?: LocalDateTime.now().plusHours(1), onDismiss = { showReminderPicker = false }, onConfirm = { onUpdate(task.copy(reminderDateTime = it)); showReminderPicker = false }, onRemove = { onUpdate(task.copy(reminderDateTime = null)); showReminderPicker = false })
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(color = Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailRow(icon = Icons.Default.Event, label = "Scheduled for", value = task.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")))
                    DetailRow(icon = Icons.Default.Schedule, label = "Time", value = task.time)
                }
            }
        }
    }
}

@Composable
fun PersistentTaskDetailScreen(
    task: com.algo1127.mytask.ui.models.Task,
    onDismiss: () -> Unit,
    onToggleCompletion: () -> Unit,
    onToggleSubtaskCompletion: (Long, Boolean) -> Unit = { _, _ -> },
    onAddSubtask: (
        title: String,
        timePreference: com.algo1127.mytask.ui.models.TimePreference,
        category: TaskCategory,
        date: java.time.LocalDate,
        description: String?,
        dueDate: java.time.LocalDate?,
        reminderDateTime: java.time.LocalDateTime?,
        estimatedEffort: java.time.Duration?
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSubtaskClick: (com.algo1127.mytask.ui.models.Subtask) -> Unit = {},
    onEditRequest: (com.algo1127.mytask.ui.models.Task) -> Unit = {},
    onDeleteSubtask: (Long) -> Unit = {},
    categories: List<TaskCategory> = emptyList(),
    onDelete: () -> Unit,
    onUpdate: (com.algo1127.mytask.ui.models.Task) -> Unit = {}
) {
    val themeColor = task.category.color
    val scrollState = rememberScrollState()
    val isCompleted = task.progress >= 1.0f

    Box(modifier = Modifier.fillMaxSize().background(Theme.BgDeep)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scrollState).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Theme.White06)) { Icon(Icons.Default.Close, null, tint = Theme.White) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { onEditRequest(task) }, modifier = Modifier.clip(CircleShape).background(Theme.White06)) { Icon(Icons.Default.Edit, null, tint = Theme.White) }
                    IconButton(onClick = onDelete, modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Surface(color = themeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(task.category.icon, null, tint = themeColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(task.category.label, color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = task.title, color = Theme.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp, modifier = Modifier.fillMaxWidth())
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = task.description, color = Theme.White60, fontSize = 16.sp, lineHeight = 24.sp)
            }
            
            if (task.subtasks.isEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Surface(onClick = onToggleCompletion, color = if (isCompleted) themeColor.copy(alpha = 0.1f) else Theme.White03, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().border(width = 2.dp, color = if (isCompleted) themeColor else Theme.White06, shape = RoundedCornerShape(28.dp))) {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isCompleted) themeColor else Theme.White06).shadow(if (isCompleted) 12.dp else 0.dp, CircleShape, spotColor = themeColor)) {
                            Icon(imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isCompleted) Theme.BgDeep else Theme.White30, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(text = if (isCompleted) "Completed" else "Mark as Done", color = if (isCompleted) themeColor else Theme.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(text = if (isCompleted) "Great job! Tap to undo." else "Tap to complete this task", color = Theme.White30, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Subtasks", color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                var showSubtaskCreator by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showSubtaskCreator = true },
                    modifier = Modifier.clip(CircleShape).background(themeColor.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Add, null, tint = themeColor)
                }
                
                if (showSubtaskCreator) {
                    AddTaskDialog(
                        defaultDate = task.dueDate ?: java.time.LocalDate.now(),
                        sourceTab = 1, // Add as task
                        categories = categories,
                        onDismiss = { showSubtaskCreator = false },
                        onAdd = { title, timePref, cat, date, desc, _, due, duration, reminder ->
                            onAddSubtask(title, timePref, cat, date, desc, due, reminder, duration)
                            showSubtaskCreator = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                task.subtasks.forEach { sub ->
                    val subSwipeState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onDeleteSubtask(sub.id)
                                    true
                                }
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    onToggleSubtaskCompletion(sub.id, !sub.isCompleted)
                                    false
                                }
                                else -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = subSwipeState,
                        backgroundContent = {
                            val direction = subSwipeState.dismissDirection
                            val color = if (direction == SwipeToDismissBoxValue.StartToEnd) Theme.PastelGreen else if (direction == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                            val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Check else Icons.Default.Delete
                            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(color), contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) { Icon(icon, null, tint = Theme.BgDeep, modifier = Modifier.padding(horizontal = 16.dp)) }
                        },
                        content = {
                            SubtaskRow(
                                subtask = sub,
                                onToggle = { onToggleSubtaskCompletion(sub.id, !sub.isCompleted) },
                                onClick = { onSubtaskClick(sub) }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            var showReminderPicker by remember { mutableStateOf(false) }
            Surface(color = if (task.reminderDateTime != null) Theme.Teal.copy(alpha = 0.1f) else Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().clickable { showReminderPicker = true }) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = if (task.reminderDateTime != null) Theme.Teal else Theme.White30, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Reminder", color = Theme.White30, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = task.reminderDateTime?.format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) ?: "No reminder set", color = if (task.reminderDateTime != null) Theme.Teal else Theme.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (showReminderPicker) {
                com.algo1127.mytask.ui.dialogs.ReminderPickerOverlay(initialDateTime = task.reminderDateTime ?: LocalDateTime.now().plusHours(1), onDismiss = { showReminderPicker = false }, onConfirm = { onUpdate(task.copy(reminderDateTime = it)); showReminderPicker = false }, onRemove = { onUpdate(task.copy(reminderDateTime = null)); showReminderPicker = false })
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(color = Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailRow(icon = Icons.Default.CalendarToday, label = "Created on", value = task.createdAt.format(DateTimeFormatter.ofPattern("EEEE, MMM d")))
                    DetailRow(icon = Icons.Default.Event, label = "Due Date", value = task.dueDate?.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) ?: "No due date")
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SubtaskDetailScreen(
    subtask: com.algo1127.mytask.ui.models.Subtask,
    onDismiss: () -> Unit,
    onToggleCompletion: () -> Unit,
    onEditRequest: (com.algo1127.mytask.ui.models.Subtask) -> Unit = {},
    onDelete: () -> Unit,
    onUpdate: (com.algo1127.mytask.ui.models.Subtask) -> Unit = {}
) {
    val themeColor = subtask.category.color
    val scrollState = rememberScrollState()
    val isCompleted = subtask.isCompleted

    Box(modifier = Modifier.fillMaxSize().background(Theme.BgDeep)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scrollState).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Theme.White06)) { Icon(Icons.Default.Close, null, tint = Theme.White) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { onEditRequest(subtask) }, modifier = Modifier.clip(CircleShape).background(Theme.White06)) { Icon(Icons.Default.Edit, null, tint = Theme.White) }
                    IconButton(onClick = onDelete, modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Surface(color = themeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(subtask.category.icon, null, tint = themeColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(subtask.category.label, color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = subtask.title, color = Theme.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp, modifier = Modifier.fillMaxWidth())
            if (subtask.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = subtask.description, color = Theme.White60, fontSize = 16.sp, lineHeight = 24.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))

            Surface(onClick = onToggleCompletion, color = if (isCompleted) themeColor.copy(alpha = 0.1f) else Theme.White03, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().border(width = 2.dp, color = if (isCompleted) themeColor else Theme.White06, shape = RoundedCornerShape(28.dp))) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isCompleted) themeColor else Theme.White06).shadow(if (isCompleted) 12.dp else 0.dp, CircleShape, spotColor = themeColor)) {
                        Icon(imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isCompleted) Theme.BgDeep else Theme.White30, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(text = if (isCompleted) "Completed" else "Mark as Done", color = if (isCompleted) themeColor else Theme.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = if (isCompleted) "Great job! Tap to undo." else "Tap to complete this subtask", color = Theme.White30, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            var showReminderPicker by remember { mutableStateOf(false) }
            Surface(color = if (subtask.reminderDateTime != null) Theme.Teal.copy(alpha = 0.1f) else Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().clickable { showReminderPicker = true }) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = if (subtask.reminderDateTime != null) Theme.Teal else Theme.White30, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Reminder", color = Theme.White30, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = subtask.reminderDateTime?.format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) ?: "No reminder set", color = if (subtask.reminderDateTime != null) Theme.Teal else Theme.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (showReminderPicker) {
                com.algo1127.mytask.ui.dialogs.ReminderPickerOverlay(initialDateTime = subtask.reminderDateTime ?: LocalDateTime.now().plusHours(1), onDismiss = { showReminderPicker = false }, onConfirm = { onUpdate(subtask.copy(reminderDateTime = it)); showReminderPicker = false }, onRemove = { onUpdate(subtask.copy(reminderDateTime = null)); showReminderPicker = false })
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(color = Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailRow(icon = Icons.Default.Event, label = "Due Date", value = subtask.dueDate?.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) ?: "No due date")
                    DetailRow(
                        icon = Icons.Default.Timer, 
                        label = "Est. Duration", 
                        value = subtask.estimatedEffort?.let { "${it.toMinutes()}m" } ?: "Not set"
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, valueColor: Color = Theme.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Theme.White30, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
