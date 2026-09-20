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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTab(
    persistentTasks: List<com.algo1127.mytask.ui.models.Task>,
    calendarTasks: List<TaskItem>,
    selectedDate: LocalDate,
    completedIds: Set<Long>,
    aiPreferences: Map<String, String>,
    onTogglePersistentCompletion: (Long, Boolean) -> Unit,
    onToggleCalendarCompletion: (Long, Boolean) -> Unit,
    onToggleSubtaskCompletion: (Long, Long, Boolean) -> Unit = { _, _, _ -> },
    onPersistentTaskUpdate: (com.algo1127.mytask.ui.models.Task) -> Unit = {},
    onPersistentTaskDelete: (Long) -> Unit = {},
    onPersistentTaskClick: (com.algo1127.mytask.ui.models.Task) -> Unit = {},
    onCalendarTaskUpdate: (TaskItem) -> Unit = {},
    onCalendarTaskDelete: (Long) -> Unit = {},
    onCalendarTaskClick: (TaskItem) -> Unit = {},
    onEditPersistentRequest: (com.algo1127.mytask.ui.models.Task) -> Unit = {},
    onEditCalendarRequest: (TaskItem) -> Unit = {},
    onSubtaskClick: (com.algo1127.mytask.ui.models.Task, com.algo1127.mytask.ui.models.Subtask) -> Unit = { _, _ -> },
    onSubtaskDelete: (Long, Long) -> Unit = { _, _ -> },
    onSubtaskUpdate: (Long, com.algo1127.mytask.ui.models.Subtask) -> Unit = { _, _ -> },
    onEditSubtaskRequest: (com.algo1127.mytask.ui.models.Task, com.algo1127.mytask.ui.models.Subtask) -> Unit = { _, _ -> },
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val allVisibleTasks = remember(persistentTasks, calendarTasks, selectedDate, completedIds) {
        val list = mutableListOf<TaskViewItem>()
        
        // 1. Persistent Tasks: Always show regardless of due date if not Archived
        persistentTasks.filter { 
            it.focusState != com.algo1127.mytask.ui.models.FocusState.Archived
        }.forEach { list.add(TaskViewItem.Persistent(it)) }
        
        // 2. Calendar Tasks: Trust the ViewModel's list (already date-filtered)
        calendarTasks.filter { !it.isReminder }
            .forEach { list.add(TaskViewItem.Calendar(it, completedIds.contains(it.id) || it.done)) }
            
        list.sortedBy { it.isCompleted }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp + bottomPadding, top = 12.dp)
    ) {
        if (allVisibleTasks.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Task,
                    title = "No active tasks",
                    subtitle = "Tap + to add a task",
                    color = Theme.Purple
                )
            }
        } else {
            items(allVisibleTasks, key = { it.uniqueId }) { viewItem ->
                val isCompleted = viewItem.isCompleted
                
                val leftAction = aiPreferences["gesture_left_action"] ?: "delete"
                val rightAction = aiPreferences["gesture_right_action"] ?: "complete"

                val swipeState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        // Disable gestures for main tasks that have subtasks!
                        val isParentWithSubtasks = viewItem is TaskViewItem.Persistent && viewItem.task.subtasks.isNotEmpty()
                        if (isParentWithSubtasks) return@rememberSwipeToDismissBoxState false

                        when (value) {
                            SwipeToDismissBoxValue.EndToStart -> {
                                when (viewItem) {
                                    is TaskViewItem.Persistent -> handlePersistentGesture(leftAction, viewItem.task, onTogglePersistentCompletion, onPersistentTaskDelete, onEditPersistentRequest, onPersistentTaskUpdate)
                                    is TaskViewItem.Calendar -> handleCalendarGesture(leftAction, viewItem.task, onToggleCalendarCompletion, onCalendarTaskDelete, onEditCalendarRequest, onCalendarTaskUpdate)
                                }
                                val shouldDismiss = leftAction == "delete"
                                shouldDismiss
                            }
                            SwipeToDismissBoxValue.StartToEnd -> {
                                when (viewItem) {
                                    is TaskViewItem.Persistent -> handlePersistentGesture(rightAction, viewItem.task, onTogglePersistentCompletion, onPersistentTaskDelete, onEditPersistentRequest, onPersistentTaskUpdate)
                                    is TaskViewItem.Calendar -> handleCalendarGesture(rightAction, viewItem.task, onToggleCalendarCompletion, onCalendarTaskDelete, onEditCalendarRequest, onCalendarTaskUpdate)
                                }
                                false
                            }
                            else -> false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = swipeState,
                    backgroundContent = {
                        val direction = swipeState.dismissDirection
                        val isParentWithSubtasks = viewItem is TaskViewItem.Persistent && viewItem.task.subtasks.isNotEmpty()
                        if (isParentWithSubtasks) return@SwipeToDismissBox

                        val action = if (direction == SwipeToDismissBoxValue.StartToEnd) rightAction else if (direction == SwipeToDismissBoxValue.EndToStart) leftAction else null
                        val (color, icon) = getActionUi(action)
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(color), contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) { Icon(icon, null, tint = Theme.BgDeep, modifier = Modifier.padding(horizontal = 20.dp)) }
                    },
                    content = {
                        TaskRow(
                            title = viewItem.title,
                            category = viewItem.category,
                            isCompleted = isCompleted,
                            onToggle = { 
                                when (viewItem) {
                                    is TaskViewItem.Persistent -> onTogglePersistentCompletion(viewItem.task.id, !isCompleted)
                                    is TaskViewItem.Calendar -> onToggleCalendarCompletion(viewItem.task.id, !isCompleted)
                                }
                            },
                            onClick = {
                                when (viewItem) {
                                    is TaskViewItem.Persistent -> onPersistentTaskClick(viewItem.task)
                                    is TaskViewItem.Calendar -> onCalendarTaskClick(viewItem.task)
                                }
                            },
                            onLongClick = {
                                when (viewItem) {
                                    is TaskViewItem.Persistent -> onEditPersistentRequest(viewItem.task)
                                    is TaskViewItem.Calendar -> onEditCalendarRequest(viewItem.task)
                                }
                            },
                            dueDate = (viewItem as? TaskViewItem.Persistent)?.task?.dueDate,
                            estimatedMinutes = (viewItem as? TaskViewItem.Persistent)?.task?.estimatedEffort?.toMinutes() ?: 0L,
                            timeLabel = (viewItem as? TaskViewItem.Calendar)?.task?.time ?: "",
                            subtasks = (viewItem as? TaskViewItem.Persistent)?.task?.subtasks ?: emptyList(),
                            onToggleSubtask = { subId, done ->
                                if (viewItem is TaskViewItem.Persistent) {
                                    onToggleSubtaskCompletion(viewItem.task.id, subId, done)
                                }
                            },
                            onSubtaskClick = { sub ->
                                if (viewItem is TaskViewItem.Persistent) {
                                    onSubtaskClick(viewItem.task, sub)
                                }
                            },
                            onSubtaskDelete = { subId ->
                                if (viewItem is TaskViewItem.Persistent) {
                                    onSubtaskDelete(viewItem.task.id, subId)
                                }
                            },
                            onSubtaskLongClick = { sub ->
                                if (viewItem is TaskViewItem.Persistent) {
                                    onEditSubtaskRequest(viewItem.task, sub)
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                )
            }
        }
    }
}

private sealed class TaskViewItem {
    abstract val uniqueId: String
    abstract val title: String
    abstract val category: TaskCategory
    abstract val isCompleted: Boolean

    data class Persistent(val task: com.algo1127.mytask.ui.models.Task) : TaskViewItem() {
        override val uniqueId = "pers_${task.id}"
        override val title = task.title
        override val category = task.category
        override val isCompleted = task.progress >= 1.0f
    }

    data class Calendar(val task: TaskItem, val completed: Boolean) : TaskViewItem() {
        override val uniqueId = "cal_${task.id}"
        override val title = task.title
        override val category = task.category
        override val isCompleted = completed
    }
}

private fun handlePersistentGesture(
    action: String,
    task: com.algo1127.mytask.ui.models.Task,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (com.algo1127.mytask.ui.models.Task) -> Unit,
    onUpdate: (com.algo1127.mytask.ui.models.Task) -> Unit
) {
    when (action) {
        "complete" -> onToggle(task.id, task.progress < 1.0f)
        "delete" -> onDelete(task.id)
        "edit" -> onEdit(task)
        "postpone" -> onUpdate(task.copy(dueDate = task.dueDate?.plusDays(1) ?: LocalDate.now().plusDays(1)))
    }
}

private fun handleCalendarGesture(
    action: String,
    task: TaskItem,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (TaskItem) -> Unit,
    onUpdate: (TaskItem) -> Unit
) {
    when (action) {
        "complete" -> onToggle(task.id, !task.done)
        "delete" -> onDelete(task.id)
        "edit" -> onEdit(task)
        "postpone" -> onUpdate(task.copy(date = task.date.plusDays(1)))
    }
}



@Composable
private fun getActionUi(action: String?): Pair<Color, androidx.compose.ui.graphics.vector.ImageVector> {
    return when (action) {
        "complete" -> Theme.PastelGreen to Icons.Default.Check
        "delete" -> Color.Red to Icons.Default.Delete
        "edit" -> Theme.Blue to Icons.Default.Edit
        "postpone" -> Theme.Gold to Icons.Default.Schedule
        else -> Color.Transparent to Icons.Default.QuestionMark
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    title: String,
    category: TaskCategory,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    dueDate: LocalDate? = null,
    estimatedMinutes: Long = 0,
    timeLabel: String = "",
    subtasks: List<com.algo1127.mytask.ui.models.Subtask> = emptyList(),
    onToggleSubtask: (Long, Boolean) -> Unit = { _, _ -> },
    onSubtaskClick: (com.algo1127.mytask.ui.models.Subtask) -> Unit = {},
    onSubtaskDelete: (Long) -> Unit = {},
    onSubtaskLongClick: (com.algo1127.mytask.ui.models.Subtask) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isTapped by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(isTapped) { if (isTapped) { delay(150); isTapped = false } }

    val scale by animateFloatAsState(targetValue = if (isTapped) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "taskScale")

    Surface(
        modifier = modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isCompleted) Brush.linearGradient(listOf(Theme.Purple.copy(alpha = 0.12f), Theme.Purple.copy(alpha = 0.06f))) else Brush.linearGradient(listOf(Theme.CardBg, Theme.CardBg)))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { isTapped = true; onClick() }, onLongClick = onLongClick)
            ) {
                Box(modifier = Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)).background(if (isCompleted) Theme.Purple else category.color.copy(alpha = 0.8f)).align(Alignment.CenterStart))
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(if (isCompleted) Theme.Purple.copy(alpha = 0.2f) else category.color.copy(alpha = 0.12f)).clickable { onToggle() }) {
                        AnimatedContent(isCompleted, label = "iconAnim") { completed ->
                            Icon(imageVector = if (completed) Icons.Default.CheckCircle else category.icon, contentDescription = null, tint = if (completed) Theme.Purple else category.color, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = if (isCompleted) Theme.White30 else Theme.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(category.color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(category.label, color = category.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (dueDate != null) {
                                Icon(Icons.Default.CalendarToday, null, tint = Theme.White30, modifier = Modifier.size(12.dp))
                                Text(dueDate.toString(), color = Theme.White60, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            } else if (timeLabel.isNotEmpty()) {
                                Icon(Icons.Default.Schedule, null, tint = Theme.White30, modifier = Modifier.size(12.dp))
                                Text(timeLabel, color = Theme.White60, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            } else {
                                Text("No due date", color = Theme.White30, fontSize = 11.sp)
                            }
                            if (estimatedMinutes > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Timer, null, tint = Theme.White30, modifier = Modifier.size(12.dp))
                                Text("${estimatedMinutes}m", color = Theme.White60, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    if (subtasks.isNotEmpty()) {
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Theme.White30
                            )
                        }
                    }

                    if (isCompleted) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).clip(CircleShape).background(Theme.Purple)) { Icon(Icons.Default.Check, null, tint = Theme.BgDeep, modifier = Modifier.size(16.dp)) }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded && subtasks.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subtasks.forEach { sub ->
                        val subSwipeState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        onSubtaskDelete(sub.id)
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        onToggleSubtask(sub.id, !sub.isCompleted)
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
                                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(color), contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) { Icon(icon, null, tint = Theme.BgDeep, modifier = Modifier.padding(horizontal = 12.dp)) }
                            },
                            content = {
                                SubtaskRow(
                                    subtask = sub,
                                    onToggle = { onToggleSubtask(sub.id, !sub.isCompleted) },
                                    onClick = { onSubtaskClick(sub) },
                                    onLongClick = { onSubtaskLongClick(sub) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
