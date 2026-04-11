package com.algo1127.mytask.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.ui.dialogs.AddEventDialog
import com.algo1127.mytask.ui.dialogs.AddTaskDialog
import com.algo1127.mytask.ui.models.TimePreference
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.TextStyle
import java.util.*

// ==================== SHIMMER LOADING ====================
@Composable
private fun ShimmerBlock(modifier: Modifier = Modifier, height: Float = 80f) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -600f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Theme.White06, Theme.White10, Theme.White06, Theme.White10, Theme.White06),
                    start = Offset(shimmerX, 0f),
                    end = Offset(shimmerX + 500f, height)
                )
            )
    )
}

@Composable
private fun ShimmerDashboard() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShimmerBlock(height = 100f) // progress card
        ShimmerBlock(height = 72f)  // calendar row
        ShimmerBlock(height = 48f)  // tabs
        ShimmerBlock(height = 80f)  // item 1
        ShimmerBlock(height = 80f)  // item 2
        ShimmerBlock(height = 80f)  // item 3
    }
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

    // ✅ FIX: refreshKey triggers produceState to re-fetch after adding tasks/events
    var refreshKey by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val calendarData by produceState(
        initialValue = Pair<List<TaskItem>, List<EventItem>>(emptyList(), emptyList()),
        key1 = selectedDate,
        key2 = refreshKey
    ) {
        isLoading = true
        value = calendarReader.getItemsForDate(selectedDate)
        isLoading = false
    }
    val calendarTasks = calendarData.first
    val calendarEvents = calendarData.second

    val completedTasks = remember { mutableStateListOf<Long>() }
    LaunchedEffect(selectedDate) { completedTasks.clear() }

    // Dialog state
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var addTaskSource by remember { mutableStateOf(1) } // 0 = Reminders, 1 = Tasks
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Calendar view toggle
    var calendarIsGrid by remember { mutableStateOf(false) }
    // Grid month navigation
    var gridMonthOffset by remember { mutableStateOf(0L) } // months from today's month

    // Infinite row scroll state: index infiniteOffset = today
    val infiniteOffset = 10_000
    val calendarRowState = rememberLazyListState(initialFirstVisibleItemIndex = infiniteOffset)

    // ✅ Derive visible month/year from row scroll position
    val visibleRowDate by remember {
        derivedStateOf {
            val idx = calendarRowState.firstVisibleItemIndex
            today.plusDays((idx - infiniteOffset).toLong())
        }
    }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    val notifAi = (LocalContext.current.applicationContext as MyTaskApplication).notifAi

    val totalTasks = calendarTasks.size
    val completedCount = completedTasks.size
    val progress = if (totalTasks > 0) completedCount.toFloat() / totalTasks else 0f
    val fabVisible = !showAddTaskDialog && !showAddEventDialog

    // Staggered entrance trigger
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Theme.BgDeep, Theme.BgMid, Theme.BgSurface),
                    startY = 0f, endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Ambient glow orbs
        Box(
            modifier = Modifier
                .size(320.dp).offset(x = (-60).dp, y = (-40).dp)
                .background(Brush.radialGradient(colors = listOf(Theme.Teal.copy(alpha = 0.07f), Color.Transparent)), CircleShape)
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .size(260.dp).align(Alignment.BottomEnd).offset(x = 60.dp, y = 60.dp)
                .background(Brush.radialGradient(colors = listOf(Theme.Blue.copy(alpha = 0.07f), Color.Transparent)), CircleShape)
                .blur(80.dp)
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 16.dp)) {

            // ==================== HEADER ====================
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { -60 }
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(46.dp).wrapContentWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(colors = listOf(Theme.Teal, Color(0xFF00C9A7)), start = Offset(0f, 0f), end = Offset(80f, 80f)))
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Theme.BgDeep, modifier = Modifier.size(20.dp))
                            Text("MyTask", color = Theme.BgDeep, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(getGreeting(), color = Theme.White60, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = Theme.White80, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp).clip(RoundedCornerShape(12.dp)).background(Theme.White06)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSettingsDialog = true }
                    ) {
                        Icon(Icons.Default.Settings, "Settings", tint = Theme.White60, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ==================== PROGRESS CARD or SHIMMER ====================
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = 80)) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { 50 }
                )
            ) {
                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "loadingAnim"
                ) { loading ->
                    if (loading) {
                        ShimmerBlock(height = 100f)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(colors = listOf(Theme.TealDim.copy(alpha = 0.55f), Theme.BgSurface.copy(alpha = 0.90f)), start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                                .padding(1.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp)).background(Theme.CardBg).padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(72.dp), strokeWidth = 6.dp, color = Theme.White10, trackColor = Color.Transparent)
                                        val animProgress by animateFloatAsState(
                                            targetValue = progress,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                            label = "progressAnim"
                                        )
                                        CircularProgressIndicator(progress = { animProgress }, modifier = Modifier.size(72.dp), strokeWidth = 6.dp, color = Theme.Teal, trackColor = Color.Transparent)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${(progress * 100).toInt()}%", color = Theme.Teal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("done", color = Theme.White60, fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Today's Progress", color = Theme.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Text(if (totalTasks == 0) "No tasks scheduled" else "$completedCount of $totalTasks tasks done", color = Theme.White60, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                                            val segments = if (totalTasks > 0) totalTasks else 5
                                            repeat(segments) { i ->
                                                val segColor by animateColorAsState(
                                                    targetValue = if (i < completedCount) Theme.Teal else Theme.White10,
                                                    animationSpec = tween(400, delayMillis = i * 60), label = "seg_$i"
                                                )
                                                Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(segColor))
                                            }
                                        }
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
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = 160)) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { 50 }
                )
            ) {
                Column {
                    // Calendar header row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ✅ In row mode: show the month/year of the currently visible scroll position
                        // In grid mode: show the navigable grid month
                        AnimatedContent(
                            targetState = if (calendarIsGrid)
                                today.plusMonths(gridMonthOffset).let { "${it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${it.year}" }
                            else
                                "${visibleRowDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleRowDate.year}",
                            transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
                            label = "monthYearAnim"
                        ) { label ->
                            Text(label, color = Theme.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // ✅ Grid mode: prev/next month arrows
                            if (calendarIsGrid) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Theme.White06)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { gridMonthOffset-- }
                                ) { Icon(Icons.Default.ChevronLeft, null, tint = Theme.White60, modifier = Modifier.size(18.dp)) }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Theme.White06)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { gridMonthOffset++ }
                                ) { Icon(Icons.Default.ChevronRight, null, tint = Theme.White60, modifier = Modifier.size(18.dp)) }
                            } else {
                                // ✅ Row mode: "Today" jump button
                                AnimatedVisibility(
                                    visible = visibleRowDate.month != today.month || visibleRowDate.year != today.year,
                                    enter = fadeIn(tween(200)) + scaleIn(spring(Spring.DampingRatioMediumBouncy)),
                                    exit = fadeOut(tween(150)) + scaleOut(tween(150))
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .height(26.dp).wrapContentWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Theme.Teal.copy(alpha = 0.18f))
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                coroutineScope.launch { calendarRowState.animateScrollToItem(infiniteOffset) }
                                                selectedEpochDay = today.toEpochDay()
                                            }
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        Text("Today", color = Theme.Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            // Grid/Row toggle
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp).clip(RoundedCornerShape(10.dp)).background(Theme.White06)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        calendarIsGrid = !calendarIsGrid
                                        if (!calendarIsGrid) gridMonthOffset = 0L
                                    }
                            ) {
                                AnimatedContent(
                                    targetState = calendarIsGrid,
                                    transitionSpec = { scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(150)) togetherWith scaleOut(tween(100)) + fadeOut(tween(100)) },
                                    label = "calToggleIcon"
                                ) { isGrid ->
                                    Icon(if (isGrid) Icons.Outlined.ViewStream else Icons.Outlined.GridView, "Toggle view", tint = Theme.White60, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Calendar body
                    AnimatedContent(
                        targetState = calendarIsGrid,
                        transitionSpec = {
                            (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.95f)) togetherWith
                                    (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f))
                        },
                        label = "calViewToggle"
                    ) { isGrid ->
                        if (isGrid) {
                            // ---- GRID: navigable month ----
                            val gridBase = today.withDayOfMonth(1).plusMonths(gridMonthOffset)
                            val daysInMonth = gridBase.lengthOfMonth()
                            val startPadding = gridBase.dayOfWeek.value - 1

                            Column {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    listOf("Mo","Tu","We","Th","Fr","Sa","Su").forEach { d ->
                                        Text(d, color = Theme.White30, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(7),
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    userScrollEnabled = false,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(startPadding) { Box(modifier = Modifier.size(36.dp)) }
                                    items(daysInMonth) { i ->
                                        val day = gridBase.plusDays(i.toLong())
                                        val isToday = day == today
                                        val isSelected = day.toEpochDay() == selectedEpochDay
                                        val dayHasTasks = calendarTasks.any { it.date == day }
                                        val dayHasEvents = calendarEvents.any { it.date == day }
                                        val bgColor by animateColorAsState(
                                            targetValue = when { isSelected -> Theme.Teal; isToday -> Theme.White10; else -> Color.Transparent },
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                            label = "gridDay_$i"
                                        )
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp).clip(CircleShape).background(bgColor)
                                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                    selectedEpochDay = day.toEpochDay()
                                                }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(day.dayOfMonth.toString(), color = if (isSelected) Theme.BgDeep else Theme.White, fontSize = 13.sp, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
                                                if (dayHasTasks || dayHasEvents) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        if (dayHasTasks) Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(if (isSelected) Theme.BgDeep.copy(alpha = 0.6f) else Theme.Gold))
                                                        if (dayHasEvents) Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(if (isSelected) Theme.BgDeep.copy(alpha = 0.6f) else Theme.Blue))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // ---- ROW: infinite horizontal ----
                            LazyRow(
                                state = calendarRowState,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(count = 20_000) { index ->
                                    val day = today.plusDays((index - infiniteOffset).toLong())
                                    val isToday = day == today
                                    val isSelected = day.toEpochDay() == selectedEpochDay
                                    val weekday = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2).uppercase()
                                    val dayHasTasks = calendarTasks.any { it.date == day }
                                    val dayHasEvents = calendarEvents.any { it.date == day }
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.08f else 1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                        label = "calDay_$index"
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(52.dp).scale(scale)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(when {
                                                isSelected -> Brush.verticalGradient(colors = listOf(Theme.Teal, Color(0xFF00C9A7)))
                                                isToday -> Brush.verticalGradient(colors = listOf(Theme.White10, Theme.White06))
                                                else -> Brush.verticalGradient(colors = listOf(Theme.White06, Theme.White03))
                                            })
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                selectedEpochDay = day.toEpochDay()
                                            }
                                            .padding(vertical = 10.dp)
                                    ) {
                                        Text(weekday, color = if (isSelected) Theme.BgDeep.copy(alpha = 0.7f) else Theme.White30, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(day.dayOfMonth.toString(), color = if (isSelected) Theme.BgDeep else Theme.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(5.dp)) {
                                            if (dayHasTasks) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Theme.BgDeep.copy(alpha = 0.5f) else Theme.Gold))
                                            if (dayHasEvents) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Theme.BgDeep.copy(alpha = 0.5f) else Theme.Blue))
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
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = 240)) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { 50 }
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.White06).padding(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple(0, Icons.Outlined.Schedule, "Reminders"),
                            Triple(1, Icons.Default.Task, "Tasks"),
                            Triple(2, Icons.Outlined.Event, "Events")
                        ).forEach { (page, icon, label) ->
                            val selected = pagerState.currentPage == page
                            val badgeCount = when(page) { 0 -> calendarTasks.size; 1 -> 0; else -> calendarEvents.size }
                            val badgeColor = when(page) { 0 -> Theme.Teal; 1 -> Theme.Purple; else -> Theme.Blue }
                            val tabScale by animateFloatAsState(
                                targetValue = if (selected) 1.03f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "tabScale_$page"
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f).scale(tabScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Brush.linearGradient(colors = when(page) {
                                        0 -> listOf(Theme.TealDim, Color(0xFF0D2E2A))
                                        1 -> listOf(Theme.Purple.copy(alpha = 0.15f), Theme.Purple.copy(alpha = 0.05f))
                                        else -> listOf(Theme.BlueDim, Color(0xFF0A2330))
                                    }) else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent)))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        coroutineScope.launch { pagerState.animateScrollToPage(page) }
                                    }
                                    .padding(vertical = 11.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(icon, label, tint = if (selected) badgeColor else Theme.White30, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(label, color = if (selected) Theme.White else Theme.White30, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    if (badgeCount > 0) {
                                        Spacer(modifier = Modifier.width(7.dp))
                                        val badgeScale by animateFloatAsState(
                                            targetValue = if (selected) 1.15f else 1f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                            label = "badge_$page"
                                        )
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp).scale(badgeScale).clip(CircleShape).background(badgeColor)) {
                                            Text("$badgeCount", color = Theme.BgDeep, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== PAGER ====================
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "pageLoadAnim_$page"
                ) { loading ->
                    if (loading) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(3) { ShimmerBlock(height = 80f) }
                        }
                    } else {
                        when (page) {
                            0 -> RemindersTab(
                                tasks = calendarTasks, selectedDate = selectedDate, notifAi = notifAi,
                                completedTasks = completedTasks,
                                onTaskCompleted = { taskId -> if (!completedTasks.contains(taskId)) completedTasks.add(taskId) }
                            )
                            1 -> TasksTab(
                                tasks = calendarTasks,
                                selectedDate = selectedDate,
                                completedTasks = completedTasks,
                                onTaskCompleted = { taskId -> if (!completedTasks.contains(taskId)) completedTasks.add(taskId) }
                            )
                            2 -> EventsTab(events = calendarEvents, selectedDate = selectedDate)
                            else -> Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        // ==================== FAB ====================
        AnimatedVisibility(
            visible = fabVisible,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(200)),
            exit = scaleOut(spring()) + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            // ✅ FIX: read activePage here so the click lambda captures the correct tab
            val fabPage = pagerState.currentPage
            val fabColor = when(fabPage) { 0 -> Theme.Teal; 1 -> Theme.Purple; else -> Theme.Blue }
            val fabIcon = when(fabPage) { 0 -> Icons.Default.Add; 1 -> Icons.Default.Task; else -> Icons.Default.CalendarToday }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp), ambientColor = fabColor.copy(alpha = 0.35f), spotColor = fabColor.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(colors = listOf(fabColor, fabColor.copy(alpha = 0.8f)), start = Offset(0f, 0f), end = Offset(80f, 80f)))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        // ✅ FIX: use fabPage (captured in this scope) not activePage from outer scope
                        when (pagerState.currentPage) {
                            0 -> { addTaskSource = 0; showAddTaskDialog = true }  // Reminders
                            1 -> { addTaskSource = 1; showAddTaskDialog = true }  // Tasks
                            else -> showAddEventDialog = true
                        }
                    }
            ) {
                AnimatedContent(
                    targetState = fabIcon,
                    transitionSpec = { (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(150))) togetherWith (scaleOut(tween(100)) + fadeOut(tween(100))) },
                    label = "fabIconAnim"
                ) { icon ->
                    Icon(icon, "Add", tint = Theme.BgDeep, modifier = Modifier.size(28.dp))
                }
            }
        }

        // ==================== DIALOGS ====================
        if (showAddTaskDialog) {
            AddTaskDialog(
                defaultDate = selectedDate,
                sourceTab = addTaskSource,
                onDismiss = { showAddTaskDialog = false },
                onAdd = { title, timePreference, category, date, description ->  // description is still received from dialog
                    val timeString = when (timePreference) {
                        is TimePreference.Fixed -> timePreference.time.toString()
                        TimePreference.LaterToday -> "Later today"
                        TimePreference.Tomorrow -> "Tomorrow"
                        TimePreference.AiDecide -> "AI decides"
                        is TimePreference.Window -> "${timePreference.startHour}:00-${timePreference.endHour}:00"
                    }

                    // ✅ Fix: Remove 'description', add explicit 'isReminder'
                    val task = TaskItem(
                        title = title,
                        time = timeString,
                        category = category,
                        date = date,
                        isReminder = (addTaskSource == 0)  // ← Critical: 0 = Reminders, 1 = Tasks
                        // id and done use defaults
                    )

                    CalendarUtils.addTaskToCalendar(context, task)
                    try { notifAi.onTaskCreated(task) } catch (e: Exception) {
                        android.util.Log.e("DashboardScreen", "onTaskCreated error: ${e.message}", e)
                    }
                    showAddTaskDialog = false
                    refreshKey++  // trigger data refresh
                }
            )
        }

        if (showAddEventDialog) {
            AddEventDialog(
                defaultDate = selectedDate,
                onDismiss = { showAddEventDialog = false },
                onAdd = { _: String, reminders: List<TaskItem> ->
                    reminders.forEach { task ->
                        try { notifAi.onTaskCreated(task) } catch (e: Exception) {
                            android.util.Log.e("DashboardScreen", "onTaskCreated error: ${e.message}", e)
                        }
                    }
                    showAddEventDialog = false
                    refreshKey++ // ✅ trigger refresh
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(onDismiss = { showSettingsDialog = false })
        }
    }
}