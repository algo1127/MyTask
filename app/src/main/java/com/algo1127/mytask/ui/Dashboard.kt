package com.algo1127.mytask.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.algo1127.mytask.MyTaskApplication
import com.algo1127.mytask.ui.dialogs.AddEventDialog
import com.algo1127.mytask.ui.dialogs.AddTaskDialog
import com.algo1127.mytask.ui.dialogs.SettingsDialog
import com.algo1127.mytask.ui.dialogs.RepetitionInfo
import com.algo1127.mytask.ui.models.TimePreference
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.TextStyle
import java.util.*

// ==================== RESPONSIVE HELPERS ====================

@Composable
private fun isWideScreen(): Boolean {
    val windowInfo = currentWindowAdaptiveInfo()
    return windowInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
}

@Composable
private fun horizontalPadding(): Dp {
    return if (isWideScreen()) 32.dp else 18.dp
}

@Composable
private fun calDayWidth(): Dp {
    val config = LocalConfiguration.current
    return (config.screenWidthDp / 9f).coerceIn(44f, 72f).dp
}

// ==================== HAPTIC HELPER ====================

@Composable
private fun rememberHaptic(): () -> Unit {
    val context = LocalContext.current
    return remember {
        {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(VibratorManager::class.java)
                    vm?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v?.vibrate(30)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

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

// ==================== EMPTY STATE ====================

@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Theme.White06)
        ) {
            Icon(icon, null, tint = Theme.White30, modifier = Modifier.size(28.dp))
        }
        Text(message, color = Theme.White30, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

// ==================== MAIN SCREEN ====================

@OptIn(ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val today = LocalDate.now()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberHaptic()

    val wide = isWideScreen()
    val hPad = horizontalPadding()
    val dayWidth = calDayWidth()

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState.isLoading
    val calendarTasks = uiState.tasks
    val calendarEvents = uiState.events

    val completedIds by viewModel.completedIds.collectAsState()
    val unscheduledTasks by viewModel.unscheduledTasks.collectAsState()

    // Pull-to-refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.refresh() }
    )

    // Dialog state
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var addTaskSource by remember { mutableIntStateOf(1) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddCountdownDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<EventItem?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var isFabExpanded by remember { mutableStateOf(false) }

    var calendarIsGrid by remember { mutableStateOf(false) }
    var gridMonthOffset by remember { mutableLongStateOf(0L) }

    val infiniteOffset = 10_000
    val calendarRowState = rememberLazyListState(initialFirstVisibleItemIndex = infiniteOffset)

    val visibleRowDate by remember {
        derivedStateOf {
            val idx = calendarRowState.firstVisibleItemIndex
            today.plusDays((idx - infiniteOffset).toLong())
        }
    }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    val notifAi = (LocalContext.current.applicationContext as MyTaskApplication).notifAi

    val completedCount = completedIds.size
    val progress = if (calendarTasks.isNotEmpty()) completedCount.toFloat() / calendarTasks.size else 0f
    val fabVisible = !showAddTaskDialog && !showAddEventDialog

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Safe bottom inset for FAB
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // --- NESTED SCROLL LOGIC ---
    val density = LocalDensity.current
    
    // Dynamic height based on calendar state
    val headerHeightBaseDp = if (calendarIsGrid) 520.dp else 350.dp
    val headerHeightPx = with(density) { headerHeightBaseDp.toPx() }
    
    var headerOffsetHeightPx by remember { mutableStateOf(0f) }

    // Reset offset when toggling grid to avoid jumping
    LaunchedEffect(calendarIsGrid) {
        headerOffsetHeightPx = 0f
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffsetHeightPx + delta
                headerOffsetHeightPx = newOffset.coerceIn(-headerHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Theme.BgDeep, Theme.BgMid, Theme.BgSurface),
                    startY = 0f, endY = Float.POSITIVE_INFINITY
                )
            )
            .pullRefresh(pullRefreshState)
            .nestedScroll(nestedScrollConnection)
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

        // ==================== WIDE LAYOUT ====================
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hPad)
                    .padding(top = 20.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TaskSidebar(
                    unscheduledTasks = unscheduledTasks,
                    onAddTask = { addTaskSource = 1; showAddTaskDialog = true },
                    onDragTask = { /* Dragging logic if needed */ }
                )
                Column(modifier = Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    DashboardHeader(today = today, visible = visible, onSettingsClick = { showSettingsDialog = true })
                    ProgressCard(isLoading = isLoading, visible = visible, totalTasks = calendarTasks.size, completedCount = completedCount, progress = progress, delayMillis = 100)
                    MiniCalendar(
                        visible = visible, today = today, selectedEpochDay = selectedDate.toEpochDay(),
                        onDaySelected = { day -> haptic(); viewModel.setSelectedDate(LocalDate.ofEpochDay(day)) },
                        calendarIsGrid = calendarIsGrid, onToggleGrid = { calendarIsGrid = !calendarIsGrid; if (!calendarIsGrid) gridMonthOffset = 0L },
                        gridMonthOffset = gridMonthOffset, onGridMonthChange = { gridMonthOffset += it },
                        calendarRowState = calendarRowState, visibleRowDate = visibleRowDate,
                        calendarTasks = calendarTasks, calendarEvents = calendarEvents,
                        infiniteOffset = infiniteOffset, coroutineScope = coroutineScope, dayWidth = dayWidth,
                        delayMillis = 200
                    )
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TabBar(pagerState = pagerState, calendarTasks = calendarTasks, calendarEvents = calendarEvents, coroutineScope = coroutineScope, visible = visible, delayMillis = 300)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                        PageContent(
                            page = page,
                            isLoading = isLoading,
                            calendarTasks = calendarTasks,
                            calendarEvents = calendarEvents,
                            selectedDate = selectedDate,
                            notifAi = notifAi,
                            completedIds = completedIds,
                            onToggleCompletion = { id, done -> haptic(); viewModel.toggleCompletion(id, done) },
                            onTaskUpdate = { viewModel.updateTask(it) }
                        )
                    }
                }
            }
        } else {
            // ==================== COMPACT LAYOUT ====================
            Box(modifier = Modifier.fillMaxSize()) {
                // The scrolling header parts
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, headerOffsetHeightPx.toInt()) }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Theme.BgDeep, Theme.BgMid),
                                startY = 0f, endY = with(density) { (headerHeightBaseDp + 100.dp).toPx() }
                            )
                        )
                        .padding(horizontal = hPad)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 16.dp)
                ) {
                    DashboardHeader(today = today, visible = visible, onSettingsClick = { showSettingsDialog = true })
                    ProgressCard(isLoading = isLoading, visible = visible, totalTasks = calendarTasks.size, completedCount = completedCount, progress = progress, delayMillis = 100)
                    Spacer(modifier = Modifier.height(18.dp))
                    MiniCalendar(
                        visible = visible, today = today, selectedEpochDay = selectedDate.toEpochDay(),
                        onDaySelected = { day -> haptic(); viewModel.setSelectedDate(LocalDate.ofEpochDay(day)) },
                        calendarIsGrid = calendarIsGrid, onToggleGrid = { calendarIsGrid = !calendarIsGrid; if (!calendarIsGrid) gridMonthOffset = 0L },
                        gridMonthOffset = gridMonthOffset, onGridMonthChange = { gridMonthOffset += it },
                        calendarRowState = calendarRowState, visibleRowDate = visibleRowDate,
                        calendarTasks = calendarTasks, calendarEvents = calendarEvents,
                        infiniteOffset = infiniteOffset, coroutineScope = coroutineScope, dayWidth = dayWidth,
                        delayMillis = 200
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // The pinned TabBar (stays pinned after header scrolls away)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            val totalHeaderExclTab = with(density) { (headerHeightBaseDp + 10.dp).toPx() } 
                            val y = (totalHeaderExclTab + headerOffsetHeightPx).coerceAtLeast(0f)
                            IntOffset(0, y.toInt())
                        }
                        .background(Theme.BgMid)
                        .padding(horizontal = hPad, vertical = 8.dp)
                ) {
                    TabBar(pagerState = pagerState, calendarTasks = calendarTasks, calendarEvents = calendarEvents, coroutineScope = coroutineScope, visible = visible, delayMillis = 300)
                }

                // The Content Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { 
                            val tabHeight = with(density) { 70.dp.toPx() }
                            val totalHeaderHeight = with(density) { (headerHeightBaseDp + 80.dp).toPx() }
                            val currentPos = (totalHeaderHeight + headerOffsetHeightPx).coerceAtLeast(tabHeight)
                            IntOffset(0, currentPos.toInt())
                        },
                    pageSpacing = 16.dp
                ) { page ->
                    PageContent(
                        page = page,
                        isLoading = isLoading,
                        calendarTasks = calendarTasks,
                        calendarEvents = calendarEvents,
                        selectedDate = selectedDate,
                        notifAi = notifAi,
                        completedIds = completedIds,
                        onToggleCompletion = { id, done -> haptic(); viewModel.toggleCompletion(id, done) },
                        onTaskUpdate = { viewModel.updateTask(it) },
                        onEditRequest = { task ->
                            taskToEdit = task
                            addTaskSource = if (task.isReminder) 0 else 1
                            showAddTaskDialog = true
                        },
                        horizontalPadding = hPad
                    )
                }
            }
        }

        PullRefreshIndicator(refreshing = isLoading, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars), backgroundColor = Theme.CardBg, contentColor = Theme.Teal)

    // Optional: Dim background when FAB expanded
    if (isFabExpanded && fabVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    isFabExpanded = false
                }
        )
    }

    // --- EXPANDING FAB MENU & DIALOGS ---
    CreationMenu(
        isExpanded = isFabExpanded,
        onToggle = { isFabExpanded = !isFabExpanded },
        isVisible = fabVisible,
        onSelect = { label ->
            isFabExpanded = false
            when (label) {
                "Reminder" -> { addTaskSource = 0; showAddTaskDialog = true }
                "Task" -> { addTaskSource = 1; showAddTaskDialog = true }
                "Event" -> { showAddEventDialog = true }
                "Countdown" -> { showAddCountdownDialog = true }
            }
        },
        haptic = haptic,
        bottomInset = bottomInset
    )

    CreationDialogs(
        selectedDate = selectedDate,
        showAddTaskDialog = showAddTaskDialog,
        addTaskSource = addTaskSource,
        taskToEdit = taskToEdit,
        onDismissAddTask = { showAddTaskDialog = false; taskToEdit = null },
        onUpdateTask = { viewModel.updateTask(it) },
        viewModel = viewModel,
        coroutineScope = coroutineScope,
        showAddEventDialog = showAddEventDialog,
        onDismissAddEvent = { showAddEventDialog = false },
        showAddCountdownDialog = showAddCountdownDialog,
        onDismissAddCountdown = { showAddCountdownDialog = false },
        showSettingsDialog = showSettingsDialog,
        onDismissSettings = { showSettingsDialog = false }
    )
    }
}

