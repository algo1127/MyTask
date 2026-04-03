package com.algo1127.mytask.ui

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.TextStyle
import java.util.*

// ==================== DESIGN TOKENS ====================
private object Theme {
    val BgDeep      = Color(0xFF080E1E)
    val BgMid       = Color(0xFF0F1A2E)
    val BgSurface   = Color(0xFF162135)
    val Teal        = Color(0xFF4DFFD2)
    val TealDim     = Color(0xFF1A5C4F)
    val TealGlow    = Color(0xFF4DFFD2).copy(alpha = 0.18f)
    val Blue        = Color(0xFF3FC3F7)
    val BlueDim     = Color(0xFF0D3A50)
    val Gold        = Color(0xFFFFBD2E)
    val Purple      = Color(0xFFB57BFF)
    val White       = Color.White
    val White80     = Color.White.copy(alpha = 0.80f)
    val White60     = Color.White.copy(alpha = 0.60f)
    val White30     = Color.White.copy(alpha = 0.30f)
    val White10     = Color.White.copy(alpha = 0.10f)
    val White06     = Color.White.copy(alpha = 0.06f)
    val White03     = Color.White.copy(alpha = 0.03f)
    val CardBg      = Color(0xFF131F32)
    val CardBorder  = Color.White.copy(alpha = 0.08f)
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    var selectedEpochDay by remember { mutableStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val context = LocalContext.current
    val calendarReader = remember { CalendarReader(context) }
    val coroutineScope = rememberCoroutineScope()

    // ✅ FIX 1: key1 = selectedDate ensures produceState re-runs on date change
    val calendarData by produceState(
        initialValue = Pair<List<TaskItem>, List<EventItem>>(emptyList(), emptyList()),
        key1 = selectedDate
    ) {
        value = calendarReader.getItemsForDate(selectedDate)
    }
    val calendarTasks  = calendarData.first
    val calendarEvents = calendarData.second

    // ✅ FIX 2: Reset completedTasks when selected date changes
    val completedTasks = remember { mutableStateListOf<Long>() }
    LaunchedEffect(selectedDate) {
        completedTasks.clear()
    }

    var showAddTaskDialog  by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val week = (0..13).map { today.plusDays(it.toLong()) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    val notifAi = (LocalContext.current.applicationContext as MyTaskApplication).notifAi

    val totalTasks     = calendarTasks.size
    val completedCount = completedTasks.size
    val progress       = if (totalTasks > 0) completedCount.toFloat() / totalTasks else 0f

    // ✅ FIX 5: FAB visibility driven by AnimatedVisibility, scale removed as the gating mechanism
    val fabVisible = !showAddTaskDialog && !showAddEventDialog

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Theme.BgDeep, Theme.BgMid, Theme.BgSurface),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Ambient glow orbs for depth
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Theme.Teal.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Theme.Blue.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .blur(80.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp)
        ) {
            // ==================== HEADER ====================
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 18.dp)
                ) {
                    // Logo pill
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(46.dp)
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Theme.Teal, Color(0xFF00C9A7)),
                                    start = Offset(0f, 0f),
                                    end   = Offset(80f, 80f)
                                )
                            )
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Theme.BgDeep,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "MyTask",
                                color = Theme.BgDeep,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = getGreeting(),
                            color = Theme.White60,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            color = Theme.White80,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Theme.White06)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showSettingsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Theme.White60,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ==================== PROGRESS CARD ====================
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 20 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Theme.TealDim.copy(alpha = 0.55f),
                                    Theme.BgSurface.copy(alpha = 0.90f)
                                ),
                                start = Offset(0f, 0f),
                                end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .padding(1.dp) // border trick
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(21.dp))
                            .background(Theme.CardBg)
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Circular progress
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier       = Modifier.size(72.dp),
                                    strokeWidth    = 6.dp,
                                    color          = Theme.White10,
                                    trackColor     = Color.Transparent
                                )
                                val animProgress by animateFloatAsState(
                                    targetValue    = progress,
                                    animationSpec  = tween(800, easing = FastOutSlowInEasing),
                                    label          = "progressAnim"
                                )
                                CircularProgressIndicator(
                                    progress = { animProgress },
                                    modifier       = Modifier.size(72.dp),
                                    strokeWidth    = 6.dp,
                                    color          = Theme.Teal,
                                    trackColor     = Color.Transparent
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        color = Theme.Teal,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "done",
                                        color = Theme.White60,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Today's Progress",
                                    color = Theme.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = if (totalTasks == 0) "No tasks scheduled"
                                    else "$completedCount of $totalTasks tasks done",
                                    color = Theme.White60,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                // Segmented mini-bar
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val segments = if (totalTasks > 0) totalTasks else 5
                                    repeat(segments) { i ->
                                        val filled = i < completedCount
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    if (filled) Theme.Teal
                                                    else Theme.White10
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==================== MINI CALENDAR ====================
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 20 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            color = Theme.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedDate.year}",
                            color = Theme.White30,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            count = week.size,
                            key   = { index -> week[index].toEpochDay() }
                        ) { index ->
                            val day        = week[index]
                            val isToday    = day == today
                            val isSelected = day.toEpochDay() == selectedEpochDay
                            val weekday    = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                .take(2).uppercase()
                            val dayOfMonth = day.dayOfMonth

                            // ✅ FIX 6: Check correct day-specific lists
                            val dayHasTasks  = calendarTasks.any  { it.date == day }
                            val dayHasEvents = calendarEvents.any { it.date == day }

                            val scale by animateFloatAsState(
                                targetValue  = if (isSelected) 1.06f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label        = "calDay_$index"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(52.dp)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isSelected -> Brush.verticalGradient(
                                                colors = listOf(Theme.Teal, Color(0xFF00C9A7))
                                            )
                                            isToday    -> Brush.verticalGradient(
                                                colors = listOf(Theme.White10, Theme.White06)
                                            )
                                            else       -> Brush.verticalGradient(
                                                colors = listOf(Theme.White06, Theme.White03)
                                            )
                                        }
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null
                                    ) { selectedEpochDay = day.toEpochDay() }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = weekday,
                                    color = if (isSelected) Theme.BgDeep.copy(alpha = 0.7f)
                                    else Theme.White30,
                                    fontSize    = 10.sp,
                                    fontWeight  = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text       = dayOfMonth.toString(),
                                    color      = if (isSelected) Theme.BgDeep else Theme.White,
                                    fontSize   = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                // Indicator dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.height(5.dp)
                                ) {
                                    if (dayHasTasks) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Theme.BgDeep.copy(alpha = 0.5f)
                                                    else Theme.Gold
                                                )
                                        )
                                    }
                                    if (dayHasEvents) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Theme.BgDeep.copy(alpha = 0.5f)
                                                    else Theme.Blue
                                                )
                                        )
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
                enter = fadeIn(tween(700)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Theme.White06)
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple(0, Icons.Outlined.Schedule, "Reminders"),
                            Triple(1, Icons.Outlined.Event,    "Events")
                        ).forEach { (page, icon, label) ->
                            val selected = pagerState.currentPage == page
                            val badgeCount = if (page == 0) calendarTasks.size else calendarEvents.size
                            val badgeColor = if (page == 0) Theme.Teal else Theme.Blue

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected)
                                            Brush.linearGradient(
                                                colors = if (page == 0)
                                                    listOf(Theme.TealDim, Color(0xFF0D2E2A))
                                                else
                                                    listOf(Theme.BlueDim, Color(0xFF0A2330))
                                            )
                                        else Brush.linearGradient(
                                            colors = listOf(Color.Transparent, Color.Transparent)
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null
                                    ) { coroutineScope.launch { pagerState.animateScrollToPage(page) } }
                                    .padding(vertical = 11.dp)
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint       = if (selected) badgeColor else Theme.White30,
                                        modifier   = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text       = label,
                                        color      = if (selected) Theme.White else Theme.White30,
                                        fontSize   = 14.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (badgeCount > 0) {
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        ) {
                                            Text(
                                                text       = "$badgeCount",
                                                color      = Theme.BgDeep,
                                                fontSize   = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
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
                state    = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> RemindersTab(
                        tasks          = calendarTasks,
                        selectedDate   = selectedDate,
                        notifAi        = notifAi,
                        completedTasks = completedTasks,
                        onTaskCompleted = { taskId ->
                            if (!completedTasks.contains(taskId)) completedTasks.add(taskId)
                        }
                    )
                    1 -> EventsTab(
                        events       = calendarEvents,
                        selectedDate = selectedDate
                    )
                }
            }
        }

        // ==================== FAB ====================
        // ✅ FIX 5: AnimatedVisibility properly controls FAB, no redundant scale gating
        AnimatedVisibility(
            visible  = fabVisible,
            enter    = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(200)),
            exit     = scaleOut(spring()) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation    = 16.dp,
                        shape        = RoundedCornerShape(18.dp),
                        ambientColor = Theme.Teal.copy(alpha = 0.35f),
                        spotColor    = Theme.Teal.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Theme.Teal, Color(0xFF00C9A7)),
                            start  = Offset(0f, 0f),
                            end    = Offset(80f, 80f)
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) {
                        if (pagerState.currentPage == 0) showAddTaskDialog = true
                        else showAddEventDialog = true
                    }
            ) {
                Icon(
                    imageVector    = Icons.Default.Add,
                    contentDescription = "Add",
                    tint           = Theme.BgDeep,
                    modifier       = Modifier.size(28.dp)
                )
            }
        }

        // ==================== DIALOGS ====================
        if (showAddTaskDialog) {
            AddTaskDialog(
                defaultDate = selectedDate,
                onDismiss   = { showAddTaskDialog = false },
                onAdd       = { title, time, category, date ->
                    val task = TaskItem(title, time, category, date)
                    CalendarUtils.addTaskToCalendar(context, task)
                    try { notifAi.onTaskCreated(task) } catch (e: Exception) {
                        android.util.Log.e("DashboardScreen", "onTaskCreated error: ${e.message}", e)
                    }
                    showAddTaskDialog = false
                }
            )
        }

        if (showAddEventDialog) {
            AddEventDialog(
                defaultDate = selectedDate,
                onDismiss   = { showAddEventDialog = false },
                onAdd       = { _, reminders ->
                    reminders.forEach { task ->
                        try { notifAi.onTaskCreated(task) } catch (e: Exception) {
                            android.util.Log.e("DashboardScreen", "onTaskCreated error: ${e.message}", e)
                        }
                    }
                    showAddEventDialog = false
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(onDismiss = { showSettingsDialog = false })
        }
    }
}

