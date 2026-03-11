package com.algo1127.mytask.ui

import com.algo1127.mytask.ui.AddEventDialog
import com.algo1127.mytask.ui.AddTaskDialog

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.NotifAi.NotifAi
import com.algo1127.mytask.NotifAi.model.NotificationAction
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    var selectedEpochDay by remember { mutableStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val context = LocalContext.current
    val calendarReader = remember { CalendarReader(context) }
    val coroutineScope = rememberCoroutineScope()

    var refreshTrigger by remember { mutableStateOf(0) }

    // ✅ FIXED: Proper destructuring with explicit Pair
    val calendarData by produceState(
        initialValue = Pair<List<TaskItem>, List<EventItem>>(emptyList(), emptyList())
    ) {
        value = calendarReader.getItemsForDate(selectedDate)
    }
    val calendarTasks = calendarData.first
    val calendarEvents = calendarData.second

    // ✅ Track completed tasks for progress
    val completedTasks = remember { mutableStateListOf<Long>() }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // 14 days for calendar scroll
    val week = (0..13).map { today.plusDays(it.toLong()) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    val notifAi = (LocalContext.current.applicationContext as MyTaskApplication).notifAi

    // ✅ FIXED: .size is a property, not a function
    val totalTasks = calendarTasks.size
    val completedCount = completedTasks.size
    val progress = if (totalTasks > 0) (completedCount.toFloat() / totalTasks) else 0f

    // FAB animation
    val fabScale by animateFloatAsState(
        targetValue = if (showAddTaskDialog || showAddEventDialog) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabScale"
    )

    Box(
        modifier = Modifier  // ✅ Use Modifier directly, not the parameter
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1226),
                        Color(0xFF122033),
                        Color(0xFF1A2F45)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ==================== HEADER ====================
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { -30 }),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 12.dp,
                        modifier = Modifier
                            .size(52.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF64FFDA), Color(0xFF00BFA5))
                                )
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "logo",
                                tint = Color(0xFF0D1226),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "MyTask",
                            color = Color(0xFF64FFDA),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = getGreeting(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Color.White.copy(alpha = 0.05f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ==================== PROGRESS CARD ====================
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + slideInHorizontally(animationSpec = tween(500), initialOffsetX = { -50 }),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(68.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = progress,
                                modifier = Modifier.size(68.dp),
                                strokeWidth = 6.dp,
                                color = Color(0xFF64FFDA),
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    color = Color(0xFF64FFDA),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Done",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Today's Progress",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$completedCount of $totalTasks tasks completed",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== MINI CALENDAR ====================
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(600)) + slideInHorizontally(animationSpec = tween(600), initialOffsetX = { 50 }),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedDate.year}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                            color = Color(0xFF64FFDA),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // ✅ FIXED: Proper items syntax with key
                        items(
                            count = week.size,
                            key = { index -> week[index].toEpochDay() }
                        ) { index ->
                            val day = week[index]
                            val isToday = day == today
                            val isSelected = day.toEpochDay() == selectedEpochDay
                            val weekday = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            val dayOfMonth = day.dayOfMonth

                            val dayHasEvents = calendarEvents.any { it.date == day }
                            val dayHasTasks = calendarTasks.any { it.date == day }

                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.08f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "calendarScale"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(60.dp)
                                    .scale(scale)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedEpochDay = day.toEpochDay() }
                            ) {
                                Text(
                                    text = weekday,
                                    color = if (isSelected) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        isSelected -> Color(0xFF64FFDA)
                                        isToday -> Color.White.copy(alpha = 0.1f)
                                        else -> Color.White.copy(alpha = 0.05f)
                                    },
                                    modifier = Modifier
                                        .height(52.dp)
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = dayOfMonth.toString(),
                                            color = if (isSelected) Color(0xFF0D1226) else Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (dayHasEvents || dayHasTasks) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (dayHasTasks) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFFFFC107),
                                                        modifier = Modifier.size(4.dp)
                                                    ) {}
                                                }
                                                if (dayHasEvents) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFF03A9F4),
                                                        modifier = Modifier.size(4.dp)
                                                    ) {}
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==================== TABS ====================
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
                            color = Color(0xFF64FFDA),
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "Reminders",
                                tint = if (pagerState.currentPage == 0) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Reminders",
                                color = if (pagerState.currentPage == 0) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (calendarTasks.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF64FFDA),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${calendarTasks.size}",
                                            color = Color(0xFF0D1226),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Event,
                                contentDescription = "Events",
                                tint = if (pagerState.currentPage == 1) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Events",
                                color = if (pagerState.currentPage == 1) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (calendarEvents.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF03A9F4),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${calendarEvents.size}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== PAGER CONTENT ====================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> RemindersTab(
                        tasks = calendarTasks,
                        selectedDate = selectedDate,
                        notifAi = notifAi,
                        completedTasks = completedTasks,
                        onTaskCompleted = { taskId ->
                            if (!completedTasks.contains(taskId)) {
                                completedTasks.add(taskId)
                            }
                        }
                    )
                    1 -> EventsTab(
                        events = calendarEvents,
                        selectedDate = selectedDate
                    )
                }
            }
        }

        // ==================== FAB ====================
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = spring()) + fadeIn(animationSpec = tween(200)),
            exit = scaleOut(animationSpec = spring()) + fadeOut(animationSpec = tween(200))
        ) {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        showAddTaskDialog = true
                    } else {
                        showAddEventDialog = true
                    }
                },
                containerColor = Color(0xFF64FFDA),
                contentColor = Color(0xFF0D1226),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .scale(fabScale)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0xFF64FFDA).copy(alpha = 0.4f)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // ==================== DIALOGS ====================
        if (showAddTaskDialog) {
            AddTaskDialog(
                defaultDate = selectedDate,
                onDismiss = { showAddTaskDialog = false },
                onAdd = { title, time, category, date ->
                    val task = TaskItem(title, time, category, date)
                    CalendarUtils.addTaskToCalendar(context, task)
                    try {
                        notifAi.onTaskCreated(task)
                    } catch (e: Exception) {
                        android.util.Log.e("DashboardScreen", "Error: ${e.message}", e)
                    }
                    showAddTaskDialog = false
                }
            )
        }

        if (showAddEventDialog) {
            AddEventDialog(
                defaultDate = selectedDate,
                onDismiss = { showAddEventDialog = false },
                onAdd = { eventItems, reminders ->
                    reminders.forEach { task ->
                        try {
                            notifAi.onTaskCreated(task)
                        } catch (e: Exception) {
                            android.util.Log.e("DashboardScreen", "Error: ${e.message}", e)
                        }
                    }
                    showAddEventDialog = false
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

// ==================== REMINDERS TAB ====================
@Composable
private fun RemindersTab(
    tasks: List<TaskItem>,
    selectedDate: LocalDate,
    notifAi: NotifAi,
    completedTasks: MutableList<Long>,
    onTaskCompleted: (Long) -> Unit
) {
    val visibleTasks = tasks.filter { it.date == selectedDate }

    if (visibleTasks.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.CheckCircle,
            title = "No Reminders",
            subtitle = "Tap + to add a new task",
            color = Color(0xFF64FFDA)
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ✅ FIXED: Proper items syntax with key
            items(
                count = visibleTasks.size,
                key = { index -> visibleTasks[index].id }
            ) { index ->
                val item = visibleTasks[index]
                val isCompleted = completedTasks.contains(item.id)

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        animationSpec = tween(400),
                        initialOffsetY = { 30 }
                    ),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
                ) {
                    ReminderRow(
                        item = item,
                        isCompleted = isCompleted,
                        onToggle = {
                            if (!isCompleted) {
                                onTaskCompleted(item.id)
                                try {
                                    notifAi.onTap(NotificationAction.COMPLETED, item.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("ReminderRow", "Error: ${e.message}", e)
                                }
                            }
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

// ==================== EVENTS TAB ====================
@Composable
private fun EventsTab(
    events: List<EventItem>,
    selectedDate: LocalDate
) {
    val visibleEvents = events.filter { it.date == selectedDate }

    if (visibleEvents.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Event,
            title = "No Events",
            subtitle = "Tap + to add a new event",
            color = Color(0xFF03A9F4)
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ✅ FIXED: Proper items syntax with key
            items(
                count = visibleEvents.size,
                key = { index -> visibleEvents[index].id }
            ) { index ->
                val event = visibleEvents[index]
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        animationSpec = tween(400),
                        initialOffsetY = { 30 }
                    )
                ) {
                    EventRow(event = event)
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

// ==================== REMINDER ROW ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(
    item: TaskItem,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    var isTapped by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isTapped) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                Color(0xFF64FFDA).copy(alpha = 0.1f)
            else
                Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isTapped = true
                onToggle()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCompleted)
                    Color(0xFF64FFDA)
                else
                    item.category.color.copy(alpha = 0.15f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = isCompleted,
                        transitionSpec = {
                            scaleIn(animationSpec = tween(200)) togetherWith
                                    scaleOut(animationSpec = tween(200))
                        }
                    ) { completed ->
                        Icon(
                            imageVector = if (completed)
                                Icons.Default.CheckCircle
                            else
                                item.category.icon,
                            contentDescription = null,
                            tint = if (completed)
                                Color(0xFF0D1226)
                            else
                                item.category.color,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = if (isCompleted)
                        Color.White.copy(alpha = 0.6f)
                    else
                        Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = item.category.color.copy(alpha = 0.2f),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = item.category.label,
                            color = item.category.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = item.time,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==================== EVENT ROW ====================
@Composable
private fun EventRow(event: EventItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF03A9F4).copy(alpha = 0.15f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = Color(0xFF03A9F4),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${event.startTime} - ${event.endTime}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
                if (event.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = event.location,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ==================== EMPTY STATE ====================
@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}

// ==================== SETTINGS DIALOG ====================
@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", color = Color.White) },
        text = {
            Column {
                Text("AI Settings", color = Color(0xFF64FFDA), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• noRoast: Disable roasts", color = Color.White.copy(alpha = 0.8f))
                Text("• soulless: Plain notifications", color = Color.White.copy(alpha = 0.8f))
                Text("• moodcast: Dynamic moods", color = Color.White.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coming soon! 🔜", color = Color.White.copy(alpha = 0.6f))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = Color(0xFF64FFDA))
            }
        },
        containerColor = Color(0xFF122033)
    )
}

// ==================== HELPER FUNCTIONS ====================
private fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour in 5..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        hour in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

// ==================== DATA CLASSES ====================
// In Dashboard.kt - Update data classes

data class TaskItem(
    val title: String,
    val time: String,
    val category: TaskCategory,
    val date: LocalDate,
    val id: Long = System.nanoTime(),  // ✅ More unique than currentTimeMillis()
    val done: Boolean = false
)

data class EventItem(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val date: LocalDate,
    val id: Long = System.nanoTime()  // ✅ More unique
)

enum class TaskCategory(
    val icon: ImageVector,
    val color: Color,
    val label: String
) {
    Design(Icons.Default.Brush, Color(0xFFFFC107), "Design"),
    Study(Icons.Default.School, Color(0xFF64FFDA), "Study"),
    Personal(Icons.Default.Favorite, Color(0xFF9C27B0), "Personal"),
    Work(Icons.Default.Business, Color(0xFF03A9F4), "Work")
}