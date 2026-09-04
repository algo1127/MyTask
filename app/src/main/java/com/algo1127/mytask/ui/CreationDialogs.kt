package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.ui.dialogs.AddCountdownDialog
import com.algo1127.mytask.ui.dialogs.AddEventDialog
import com.algo1127.mytask.ui.dialogs.AddTaskDialog
import com.algo1127.mytask.ui.dialogs.RepetitionInfo
import com.algo1127.mytask.ui.models.TimePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@Composable
fun CreationMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isVisible: Boolean,
    onSelect: (String) -> Unit,
    haptic: () -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 20.dp, bottom = (20.dp + bottomInset)),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Sub-buttons
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 74.dp)
        ) {
            val menuItems = listOf(
                Triple(Icons.Outlined.Schedule, Theme.Teal, "Reminder"),
                Triple(Icons.Default.Task, Theme.Purple, "Task"),
                Triple(Icons.Outlined.Event, Theme.Blue, "Event"),
                Triple(Icons.Default.HourglassEmpty, Theme.Gold, "Countdown")
            )

            menuItems.forEachIndexed { index, (icon, color, label) ->
                AnimatedVisibility(
                    visible = isExpanded && isVisible,
                    enter = fadeIn(tween(150, delayMillis = index * 50)) +
                            slideInVertically(spring(Spring.DampingRatioLowBouncy)) { it / 2 } +
                            scaleIn(spring(Spring.DampingRatioMediumBouncy), initialScale = 0.5f),
                    exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.8f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            haptic()
                            onSelect(label)
                        }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Theme.CardBg,
                            shadowElevation = 6.dp,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = label,
                                color = color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(color)
                        ) {
                            Icon(icon, null, tint = Theme.BgDeep, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        // Main FAB
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(200)),
            exit = scaleOut(spring()) + fadeOut(tween(150))
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 45f else 0f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                label = "fabRotation"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(62.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Theme.Teal.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Theme.Teal, Color(0xFF00C9A7)),
                            start = Offset(0f, 0f), end = Offset(100f, 100f)
                        )
                    )
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        haptic()
                        onToggle()
                    }
            ) {
                Icon(
                    Icons.Default.Add,
                    "Add",
                    tint = Theme.BgDeep,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

@Composable
fun CreationDialogs(
    selectedDate: LocalDate,
    showAddTaskDialog: Boolean,
    addTaskSource: Int,
    taskToEdit: TaskItem?,
    onDismissAddTask: () -> Unit,
    onUpdateTask: (TaskItem) -> Unit,
    viewModel: DashboardViewModel,
    coroutineScope: CoroutineScope,
    showAddEventDialog: Boolean,
    onDismissAddEvent: () -> Unit,
    showAddCountdownDialog: Boolean,
    onDismissAddCountdown: () -> Unit,
    showSettingsDialog: Boolean,
    onDismissSettings: () -> Unit,
    onOpenAiKnowledge: () -> Unit,
    onOpenCategoryManager: () -> Unit
) {
    val context = LocalContext.current
    val notifAi = (context.applicationContext as MyTaskApplication).notifAi

    if (showAddTaskDialog) {
        val categories by viewModel.categories.collectAsState()
        AddTaskDialog(
            defaultDate = selectedDate,
            sourceTab = addTaskSource,
            taskToEdit = taskToEdit,
            categories = categories,
            onDismiss = onDismissAddTask,
            onEdit = { updatedTask ->
                onUpdateTask(updatedTask)
                onDismissAddTask()
            },
            onAdd = { title, timePref, category, date, description, repetition ->
                val timeString = when (timePref) {
                    is TimePreference.Fixed -> String.format(Locale.US, "%02d:%02d", timePref.time.hour, timePref.time.minute)
                    TimePreference.LaterToday -> {
                        val suggest = LocalTime.now().plusHours(2)
                        String.format(Locale.US, "%02d:%02d", suggest.hour, suggest.minute)
                    }
                    TimePreference.Tomorrow -> "09:00"
                    TimePreference.AiDecide -> "10:00"
                    is TimePreference.Window -> String.format(Locale.US, "%02d:00", timePref.startHour)
                }

                val task = TaskItem(
                    title = title, 
                    time = timeString, 
                    category = category, 
                    date = date, 
                    isReminder = (addTaskSource == 0),
                    notes = description ?: ""
                )
                val rrule = repetition?.let { CalendarUtils.generateRRule(it.frequency, it.untilDate, it.interval, it.selectedDays) }
                
                val id = CalendarUtils.addTaskToCalendar(context, task, rrule)
                if (id != null) {
                    android.widget.Toast.makeText(context, "Added!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed: Check Calendar Sync", android.widget.Toast.LENGTH_LONG).show()
                }

                try { notifAi.onTaskCreated(task) } catch (e: Exception) {
                    android.util.Log.e("CreationDialogs", "onTaskCreated error: ${e.message}", e)
                }
                
                onDismissAddTask()
                coroutineScope.launch {
                    kotlinx.coroutines.delay(500)
                    viewModel.refresh()
                }
            }
        )
    }

    if (showAddEventDialog) {
        AddEventDialog(
            defaultDate = selectedDate,
            onDismiss = onDismissAddEvent,
            onAdd = { title, date, startTime, endTime, location, notes, repetition ->
                val event = EventItem(title = title, date = date, startTime = startTime, endTime = endTime, location = location, notes = notes)
                val rrule = repetition?.let { CalendarUtils.generateRRule(it.frequency, it.untilDate, it.interval, it.selectedDays) }
                
                val id = CalendarUtils.addEventToCalendar(context, event, rrule)
                if (id != null) {
                    android.widget.Toast.makeText(context, "Event added!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to add event", android.widget.Toast.LENGTH_LONG).show()
                }

                onDismissAddEvent()
                coroutineScope.launch {
                    kotlinx.coroutines.delay(500)
                    viewModel.refresh()
                }
            }
        )
    }

    if (showSettingsDialog) {
        com.algo1127.mytask.ui.dialogs.SettingsDialog(
            onDismiss = onDismissSettings,
            onOpenAiKnowledge = onOpenAiKnowledge,
            onOpenCategoryManager = onOpenCategoryManager
        )
    }

    if (showAddCountdownDialog) {
        val uiState by viewModel.uiState.collectAsState()
        AddCountdownDialog(
            tasks = uiState.tasks,
            events = uiState.events,
            notifAi = notifAi,
            onDismiss = onDismissAddCountdown,
            onAdd = { title, target, color, linkedId, linkedType, optTitle ->
                viewModel.addCountdown(
                    title = title,
                    target = target,
                    color = color.toArgb(),
                    linkedId = linkedId,
                    linkedType = linkedType,
                    optionalTitle = optTitle
                )
                android.widget.Toast.makeText(context, "Countdown started!", android.widget.Toast.LENGTH_SHORT).show()
                onDismissAddCountdown()
            }
        )
    }
}