// ==================== REMINDERS TAB ====================
@Composable
private fun RemindersTab(
    tasks           : List<TaskItem>,
    selectedDate    : LocalDate,
    notifAi         : NotifAi,
    completedTasks  : MutableList<Long>,
    onTaskCompleted : (Long) -> Unit
) {
    val visibleTasks = tasks.filter { it.date == selectedDate }

    if (visibleTasks.isEmpty()) {
        EmptyState(
            icon     = Icons.Outlined.CheckCircle,
            title    = "All clear",
            subtitle = "Tap + to add a reminder",
            color    = Theme.Teal
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = visibleTasks.size,
                key   = { index -> visibleTasks[index].id }
            ) { index ->
                val item        = visibleTasks[index]
                val isCompleted = completedTasks.contains(item.id)

                // ✅ FIX 7: Proper enter AND exit
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn(tween(300, delayMillis = index * 50)) +
                            slideInVertically(tween(300, delayMillis = index * 50)) { 40 },
                    exit    = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    ReminderRow(
                        item        = item,
                        isCompleted = isCompleted,
                        onToggle    = {
                            if (!isCompleted) {
                                onTaskCompleted(item.id)
                                try { notifAi.onTap(NotificationAction.COMPLETED, item.id) }
                                catch (e: Exception) {
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
    events       : List<EventItem>,
    selectedDate : LocalDate
) {
    val visibleEvents = events.filter { it.date == selectedDate }

    if (visibleEvents.isEmpty()) {
        EmptyState(
            icon     = Icons.Outlined.Event,
            title    = "No events",
            subtitle = "Tap + to add an event",
            color    = Theme.Blue
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = visibleEvents.size,
                key   = { index -> visibleEvents[index].id }
            ) { index ->
                val event = visibleEvents[index]
                // ✅ FIX 7: Proper enter AND exit
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn(tween(300, delayMillis = index * 50)) +
                            slideInVertically(tween(300, delayMillis = index * 50)) { 40 },
                    exit    = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    EventRow(event = event)
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

// ==================== REMINDER ROW ====================
@Composable
private fun ReminderRow(
    item        : TaskItem,
    isCompleted : Boolean,
    onToggle    : () -> Unit
) {
    // ✅ FIX 4: isTapped resets via LaunchedEffect so animation can retrigger
    var isTapped by remember { mutableStateOf(false) }
    LaunchedEffect(isTapped) {
        if (isTapped) {
            delay(150)
            isTapped = false
        }
    }

    val scale by animateFloatAsState(
        targetValue   = if (isTapped) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "reminderScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isCompleted)
                    Brush.linearGradient(
                        colors = listOf(
                            Theme.Teal.copy(alpha = 0.12f),
                            Theme.TealDim.copy(alpha = 0.08f)
                        )
                    )
                else Brush.linearGradient(
                    colors = listOf(Theme.CardBg, Theme.CardBg)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) {
                isTapped = true
                onToggle()
            }
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                .background(
                    if (isCompleted) Theme.Teal
                    else item.category.color.copy(alpha = 0.8f)
                )
                .align(Alignment.CenterStart)
        )
        Row(
            modifier           = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment  = Alignment.CenterVertically
        ) {
            // Icon bubble
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (isCompleted)
                            Theme.Teal.copy(alpha = 0.2f)
                        else
                            item.category.color.copy(alpha = 0.12f)
                    )
            ) {
                AnimatedContent(
                    targetState  = isCompleted,
                    transitionSpec = {
                        scaleIn(tween(200))  togetherWith scaleOut(tween(200))
                    },
                    label = "iconTransition"
                ) { completed ->
                    Icon(
                        imageVector = if (completed) Icons.Default.CheckCircle
                        else item.category.icon,
                        contentDescription = null,
                        tint       = if (completed) Theme.Teal else item.category.color,
                        modifier   = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text           = item.title,
                    color          = if (isCompleted) Theme.White30 else Theme.White,
                    fontSize       = 15.sp,
                    fontWeight     = FontWeight.SemiBold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines       = 2,
                    overflow       = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(item.category.color.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text       = item.category.label,
                            color      = item.category.color,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint       = Theme.White30,
                        modifier   = Modifier.size(12.dp)
                    )
                    Text(
                        text       = item.time,
                        color      = Theme.White60,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (isCompleted) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Theme.Teal)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint     = Theme.BgDeep,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==================== EVENT ROW ====================
@Composable
private fun EventRow(event: EventItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Theme.CardBg)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                .background(Theme.Blue.copy(alpha = 0.8f))
                .align(Alignment.CenterStart)
        )
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Theme.Blue.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint     = Theme.Blue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = event.title,
                    color      = Theme.White,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint     = Theme.White30,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text  = "${event.startTime} – ${event.endTime}",
                        color = Theme.White60,
                        fontSize = 12.sp
                    )
                }
                if (event.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint     = Theme.Blue.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text     = event.location,
                            color    = Theme.White60,
                            fontSize = 12.sp,
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
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    color    : Color
) {
    Column(
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.08f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint     = color.copy(alpha = 0.7f),
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text       = title,
            color      = Theme.White80,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text  = subtitle,
            color = Theme.White30,
            fontSize = 14.sp
        )
    }
}

// ==================== SETTINGS DIALOG ====================
@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Settings", color = Theme.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text       = "AI Personality",
                    color      = Theme.Teal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                listOf(
                    "noRoast"   to "Disable roasts",
                    "soulless"  to "Plain notifications",
                    "moodcast"  to "Dynamic moods"
                ).forEach { (flag, desc) ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Theme.Teal.copy(alpha = 0.5f))
                        )
                        Column {
                            Text(flag,  color = Theme.White80,  fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(desc,  color = Theme.White30,  fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("More settings coming soon 🔜", color = Theme.White30, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = Theme.Teal, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = Theme.BgSurface,
        shape           = RoundedCornerShape(22.dp)
    )
}

// ==================== HELPERS ====================
private fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour in 5..11  -> "Good morning ☀️"
        hour in 12..16 -> "Good afternoon"
        hour in 17..20 -> "Good evening 🌆"
        else           -> "Good night 🌙"
    }
}

// ==================== DATA CLASSES ====================
data class TaskItem(
    val title    : String,
    val time     : String,
    val category : TaskCategory,
    val date     : LocalDate,
    val id       : Long    = System.nanoTime(),
    val done     : Boolean = false
)

data class EventItem(
    val title     : String,
    val startTime : String,
    val endTime   : String,
    val location  : String,
    val date      : LocalDate,
    val id        : Long = System.nanoTime()
)

enum class TaskCategory(
    val icon  : ImageVector,
    val color : Color,
    val label : String
) {
    Design  (Icons.Default.Brush,    Color(0xFFFFBD2E), "Design"),
    Study   (Icons.Default.School,   Color(0xFF4DFFD2), "Study"),
    Personal(Icons.Default.Favorite, Color(0xFFB57BFF), "Personal"),
    Work    (Icons.Default.Business, Color(0xFF3FC3F7), "Work")
}