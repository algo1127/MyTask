package com.algo1127.mytask.ui

import com.algo1127.mytask.ui.AddEventDialog
import com.algo1127.mytask.ui.AddTaskDialog




import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.NotifAi.NotifAi
import com.algo1127.mytask.NotifAi.model.NotificationAction
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.TextStyle
import java.util.*

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.algo1127.mytask.ui.CalendarManager

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    var selectedEpochDay by remember { mutableStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val tasks = remember {
        mutableStateListOf(
            TaskItem("Sketch UI ideas", "09:00", TaskCategory.Design, date = today),
            TaskItem("Review literature notes", "11:30", TaskCategory.Study, date = today),
            TaskItem("Buy milk & bread", "16:00", TaskCategory.Personal, date = today),
            TaskItem("Push GitHub commit", "20:15", TaskCategory.Work, date = today.plusDays(1))
        )
    }
    val events = remember {
        mutableStateListOf(
            EventItem("Team Meeting", "10:00", "11:00", "Meeting Room A", today),
            EventItem("Lunch with Client", "12:30", "13:30", "Downtown Cafe", today),
            EventItem("Project Review", "14:00", "15:00", "Online", today.plusDays(1))
        )
    }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    val week = (0..13).map { today.plusDays(it.toLong()) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val notifAi = (LocalContext.current.applicationContext as MyTaskApplication).notifAi

    val fabScale by animateFloatAsState(
        targetValue = if (showAddTaskDialog || showAddEventDialog) 0.8f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1226), Color(0xFF122033))
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(300))
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 8.dp,
                        color = Color(0xFF64FFDA).copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "logo",
                                tint = Color(0xFF64FFDA)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "MyTask",
                        color = Color(0xFF64FFDA),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { /* future: settings */ }) {
                        Surface(
                            shape = CircleShape,
                            tonalElevation = 6.dp,
                            color = Color.White.copy(alpha = 0.03f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "mini",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(600)) + slideInHorizontally(animationSpec = tween(600)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.height(140.dp)
                ) {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Text(
                            text = "Mini Calendar",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(week) { day ->
                                val isToday = day == today
                                val isSelected = day.toEpochDay() == selectedEpochDay
                                val weekday = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.1f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "calendarScale"
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(78.dp)
                                        .scale(scale)
                                        .clickable { selectedEpochDay = day.toEpochDay() }
                                ) {
                                    Text(
                                        text = weekday,
                                        color = if (isSelected) Color(0xFF0D1226) else Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = if (isSelected) RoundedCornerShape(14.dp) else RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 6.dp,
                                            bottomStart = 6.dp,
                                            bottomEnd = 18.dp
                                        ),
                                        color = when {
                                            isSelected -> Color(0xFF64FFDA)
                                            isToday -> Color.White.copy(alpha = 0.06f)
                                            else -> Color.White.copy(alpha = 0.04f)
                                        },
                                        modifier = Modifier
                                            .height(56.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = day.dayOfMonth.toString(),
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(700)),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF64FFDA),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = Color(0xFF64FFDA)
                        )
                    }
                ) {
                    listOf("Reminders", "Events").forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title, color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.6f)) }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> RemindersTab(tasks, selectedDate, notifAi)
                    1 -> EventsTab(events, selectedDate)
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (pagerState.currentPage == 0) {
                    showAddTaskDialog = true
                } else {
                    showAddEventDialog = true
                }
            },
            containerColor = Color(0xFF64FFDA),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .scale(fabScale)
        ) {
            Text("+", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        AnimatedVisibility(
            visible = showAddTaskDialog,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            AddTaskDialog(
                defaultDate = selectedDate,
                onDismiss = { showAddTaskDialog = false },
                onAdd = { title, time, category, date ->
                    val task = TaskItem(title, time, category, date)
                    tasks.add(0, task)
                    try {
                        notifAi.onTaskCreated(task)
                        android.util.Log.d("DashboardScreen", "Task created: ${task.title}")
                    } catch (e: Exception) {
                        android.util.Log.e("DashboardScreen", "Error in task creation: ${e.message}", e)
                    }
                    showAddTaskDialog = false
                }
            )
        }

        AnimatedVisibility(
            visible = showAddEventDialog,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            AddEventDialog(
                defaultDate = selectedDate,
                onDismiss = { showAddEventDialog = false },
                onAdd = { eventItems, reminders ->
                    events.addAll(0, eventItems)
                    reminders.forEach { task ->
                        tasks.add(task)
                        try {
                            notifAi.onTaskCreated(task)
                            android.util.Log.d("DashboardScreen", "Reminder created: ${task.title}")
                        } catch (e: Exception) {
                            android.util.Log.e("DashboardScreen", "Error in reminder creation: ${e.message}", e)
                        }
                    }
                    showAddEventDialog = false
                }
            )
        }
    }
}