// ==================== EXTRACTED COMPOSABLES ====================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DashboardHeader(today: LocalDate, visible: Boolean, onSettingsClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible, 
        enter = fadeIn(tween(600)) + slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), 
            initialOffsetY = { -100 }
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier
                    .height(48.dp)
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(colors = listOf(Theme.Teal, Color(0xFF00C9A7))))
                    .padding(horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
                    val logoScale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "logoScale"
                    )
                    Icon(Icons.Default.CheckCircle, null, tint = Theme.BgDeep, modifier = Modifier.size(22.dp).scale(logoScale))
                    Text("MyTask", color = Theme.BgDeep, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(getGreeting(), color = Theme.White60, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = Theme.White80, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Theme.White06)
            ) {
                Icon(Icons.Default.Settings, "Settings", tint = Theme.White60, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun ProgressCard(isLoading: Boolean, visible: Boolean, totalTasks: Int, completedCount: Int, progress: Float, delayMillis: Int = 0) {
    AnimatedVisibility(
        visible = visible, 
        enter = fadeIn(tween(600, delayMillis = delayMillis)) + slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), 
            initialOffsetY = { 100 }
        )
    ) {
        AnimatedContent(
            targetState = isLoading, 
            transitionSpec = { (scaleIn(spring()) + fadeIn()) togetherWith (scaleOut(spring()) + fadeOut()) }, 
            label = "loadingAnim"
        ) { loading ->
            if (loading) { ShimmerBlock(height = 110f) } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(colors = listOf(Theme.Teal.copy(alpha = 0.15f), Theme.BgSurface)))
                        .padding(1.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(Theme.CardBg).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(80.dp), strokeWidth = 8.dp, color = Theme.White06, trackColor = Color.Transparent)
                                val animProgress by animateFloatAsState(targetValue = progress, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow), label = "progressAnim")
                                CircularProgressIndicator(progress = { animProgress }, modifier = Modifier.size(80.dp), strokeWidth = 8.dp, color = Theme.Teal, trackColor = Color.Transparent, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${(progress * 100).toInt()}%", color = Theme.Teal, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("done", color = Theme.White30, fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Progress", color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (totalTasks == 0) "No tasks for today!" else "$completedCount / $totalTasks completed", color = Theme.White60, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                    val segments = if (totalTasks > 0) totalTasks else 5
                                    repeat(segments) { i ->
                                        val isFilled = i < completedCount
                                        val segColor by animateColorAsState(targetValue = if (isFilled) Theme.Teal else Theme.White10, animationSpec = tween(500, delayMillis = i * 50), label = "seg_$i")
                                        val segScale by animateFloatAsState(targetValue = if (isFilled) 1.1f else 1.0f, animationSpec = spring(Spring.DampingRatioHighBouncy), label = "segScale_$i")
                                        Box(modifier = Modifier.weight(1f).height(6.dp).scale(segScale).clip(RoundedCornerShape(3.dp)).background(segColor))
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MiniCalendar(visible: Boolean, today: LocalDate, selectedEpochDay: Long, onDaySelected: (Long) -> Unit, calendarIsGrid: Boolean, onToggleGrid: () -> Unit, gridMonthOffset: Long, onGridMonthChange: (Long) -> Unit, calendarRowState: androidx.compose.foundation.lazy.LazyListState, visibleRowDate: LocalDate, calendarTasks: List<TaskItem>, calendarEvents: List<EventItem>, infiniteOffset: Int, coroutineScope: kotlinx.coroutines.CoroutineScope, dayWidth: Dp, delayMillis: Int) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, delayMillis = delayMillis)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 60 })) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(targetState = if (calendarIsGrid) today.withDayOfMonth(1).plusMonths(gridMonthOffset).let { "${it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${it.year}" } else "${visibleRowDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleRowDate.year}", transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) }, label = "monthYearAnim") { label -> Text(label, color = Theme.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (calendarIsGrid) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Theme.White06).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGridMonthChange(-1) }) { Icon(Icons.Default.ChevronLeft, null, tint = Theme.White60, modifier = Modifier.size(18.dp)) }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Theme.White06).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGridMonthChange(1) }) { Icon(Icons.Default.ChevronRight, null, tint = Theme.White60, modifier = Modifier.size(18.dp)) }
                    } else {
                        AnimatedVisibility(visible = visibleRowDate.month != today.month || visibleRowDate.year != today.year, enter = fadeIn(tween(200)) + scaleIn(spring(Spring.DampingRatioMediumBouncy)), exit = fadeOut(tween(150)) + scaleOut(tween(150))) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(26.dp).wrapContentWidth().clip(RoundedCornerShape(8.dp)).background(Theme.Teal.copy(alpha = 0.18f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { coroutineScope.launch { calendarRowState.animateScrollToItem(infiniteOffset) }; onDaySelected(today.toEpochDay()) }.padding(horizontal = 10.dp)) { Text("Today", color = Theme.Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Theme.White06).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleGrid() }) {
                        AnimatedContent(targetState = calendarIsGrid, transitionSpec = { scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(150)) togetherWith scaleOut(tween(100)) + fadeOut(tween(100)) }, label = "calToggleIcon") { isGrid -> Icon(if (isGrid) Icons.Outlined.ViewStream else Icons.Outlined.GridView, "Toggle view", tint = Theme.White60, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
            AnimatedContent(targetState = calendarIsGrid, transitionSpec = { (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.95f)) togetherWith (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f)) }, label = "calViewToggle") { isGrid ->
                if (isGrid) {
                    val gridBase = today.withDayOfMonth(1).plusMonths(gridMonthOffset)
                    val daysInMonth = gridBase.lengthOfMonth()
                    val startPadding = gridBase.dayOfWeek.value - 1
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) { listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { d -> Text(d, color = Theme.White30, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth().height(200.dp), userScrollEnabled = false, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(startPadding) { Box(modifier = Modifier.size(36.dp)) }
                            items(daysInMonth) { i ->
                                val day = gridBase.plusDays(i.toLong())
                                val isToday = day == today
                                val isSelected = day.toEpochDay() == selectedEpochDay
                                val dayHasTasks = calendarTasks.any { it.date == day }
                                val dayHasEvents = calendarEvents.any { it.date == day }
                                val bgColor by animateColorAsState(targetValue = when { isSelected -> Theme.Teal; isToday -> Theme.White10; else -> Color.Transparent }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gridDay_$i")
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(CircleShape).background(bgColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDaySelected(day.toEpochDay()) }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(day.dayOfMonth.toString(), color = if (isSelected) Theme.BgDeep else Theme.White, fontSize = 13.sp, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
                                        if (dayHasTasks || dayHasEvents) { Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { if (dayHasTasks) Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(if (isSelected) Theme.BgDeep.copy(alpha = 0.6f) else Theme.Gold)); if (dayHasEvents) Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(if (isSelected) Theme.BgDeep.copy(alpha = 0.6f) else Theme.Blue)) } }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(state = calendarRowState, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 2.dp)) {
                        items(count = 20_000) { index ->
                            val day = today.plusDays((index - infiniteOffset).toLong())
                            val isToday = day == today
                            val isSelected = day.toEpochDay() == selectedEpochDay
                            val weekday = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2).uppercase()
                            val dayHasTasks = calendarTasks.any { it.date == day }
                            val dayHasEvents = calendarEvents.any { it.date == day }
                            val scale by animateFloatAsState(targetValue = if (isSelected) 1.08f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "calDay_$index")
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(dayWidth).scale(scale).clip(RoundedCornerShape(14.dp)).background(when { isSelected -> Brush.verticalGradient(colors = listOf(Theme.Teal, Color(0xFF00C9A7))); isToday -> Brush.verticalGradient(colors = listOf(Theme.White10, Theme.White06)); else -> Brush.verticalGradient(colors = listOf(Theme.White06, Theme.White03)) }).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDaySelected(day.toEpochDay()) }.padding(vertical = 10.dp)) {
                                Text(weekday, color = if (isSelected) Theme.BgDeep.copy(alpha = 0.7f) else Theme.White30, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(day.dayOfMonth.toString(), color = if (isSelected) Theme.BgDeep else Theme.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(5.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(5.dp)) { if (dayHasTasks) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Theme.BgDeep.copy(alpha = 0.5f) else Theme.Gold)); if (dayHasEvents) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Theme.BgDeep.copy(alpha = 0.5f) else Theme.Blue)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBar(pagerState: androidx.compose.foundation.pager.PagerState, calendarTasks: List<TaskItem>, calendarEvents: List<EventItem>, coroutineScope: kotlinx.coroutines.CoroutineScope, visible: Boolean, delayMillis: Int) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, delayMillis = delayMillis)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 60 })) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.White06).padding(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(Triple(0, Icons.Outlined.Schedule, "Reminders"), Triple(1, Icons.Default.Task, "Tasks"), Triple(2, Icons.Outlined.Event, "Events")).forEach { (page, icon, label) ->
                    val selected = pagerState.currentPage == page
                    val badgeCount = when (page) { 0 -> calendarTasks.count { it.isReminder }; 1 -> calendarTasks.count { !it.isReminder }; else -> calendarEvents.size }
                    val badgeColor = when (page) { 0 -> Theme.Teal; 1 -> Theme.Purple; else -> Theme.Blue }
                    val tabScale by animateFloatAsState(targetValue = if (selected) 1.03f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tabScale_$page")
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).scale(tabScale).clip(RoundedCornerShape(12.dp)).background(if (selected) Brush.linearGradient(colors = when (page) { 0 -> listOf(Theme.TealDim, Color(0xFF0D2E2A)); 1 -> listOf(Theme.Purple.copy(alpha = 0.15f), Theme.Purple.copy(alpha = 0.05f)); else -> listOf(Theme.BlueDim, Color(0xFF0A2330)) }) else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { coroutineScope.launch { pagerState.animateScrollToPage(page) } }.padding(vertical = 11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(icon, label, tint = if (selected) badgeColor else Theme.White30, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(label, color = if (selected) Theme.White else Theme.White30, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            if (badgeCount > 0) {
                                Spacer(modifier = Modifier.width(7.dp))
                                val badgeScale by animateFloatAsState(targetValue = if (selected) 1.15f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "badge_$page")
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp).scale(badgeScale).clip(CircleShape).background(badgeColor)) { Text(badgeCount.toString(), color = Theme.BgDeep, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PageContent(
    page: Int,
    isLoading: Boolean,
    calendarTasks: List<TaskItem>,
    calendarEvents: List<EventItem>,
    selectedDate: LocalDate,
    notifAi: Any,
    completedIds: Set<Long>,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onTaskUpdate: (TaskItem) -> Unit,
    onEditRequest: (TaskItem) -> Unit = {},
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    AnimatedContent(
        targetState = page, 
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }.using(SizeTransform(clip = false))
        },
        label = "pageTransition"
    ) { targetPage ->
        Box(Modifier.fillMaxSize().padding(horizontal = horizontalPadding)) {
            if (isLoading && false) { 
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { 
                    repeat(3) { ShimmerBlock(height = 80f) } 
                } 
            } else {
                when (targetPage) {
                    0 -> RemindersTab(tasks = calendarTasks, selectedDate = selectedDate, notifAi = notifAi as com.algo1127.mytask.NotifAi.NotifAi, completedIds = completedIds, onToggleCompletion = onToggleCompletion, onEditRequest = onEditRequest)
                    1 -> TasksTab(tasks = calendarTasks, selectedDate = selectedDate, completedIds = completedIds, onToggleCompletion = onToggleCompletion, onTaskUpdate = onTaskUpdate, onEditRequest = onEditRequest)
                    2 -> EventsTab(events = calendarEvents, selectedDate = selectedDate, completedIds = completedIds, onToggleCompletion = onToggleCompletion)
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