@Composable
private fun RemindersTab(tasks: MutableList<TaskItem>, selectedDate: LocalDate, notifAi: NotifAi) {
    val visibleTasks = tasks.filter { it.date == selectedDate }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        if (visibleTasks.isEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(800)) + slideInVertically(animationSpec = tween(800))
                ) {
                    Text(
                        text = "No reminders for this day. Tap + to add one.",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            items(visibleTasks) { item ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                        animationSpec = tween(800),
                        initialOffsetY = { 50 }
                    )
                ) {
                    ReminderRow(item = item, onToggle = { toggled ->
                        val idx = tasks.indexOfFirst { it === item }
                        if (idx >= 0) {
                            tasks[idx] = tasks[idx].copy(done = !tasks[idx].done)
                            if (tasks[idx].done) {
                                try {
                                    notifAi.onTap(NotificationAction.COMPLETED, item.id)
                                    android.util.Log.d("DashboardScreen", "Task ${item.title} marked completed")
                                } catch (e: Exception) {
                                    android.util.Log.e("DashboardScreen", "Error in task completion: ${e.message}", e)
                                }
                            }
                        }
                    })
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun EventsTab(events: List<EventItem>, selectedDate: LocalDate) {
    val visibleEvents = events.filter { it.date == selectedDate }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        if (visibleEvents.isEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(800)) + slideInVertically(animationSpec = tween(800))
                ) {
                    Text(
                        text = "No events for this day.",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            items(visibleEvents) { event ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                        animationSpec = tween(800),
                        initialOffsetY = { 50 }
                    )
                ) {
                    EventRow(event = event)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ReminderRow(item: TaskItem, onToggle: (TaskItem) -> Unit) {
    var isTapped by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isTapped) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "reminderScale"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .scale(scale)
            .clickable(
                onClick = {
                    isTapped = true
                    onToggle(item)
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(200)
                        isTapped = false
                    }
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 4.dp,
                color = item.category.color.copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.category.icon,
                        contentDescription = item.title,
                        tint = item.category.color
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.category.label}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Text(
                text = item.time,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EventRow(event: EventItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 4.dp,
                color = Color(0xFF03A9F4).copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = event.title,
                        tint = Color(0xFF03A9F4)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${event.location} (${event.startTime} - ${event.endTime})",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

data class TaskItem(
    val title: String,
    val time: String,
    val category: TaskCategory,
    val date: LocalDate,
    val id: Long = System.currentTimeMillis(),
    val done: Boolean = false
) {
    fun copy(done: Boolean = this.done): TaskItem = TaskItem(title, time, category, date, id, done)
}

data class EventItem(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val date: LocalDate,
    val id: Long = System.currentTimeMillis()
)

enum class TaskCategory(val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color, val label: String) {
    Design(Icons.Default.CheckCircle, Color(0xFFFFC107), "Design"),
    Study(Icons.Default.Alarm, Color(0xFF64FFDA), "Study"),
    Personal(Icons.Default.Schedule, Color(0xFF9C27B0), "Personal"),
    Work(Icons.Default.Event, Color(0xFF03A9F4), "Work")
}
