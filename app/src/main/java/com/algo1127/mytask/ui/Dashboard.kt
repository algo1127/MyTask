package com.algo1127.mytask.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.max

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

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
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

    val windowInfo = currentWindowAdaptiveInfo()
    val wide = remember(windowInfo) { 
        windowInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    }
    val hPad = remember(wide) { if (wide) 32.dp else 18.dp }

    val uiState by viewModel.uiState.collectAsState()
    val calendarData by viewModel.calendarData.collectAsState()
    val isLoading = uiState.isLoading
    val calendarTasks = uiState.tasks
    val calendarEvents = uiState.events
    val taskCounts = calendarData.first
    val eventCounts = calendarData.second
    val taskAgendas = calendarData.third

    val completedIds by viewModel.completedIds.collectAsState()
    val unscheduledTasks by viewModel.unscheduledTasks.collectAsState()
    val activeCountdowns by viewModel.activeCountdowns.collectAsState()

    var isShowingCountdowns by remember { mutableStateOf(false) }
    var selectedCountdown by remember { mutableStateOf<com.algo1127.mytask.ui.models.CountdownItem?>(null) }
    var selectedEvent by remember { mutableStateOf<EventItem?>(null) }
    var selectedTask by remember { mutableStateOf<TaskItem?>(null) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.refresh() }
    )

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var addTaskSource by remember { mutableIntStateOf(1) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddCountdownDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showJumpToDateDialog by remember { mutableStateOf(false) }
    var showAiKnowledge by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }

    var isFabExpanded by remember { mutableStateOf(false) }

    var calendarIsGrid by remember { mutableStateOf(false) }
    var gridMonthOffset by remember { mutableLongStateOf(0L) }

    LaunchedEffect(calendarIsGrid, selectedDate) {
        if (calendarIsGrid) {
            val monthsBetween = ChronoUnit.MONTHS.between(
                today.withDayOfMonth(1),
                selectedDate.withDayOfMonth(1)
            )
            gridMonthOffset = monthsBetween
        }
    }

    val infiniteOffset = 10_000
    val calendarRowState = rememberLazyListState(initialFirstVisibleItemIndex = infiniteOffset)

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 4 })
    val notifAi = (LocalContext.current.applicationContext as MyTaskApplication).notifAi

    val completedCount = completedIds.size
    val progress = if (calendarTasks.isNotEmpty()) completedCount.toFloat() / calendarTasks.size else 0f
    val fabVisible = !showAddTaskDialog && !showAddEventDialog

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var tabBarHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetHeightPx by remember { mutableStateOf(0f) }

    LaunchedEffect(calendarIsGrid) { headerOffsetHeightPx = 0f }

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
        Box(modifier = Modifier.size(320.dp).offset(x = (-60).dp, y = (-40).dp).background(Brush.radialGradient(colors = listOf(Theme.Teal.copy(alpha = 0.07f), Color.Transparent)), CircleShape).blur(80.dp))
        Box(modifier = Modifier.size(260.dp).align(Alignment.BottomEnd).offset(x = 60.dp, y = 60.dp).background(Brush.radialGradient(colors = listOf(Theme.Blue.copy(alpha = 0.07f), Color.Transparent)), CircleShape).blur(80.dp))

        if (wide) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = hPad).padding(top = 20.dp).windowInsetsPadding(WindowInsets.statusBars), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TaskSidebar(unscheduledTasks = unscheduledTasks, onAddTask = { addTaskSource = 1; showAddTaskDialog = true }, onDragTask = { })
                Column(modifier = Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    DashboardHeader(today = today, visible = visible, onSettingsClick = { showSettingsDialog = true })
                    ProgressCard(isLoading = isLoading, visible = visible, totalTasks = calendarTasks.size, completedCount = completedCount, progress = progress, countdowns = activeCountdowns, isShowingCountdowns = isShowingCountdowns, onToggle = { isShowingCountdowns = !isShowingCountdowns }, onCountdownClick = { selectedCountdown = it }, delayMillis = 100)
                    MiniCalendar(visible = visible, today = today, selectedEpochDay = selectedDate.toEpochDay(), onDaySelected = { day -> haptic(); viewModel.setSelectedDate(LocalDate.ofEpochDay(day)) }, calendarIsGrid = calendarIsGrid, onToggleGrid = { calendarIsGrid = !calendarIsGrid; if (!calendarIsGrid) gridMonthOffset = 0L }, gridMonthOffset = gridMonthOffset, onGridMonthChange = { gridMonthOffset += it }, onResetMonth = { gridMonthOffset = 0L }, calendarRowState = calendarRowState, taskCounts = taskCounts, eventCounts = eventCounts, taskAgendas = taskAgendas, infiniteOffset = infiniteOffset, coroutineScope = coroutineScope, delayMillis = 200, onJumpToDateRequest = { showJumpToDateDialog = true })
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TabBar(pagerState = pagerState, calendarTasks = calendarTasks, calendarEvents = calendarEvents, coroutineScope = coroutineScope, visible = visible, delayMillis = 300)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f), beyondViewportPageCount = 1) { page ->
                        PageContent(page = page, isLoading = isLoading, calendarTasks = calendarTasks, calendarEvents = calendarEvents, selectedDate = selectedDate, notifAi = notifAi, completedIds = completedIds, countdowns = activeCountdowns, onToggleCompletion = { id, done -> haptic(); viewModel.toggleCompletion(id, done) }, onTaskUpdate = { viewModel.updateTask(it) }, onCountdownClick = { selectedCountdown = it }, onEventClick = { selectedEvent = it }, onTaskClick = { selectedTask = it }, horizontalPadding = hPad)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxWidth().offset { IntOffset(0, headerOffsetHeightPx.toInt()) }.onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }.background(Brush.verticalGradient(colors = listOf(Theme.BgDeep, Theme.BgMid), startY = 0f, endY = headerHeightPx + 200f)).padding(horizontal = hPad).windowInsetsPadding(WindowInsets.statusBars).padding(top = 16.dp)
                ) {
                    DashboardHeader(today = today, visible = visible, onSettingsClick = { showSettingsDialog = true })
                    ProgressCard(isLoading = isLoading, visible = visible, totalTasks = calendarTasks.size, completedCount = completedCount, progress = progress, countdowns = activeCountdowns, isShowingCountdowns = isShowingCountdowns, onToggle = { isShowingCountdowns = !isShowingCountdowns }, onCountdownClick = { selectedCountdown = it }, delayMillis = 100)
                    Spacer(modifier = Modifier.height(20.dp))
                    MiniCalendar(visible = visible, today = today, selectedEpochDay = selectedDate.toEpochDay(), onDaySelected = { day -> haptic(); viewModel.setSelectedDate(LocalDate.ofEpochDay(day)) }, calendarIsGrid = calendarIsGrid, onToggleGrid = { calendarIsGrid = !calendarIsGrid; if (!calendarIsGrid) gridMonthOffset = 0L }, gridMonthOffset = gridMonthOffset, onGridMonthChange = { gridMonthOffset += it }, onResetMonth = { gridMonthOffset = 0L }, calendarRowState = calendarRowState, taskCounts = taskCounts, eventCounts = eventCounts, taskAgendas = taskAgendas, infiniteOffset = infiniteOffset, coroutineScope = coroutineScope, delayMillis = 200, onJumpToDateRequest = { showJumpToDateDialog = true })
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Box(modifier = Modifier.fillMaxWidth().offset { val y = (headerOffsetHeightPx + headerHeightPx).coerceAtLeast(0f); IntOffset(0, y.toInt()) }.onGloballyPositioned { tabBarHeightPx = it.size.height.toFloat() }.background(Theme.BgMid).padding(horizontal = hPad, vertical = 8.dp)) {
                    TabBar(pagerState = pagerState, calendarTasks = calendarTasks, calendarEvents = calendarEvents, coroutineScope = coroutineScope, visible = visible, delayMillis = 300)
                }
                HorizontalPager(
                    state = pagerState, modifier = Modifier.fillMaxSize().offset { val currentPos = (headerHeightPx + tabBarHeightPx + headerOffsetHeightPx).coerceAtLeast(tabBarHeightPx); IntOffset(0, currentPos.toInt()) }, pageSpacing = 16.dp, userScrollEnabled = true, beyondViewportPageCount = 1
                ) { page ->
                    PageContent(page = page, isLoading = isLoading, calendarTasks = calendarTasks, calendarEvents = calendarEvents, selectedDate = selectedDate, notifAi = notifAi, completedIds = completedIds, countdowns = activeCountdowns, onToggleCompletion = { id, done -> haptic(); viewModel.toggleCompletion(id, done) }, onTaskUpdate = { viewModel.updateTask(it) }, onCountdownClick = { selectedCountdown = it }, onEventClick = { selectedEvent = it }, onTaskClick = { selectedTask = it }, onEditRequest = { task -> taskToEdit = task; addTaskSource = if (task.isReminder) 0 else 1; showAddTaskDialog = true }, horizontalPadding = hPad)
                }
            }
        }

        PullRefreshIndicator(refreshing = isLoading, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars), backgroundColor = Theme.CardBg, contentColor = Theme.Teal)

        if (isFabExpanded && fabVisible) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isFabExpanded = false })
        }

        CreationMenu(isExpanded = isFabExpanded, onToggle = { isFabExpanded = !isFabExpanded }, isVisible = fabVisible, onSelect = { label -> isFabExpanded = false; when (label) { "Reminder" -> { addTaskSource = 0; showAddTaskDialog = true }; "Task" -> { addTaskSource = 1; showAddTaskDialog = true }; "Event" -> { showAddEventDialog = true }; "Countdown" -> { showAddCountdownDialog = true } } }, haptic = haptic, bottomInset = bottomInset)
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
            onDismissSettings = { showSettingsDialog = false },
            onOpenAiKnowledge = { showAiKnowledge = true },
            onOpenCategoryManager = { showCategoryManager = true },
            onOpenPermissions = { showPermissions = true }
        )

        if (showAiKnowledge) {
            val knowledge by viewModel.aiKnowledge.collectAsState()
            knowledge?.let {
                AiKnowledgeScreen(
                    knowledge = it,
                    onDismiss = { showAiKnowledge = false }
                )
            }
        }

        if (showCategoryManager) {
            val categories by viewModel.categories.collectAsState()
            CategoryManagerScreen(
                categories = categories,
                onAdd = { label, icon, color -> viewModel.addCategory(label, icon, color) },
                onUpdate = { viewModel.updateCategory(it) },
                onDelete = { viewModel.deleteCategory(it) },
                onReorder = { viewModel.reorderCategories(it) },
                onDismiss = { showCategoryManager = false }
            )
        }

        if (showPermissions) {
            PermissionsScreen(
                onDismiss = { showPermissions = false }
            )
        }

        if (showJumpToDateDialog) {
            val dpState = rememberDatePickerState(initialSelectedDate = selectedDate)
            DatePickerDialog(
                onDismissRequest = { showJumpToDateDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { millis ->
                            val jumped = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.setSelectedDate(jumped)
                            val diff = ChronoUnit.DAYS.between(today, jumped).toInt()
                            coroutineScope.launch { calendarRowState.scrollToItem(infiniteOffset + diff) }
                        }
                        showJumpToDateDialog = false
                    }) { Text("Jump", color = Theme.Teal, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpToDateDialog = false }) { Text("Cancel", color = Theme.White60) }
                },
                colors = DatePickerDefaults.colors(containerColor = Theme.CardBg)
            ) {
                DatePicker(state = dpState, colors = DatePickerDefaults.colors(containerColor = Theme.CardBg, titleContentColor = Theme.White, headlineContentColor = Theme.Teal, selectedDayContainerColor = Theme.Teal, selectedDayContentColor = Theme.BgDeep, todayContentColor = Theme.Teal, todayDateBorderColor = Theme.Teal))
            }
        }

        selectedCountdown?.let { countdown ->
            var linkedInfo by remember(countdown.id) { mutableStateOf<Pair<String, TaskCategory>?>(null) }
            LaunchedEffect(countdown.linkedItemId) {
                if (countdown.linkedItemId != null && countdown.linkedItemType != null) {
                    linkedInfo = viewModel.getLinkedItemInfo(countdown.linkedItemId, countdown.linkedItemType)
                }
            }
            CountdownDetailScreen(item = countdown, linkedItemInfo = linkedInfo, onDismiss = { selectedCountdown = null }, onEdit = { updated -> viewModel.updateCountdown(updated); selectedCountdown = updated }, onDelete = { viewModel.deleteCountdown(countdown); selectedCountdown = null })
        }

        selectedEvent?.let { event ->
            EventDetailScreen(
                event = event,
                onDismiss = { selectedEvent = null },
                onDelete = { 
                    viewModel.deleteEvent(event)
                    selectedEvent = null 
                }
            )
        }

        selectedTask?.let { task ->
            val isCompleted = completedIds.contains(task.id)
            TaskDetailScreen(
                task = task,
                isCompleted = isCompleted,
                onDismiss = { selectedTask = null },
                onToggleCompletion = { 
                    haptic()
                    viewModel.toggleCompletion(task.id, !isCompleted)
                },
                onDelete = {
                    viewModel.deleteTask(task.id)
                    selectedTask = null
                }
            )
        }
    }
}

// ==================== EXTRACTED COMPOSABLES ====================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DashboardHeader(today: LocalDate, visible: Boolean, onSettingsClick: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(600)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { -100 })) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp).wrapContentWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colors = listOf(Theme.Teal, Color(0xFF00C9A7)))).padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
                    val logoScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "logoScale")
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
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Theme.White06)) { Icon(Icons.Default.Settings, "Settings", tint = Theme.White60, modifier = Modifier.size(22.dp)) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ProgressCard(isLoading: Boolean, visible: Boolean, totalTasks: Int, completedCount: Int, progress: Float, countdowns: List<com.algo1127.mytask.ui.models.CountdownItem>, isShowingCountdowns: Boolean, onToggle: () -> Unit, onCountdownClick: (com.algo1127.mytask.ui.models.CountdownItem) -> Unit, delayMillis: Int = 0) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(600, delayMillis = delayMillis)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), initialOffsetY = { 100 })) {
        AnimatedContent(targetState = isLoading, transitionSpec = { (scaleIn(spring()) + fadeIn()) togetherWith (scaleOut(spring()) + fadeOut()) }, label = "loadingAnim") { loading ->
            if (loading) { ShimmerBlock(height = 110f) } else {
                val glowColor by animateColorAsState(targetValue = if (isShowingCountdowns) Theme.Gold else Theme.Teal, animationSpec = tween(600), label = "glowColor")
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable { onToggle() }.background(Brush.linearGradient(colors = listOf(glowColor.copy(alpha = 0.15f), Theme.BgSurface))).padding(1.dp)) {
                    Box(modifier = Modifier.matchParentSize().blur(30.dp).background(Brush.radialGradient(colors = listOf(glowColor.copy(alpha = 0.15f), Color.Transparent), radius = 400f)))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(Theme.CardBg).padding(20.dp)) {
                        AnimatedContent(
                            targetState = isShowingCountdowns, 
                            transitionSpec = { 
                                if (targetState) {
                                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                                } else {
                                    (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                                }
                            }, 
                            label = "cardContent"
                        ) { showCountdowns ->
                            if (!showCountdowns) {
                                val progressValue = progress
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(80.dp), strokeWidth = 8.dp, color = Theme.White06, trackColor = Color.Transparent)
                                        val animProgress by animateFloatAsState(targetValue = progressValue, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow), label = "progressAnim")
                                        CircularProgressIndicator(progress = { animProgress }, modifier = Modifier.size(80.dp), strokeWidth = 8.dp, color = Theme.Teal, trackColor = Color.Transparent, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${(progressValue * 100).toInt()}%", color = Theme.Teal, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text("done", color = Theme.White30, fontSize = 10.sp) }
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Daily Progress", color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                            } else {
                                if (countdowns.isEmpty()) { Row(verticalAlignment = Alignment.CenterVertically) { Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) { Icon(Icons.Default.HourglassEmpty, null, tint = Theme.White10, modifier = Modifier.size(40.dp)) }; Spacer(modifier = Modifier.width(20.dp)); Column { Text("Countdowns", color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("No active timers", color = Theme.White30, fontSize = 13.sp) } } } 
                                else if (countdowns.size == 1) {
                                    val item = countdowns.first()
                                    val remaining = java.time.Duration.between(LocalDateTime.now(), item.targetDateTime)
                                    val total = java.time.Duration.between(item.createdAt, item.targetDateTime)
                                    val countdownProgress = if (total.isZero) 1f else (1f - remaining.toMillis().toFloat() / total.toMillis().toFloat()).coerceIn(0f, 1f)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(80.dp), strokeWidth = 8.dp, color = Theme.White06, trackColor = Color.Transparent)
                                            val animP by animateFloatAsState(targetValue = countdownProgress, animationSpec = tween(1000), label = "cp")
                                            CircularProgressIndicator(progress = { animP }, modifier = Modifier.size(80.dp), strokeWidth = 8.dp, color = Color(item.color), trackColor = Color.Transparent, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                                            Icon(Icons.Default.HourglassEmpty, null, tint = Color(item.color).copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(20.dp))
                                        Column(modifier = Modifier.weight(1f)) { Text(item.optionalTitle ?: item.title, color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(text = formatDuration(remaining), color = Color(item.color).copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                                    }
                                } else {
                                    val ticker = remember { mutableStateOf(LocalDateTime.now()) }
                                    LaunchedEffect(Unit) { while(true) { kotlinx.coroutines.delay(1000L); ticker.value = LocalDateTime.now() } }
                                    Column {
                                        Text("Countdowns", color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            val itemsToDisplay = countdowns.take(if (countdowns.size == 2) 2 else 4)
                                            itemsToDisplay.forEach { item ->
                                                val nowValue = ticker.value
                                                val remaining = java.time.Duration.between(nowValue, item.targetDateTime)
                                                val total = java.time.Duration.between(item.createdAt, item.targetDateTime)
                                                val countdownProgress = if (total.isZero) 1f else (1f - remaining.toMillis().toFloat() / total.toMillis().toFloat()).coerceIn(0f, 1f)
                                                val chartSize = if (countdowns.size == 2) 56.dp else 44.dp
                                                val strokeWidth = if (countdowns.size == 2) 6.dp else 4.dp
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(chartSize)) {
                                                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(chartSize), strokeWidth = strokeWidth, color = Theme.White06, trackColor = Color.Transparent)
                                                        val animP by animateFloatAsState(targetValue = countdownProgress, animationSpec = tween(1000), label = "cp_${item.id}")
                                                        CircularProgressIndicator(progress = { animP }, modifier = Modifier.size(chartSize), strokeWidth = strokeWidth, color = Color(item.color), trackColor = Color.Transparent, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                        if (countdowns.size == 2) Icon(Icons.Default.HourglassEmpty, null, tint = Color(item.color).copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(text = item.optionalTitle ?: item.title, color = Theme.White, fontSize = if (countdowns.size == 2) 12.sp else 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                                                    Text(text = formatHighestUnit(remaining), color = Color(item.color).copy(alpha = 0.7f), fontSize = if (countdowns.size == 2) 10.sp else 9.sp, fontWeight = FontWeight.Medium, maxLines = 1, textAlign = TextAlign.Center)
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
        }
    }
}

private fun formatDuration(duration: java.time.Duration): String {
    val years = duration.toDays() / 365
    val days = duration.toDays() % 365
    val hours = duration.toHours() % 24
    val mins = duration.toMinutes() % 60
    return when {
        years > 0 -> "$years ${if (years == 1L) "Year" else "Years"}, $days d left"
        days > 0 -> "$days ${if (days == 1L) "Day" else "Days"}, $hours h left"
        hours > 0 -> "$hours h, $mins m left"
        else -> "$mins mins left"
    }
}

private fun formatHighestUnit(duration: java.time.Duration): String {
    if (duration.isNegative) return "Done"
    val years = duration.toDays() / 365
    val days = duration.toDays() % 365
    val hours = duration.toHours() % 24
    val mins = duration.toMinutes() % 60
    val secs = duration.seconds % 60
    return when {
        years > 0 -> "$years ${if (years == 1L) "Year" else "Years"}"
        days > 0 -> "$days ${if (days == 1L) "Day" else "Days"}"
        hours > 0 -> "$hours ${if (hours == 1L) "Hr" else "Hrs"}"
        mins > 0 -> "$mins ${if (mins == 1L) "Min" else "Mins"}"
        else -> "$secs Sec"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MonthYearLabel(
    calendarIsGrid: Boolean,
    gridMonthOffset: Long,
    today: LocalDate,
    calendarRowState: androidx.compose.foundation.lazy.LazyListState,
    infiniteOffset: Int,
    onClick: () -> Unit
) {
    val currentMonthDate by remember(calendarIsGrid, gridMonthOffset) {
        derivedStateOf {
            if (calendarIsGrid) {
                today.withDayOfMonth(1).plusMonths(gridMonthOffset)
            } else {
                val idx = calendarRowState.firstVisibleItemIndex
                today.plusDays((idx - infiniteOffset).toLong()).withDayOfMonth(1)
            }
        }
    }

    Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }) {
        AnimatedContent(
            targetState = currentMonthDate,
            transitionSpec = { 
                if (targetState.isAfter(initialState)) {
                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                } else {
                    (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                }
            },
            label = "monthYearAnim"
        ) { date -> 
            val label = remember(date) { "${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}" }
            Text(label, color = Theme.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) 
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MiniCalendar(
    visible: Boolean, today: LocalDate, selectedEpochDay: Long, onDaySelected: (Long) -> Unit, 
    calendarIsGrid: Boolean, onToggleGrid: () -> Unit, gridMonthOffset: Long, onGridMonthChange: (Long) -> Unit, 
    onResetMonth: () -> Unit, calendarRowState: androidx.compose.foundation.lazy.LazyListState, 
    taskCounts: Map<LocalDate, Int>, eventCounts: Map<LocalDate, Int>, taskAgendas: Map<LocalDate, List<TaskItem>>, 
    infiniteOffset: Int, coroutineScope: kotlinx.coroutines.CoroutineScope, delayMillis: Int, onJumpToDateRequest: () -> Unit
) {
    BoxWithConstraints {
        val containerWidth = maxWidth
        val itemsToShow = remember(containerWidth) { if (containerWidth > 600.dp) 9 else 7 }
        val dayWidth = remember(containerWidth, itemsToShow) { (containerWidth / itemsToShow.toFloat()) - 8.dp }

        AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, delayMillis = delayMillis)) + slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 60 })) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    MonthYearLabel(
                        calendarIsGrid = calendarIsGrid,
                        gridMonthOffset = gridMonthOffset,
                        today = today,
                        calendarRowState = calendarRowState,
                        infiniteOffset = infiniteOffset,
                        onClick = onJumpToDateRequest
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val showTodayButton by remember(selectedEpochDay, calendarIsGrid, gridMonthOffset) {
                            derivedStateOf {
                                if (calendarIsGrid) {
                                    gridMonthOffset != 0L || selectedEpochDay != today.toEpochDay()
                                } else {
                                    val firstIdx = calendarRowState.firstVisibleItemIndex
                                    val isTodayVisible = infiniteOffset >= firstIdx && infiniteOffset < firstIdx + itemsToShow
                                    !isTodayVisible || selectedEpochDay != today.toEpochDay()
                                }
                            }
                        }
                        AnimatedVisibility(visible = showTodayButton, enter = fadeIn() + scaleIn(spring(Spring.DampingRatioMediumBouncy)), exit = fadeOut() + scaleOut()) {
                            Box(
                                contentAlignment = Alignment.Center, 
                                modifier = Modifier
                                    .height(32.dp)
                                    .wrapContentWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Theme.White06)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                                        coroutineScope.launch { if (!calendarIsGrid) calendarRowState.animateScrollToItem(infiniteOffset) else onResetMonth() }
                                        onDaySelected(today.toEpochDay()) 
                                    }
                                    .padding(horizontal = 12.dp)
                            ) { 
                                Text("Today", color = Theme.White60, fontSize = 12.sp, fontWeight = FontWeight.Bold) 
                            }
                        }
                        if (calendarIsGrid) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Theme.White06).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGridMonthChange(-1) }) { Icon(Icons.Default.ChevronLeft, null, tint = Theme.White60, modifier = Modifier.size(18.dp)) }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Theme.White06).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGridMonthChange(1) }) { Icon(Icons.Default.ChevronRight, null, tint = Theme.White60, modifier = Modifier.size(18.dp)) }
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Theme.White06).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleGrid() }) {
                            AnimatedContent(targetState = calendarIsGrid, transitionSpec = { scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(150)) togetherWith scaleOut(tween(100)) + fadeOut(tween(100)) }, label = "calToggleIcon") { isGrid -> Icon(if (isGrid) Icons.Outlined.ViewStream else Icons.Outlined.GridView, "Toggle view", tint = Theme.White60, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
                AnimatedContent(targetState = calendarIsGrid, transitionSpec = { (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.98f)) togetherWith (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.98f)) }, label = "calViewToggle") { isGrid ->
                    if (isGrid) {
                        val gridBase = today.withDayOfMonth(1).plusMonths(gridMonthOffset)
                        val daysInMonth = gridBase.lengthOfMonth()
                        val firstDayOfWeek = gridBase.dayOfWeek.value
                        val startPadding = firstDayOfWeek - 1
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            // Weekday Labels
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) { 
                                listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { d -> 
                                    Text(
                                        text = d, 
                                        color = if (d == "SAT" || d == "SUN") Theme.White30 else Theme.White60, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        modifier = Modifier.weight(1f), 
                                        textAlign = TextAlign.Center
                                    ) 
                                } 
                            }

                            // Calculate grid rows for the current month only
                            val totalCells = startPadding + daysInMonth
                            val rows = (totalCells + 6) / 7
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(rows) { rowIndex ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(7) { colIndex ->
                                            val cellIndex = rowIndex * 7 + colIndex
                                            val dayOfMonth = cellIndex - startPadding + 1
                                            
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (dayOfMonth in 1..daysInMonth) {
                                                    val day = gridBase.withDayOfMonth(dayOfMonth)
                                                    val isT = day == today
                                                    val isS = day.toEpochDay() == selectedEpochDay
                                                    val scale by animateFloatAsState(
                                                        targetValue = if (isS) 1.05f else 1f, 
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), 
                                                        label = "gridDayAnim"
                                                    )
                                                    
                                                    val tCount = taskCounts[day] ?: 0
                                                    val eCount = eventCounts[day] ?: 0
                                                    val workload = (tCount + eCount * 2).coerceIn(0, 10)
                                                    DayItem(
                                                        day = day, 
                                                        isSelected = isS, 
                                                        isToday = isT, 
                                                        weekday = "", 
                                                        taskCount = tCount, 
                                                        eventCount = eCount, 
                                                        workload = workload, 
                                                        dayAgenda = taskAgendas[day] ?: emptyList(), 
                                                        scale = scale, 
                                                        width = Dp.Unspecified, 
                                                        isGrid = true, 
                                                        isInCurrentMonth = true,
                                                        onClick = { onDaySelected(day.toEpochDay()) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val density = LocalDensity.current
                        val itemWidthPx = remember(dayWidth) { with(density) { dayWidth.toPx() } }
                        val spacingPx = remember { with(density) { 8.dp.toPx() } }
                        val horizontalPaddingPx = remember { with(density) { 8.dp.toPx() } }
                        val topOffsetPx = remember { with(density) { 10.dp.toPx() } }
                        val pillHeightPx = remember { with(density) { 90.dp.toPx() } }
                        val cornerRadiusPx = remember { with(density) { 16.dp.toPx() } }
                        val selectedIdx = (selectedEpochDay - today.toEpochDay() + infiniteOffset).toInt()
                        val previousIdx = remember { mutableIntStateOf(selectedIdx) }
                        val movingRight = selectedIdx > previousIdx.intValue
                        SideEffect { previousIdx.intValue = selectedIdx }
                        val animIdxStart by animateFloatAsState(targetValue = selectedIdx.toFloat(), animationSpec = spring(stiffness = if (movingRight) 350f else 800f, dampingRatio = 0.85f), label = "idxStart")
                        val animIdxEnd by animateFloatAsState(targetValue = (selectedIdx + 1).toFloat(), animationSpec = spring(stiffness = if (movingRight) 800f else 350f, dampingRatio = 0.85f), label = "idxEnd")
                        Box(modifier = Modifier.fillMaxWidth().height(110.dp).drawWithCache { val gradient = Brush.verticalGradient(colors = listOf(Theme.Teal, Color(0xFF00C9A7))); onDrawBehind { val firstVisible = calendarRowState.firstVisibleItemIndex; val firstVisibleOffset = calendarRowState.firstVisibleItemScrollOffset; val startX = (animIdxStart - firstVisible) * (itemWidthPx + spacingPx) - firstVisibleOffset + horizontalPaddingPx; val endX = (animIdxEnd - firstVisible) * (itemWidthPx + spacingPx) - spacingPx - firstVisibleOffset + horizontalPaddingPx; drawRoundRect(brush = gradient, topLeft = Offset(startX, topOffsetPx), size = Size(kotlin.math.max(0f, endX - startX), pillHeightPx), cornerRadius = CornerRadius(cornerRadiusPx)) } }) {
                            LazyRow(state = calendarRowState, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                                items(count = 20_000) { index ->
                                    val day = today.plusDays((index - infiniteOffset).toLong())
                                    val isT = day == today; val isS = day.toEpochDay() == selectedEpochDay
                                    val weekday = when(day.dayOfWeek.value) { 1 -> "MO"; 2 -> "TU"; 3 -> "WE"; 4 -> "TH"; 5 -> "FR"; 6 -> "SA"; else -> "SU" }
                                    val tCount = taskCounts[day] ?: 0
                                    val eCount = eventCounts[day] ?: 0
                                    val workload = (tCount + eCount * 2).coerceIn(0, 10)
                                    val scale by animateFloatAsState(targetValue = if (isS) 1.05f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "calDay_$index")
                                    DayItem(day = day, isSelected = isS, isToday = isT, weekday = weekday, taskCount = tCount, eventCount = eCount, workload = workload, dayAgenda = taskAgendas[day] ?: emptyList(), scale = scale, width = dayWidth, onClick = { onDaySelected(day.toEpochDay()) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DayItem(
    day: LocalDate, 
    isSelected: Boolean, 
    isToday: Boolean, 
    weekday: String, 
    taskCount: Int, 
    eventCount: Int, 
    workload: Int, 
    dayAgenda: List<TaskItem>, 
    scale: Float, 
    width: Dp, 
    isGrid: Boolean = false, 
    isInCurrentMonth: Boolean = true, 
    onClick: () -> Unit
) {
    var showPeek by remember { mutableStateOf(false) }
    val haptic = rememberHaptic()
    
    val alphaAnim by animateFloatAsState(targetValue = if (isInCurrentMonth) 1f else 0.25f, label = "dayAlpha")
    
    val shape = remember { RoundedCornerShape(16.dp) }

    Box(
        contentAlignment = Alignment.Center, 
        modifier = Modifier.graphicsLayer {
            alpha = alphaAnim
            scaleX = scale
            scaleY = scale
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.Center,
            modifier = (if (isGrid) Modifier.aspectRatio(1f) else Modifier.width(width).fillMaxHeight())
                .then(
                    when {
                        isSelected && isGrid -> {
                            Modifier
                                .shadow(4.dp, shape, spotColor = Theme.Teal)
                                .background(Brush.verticalGradient(colors = listOf(Theme.Teal, Color(0xFF00C9A7))), shape)
                        }
                        isSelected && !isGrid -> {
                            Modifier // Background handled by parent selection indicator
                        }
                        isToday -> {
                            Modifier
                                .then(if (!isGrid) Modifier.background(Theme.White06, shape) else Modifier)
                                .border(1.dp, Theme.Teal.copy(alpha = 0.5f), shape)
                        }
                        !isGrid -> {
                            Modifier.background(Theme.White03, shape)
                        }
                        else -> Modifier
                    }
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() }, 
                    indication = null, 
                    onClick = { onClick() }, 
                    onLongClick = { haptic(); showPeek = true }
                )
                .padding(vertical = if (isGrid) 4.dp else 8.dp)
        ) {
            Text(
                text = day.dayOfMonth.toString(), 
                color = if (isSelected) Theme.BgDeep else Theme.White, 
                fontSize = if (isGrid) 16.sp else 18.sp, 
                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium
            )
            if (!isGrid && weekday.isNotEmpty()) {
                Text(
                    text = weekday, 
                    color = if (isSelected) Theme.BgDeep.copy(alpha = 0.6f) else Theme.White30, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            if (taskCount > 0 || eventCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(4.dp)) { 
                    if (taskCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(if (isGrid) 3.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Theme.BgDeep.copy(alpha = 0.5f) else Theme.Gold)
                        ) 
                    }
                    if (eventCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(if (isGrid) 3.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Theme.BgDeep.copy(alpha = 0.5f) else Theme.Blue)
                        ) 
                    }
                }
            } else if (isGrid) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        if (showPeek) {
            androidx.compose.ui.window.Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -180), onDismissRequest = { showPeek = false }) {
                Surface(color = Theme.CardBg.copy(alpha = 0.95f), shape = RoundedCornerShape(18.dp), modifier = Modifier.width(200.dp).border(1.dp, Theme.White10, RoundedCornerShape(18.dp)).shadow(20.dp, RoundedCornerShape(18.dp))) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Agenda Preview", color = Theme.Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (dayAgenda.isEmpty()) Text("Clear schedule", color = Theme.White30, fontSize = 11.sp)
                        else dayAgenda.forEach { task ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(task.category.color))
                                Spacer(Modifier.width(8.dp))
                                Text(task.title, color = Theme.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, delayMillis = delayMillis)) + slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 60 })) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Theme.White06).padding(4.dp)) {
            val totalWidth = maxWidth; val itemWidth = totalWidth / 4f; val selectedIndex = pagerState.currentPage; val previousIndex = remember { mutableIntStateOf(selectedIndex) }; val movingRight = selectedIndex > previousIndex.intValue; SideEffect { previousIndex.intValue = selectedIndex }; val indicatorStart by animateDpAsState(targetValue = itemWidth * selectedIndex, animationSpec = spring(stiffness = if (movingRight) 350f else 800f, dampingRatio = 0.85f), label = "indicatorStart"); val indicatorEnd by animateDpAsState(targetValue = itemWidth * (selectedIndex + 1), animationSpec = spring(stiffness = if (movingRight) 800f else 350f, dampingRatio = 0.85f), label = "indicatorEnd"); val indicatorColor by animateColorAsState(targetValue = when (selectedIndex) { 0 -> Theme.Teal; 1 -> Theme.Purple; 2 -> Theme.Blue; else -> Theme.Gold }, animationSpec = tween(400), label = "indicatorColor")
            Box(modifier = Modifier.offset(x = indicatorStart).width(indicatorEnd - indicatorStart).height(48.dp).background(indicatorColor, RoundedCornerShape(12.dp)).shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = indicatorColor, spotColor = indicatorColor))
            Row(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(Triple(0, Icons.Outlined.Schedule, "Reminders"), Triple(1, Icons.Default.Task, "Tasks"), Triple(2, Icons.Outlined.Event, "Events"), Triple(3, Icons.Default.HourglassEmpty, "Timers"))
                tabs.forEach { (page, icon, label) ->
                    val selected = selectedIndex == page; val badgeCount = when (page) { 0 -> calendarTasks.count { it.isReminder }; 1 -> calendarTasks.count { !it.isReminder }; 2 -> calendarEvents.size; else -> 0 }; val badgeColor = when (page) { 0 -> Theme.Teal; 1 -> Theme.Purple; 2 -> Theme.Blue; else -> Theme.Gold }; val tabScale by animateFloatAsState(targetValue = if (selected) 1.05f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tabScale_$page")
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(48.dp).scale(tabScale).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { coroutineScope.launch { val distance = kotlin.math.abs(page - selectedIndex); if (distance == 1) pagerState.animateScrollToPage(page) else pagerState.scrollToPage(page) } }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = if (selected) when(page) { 0 -> Icons.Default.Schedule; 1 -> Icons.Default.Task; 2 -> Icons.Default.Event; else -> Icons.Default.HourglassFull } else icon, contentDescription = label, tint = if (selected) Theme.BgDeep else Theme.White30, modifier = Modifier.size(18.dp))
                            if (selected) { Spacer(modifier = Modifier.width(6.dp)); Text(text = label, color = Theme.BgDeep, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
                            if (badgeCount > 0) { Spacer(modifier = Modifier.width(4.dp)); Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp).clip(CircleShape).background(if (selected) Theme.White else badgeColor)) { Text(text = badgeCount.toString(), color = if (selected) indicatorColor else Theme.BgDeep, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) } }
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
    countdowns: List<com.algo1127.mytask.ui.models.CountdownItem>, 
    onToggleCompletion: (Long, Boolean) -> Unit, 
    onTaskUpdate: (TaskItem) -> Unit, 
    onCountdownClick: (com.algo1127.mytask.ui.models.CountdownItem) -> Unit, 
    onEventClick: (EventItem) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onEditRequest: (TaskItem) -> Unit = {}, 
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    AnimatedContent(targetState = page, transitionSpec = { val stiffness = Spring.StiffnessMediumLow; if (targetState > initialState) { (slideInHorizontally(spring(stiffness = stiffness, dampingRatio = 0.8f)) { it } + fadeIn()).togetherWith(slideOutHorizontally(spring(stiffness = stiffness, dampingRatio = 0.8f)) { -it } + fadeOut()) } else { (slideInHorizontally(spring(stiffness = stiffness, dampingRatio = 0.8f)) { -it } + fadeIn()).togetherWith(slideOutHorizontally(spring(stiffness = stiffness, dampingRatio = 0.8f)) { it } + fadeOut()) }.using(SizeTransform(clip = false)) }, label = "pageTransition") { targetPage ->
        Box(Modifier.fillMaxSize().padding(horizontal = horizontalPadding)) {
            if (isLoading && false) { Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(3) { ShimmerBlock(height = 80f) } } } 
            else {
                when (targetPage) {
                    0 -> RemindersTab(tasks = calendarTasks, selectedDate = selectedDate, notifAi = notifAi as com.algo1127.mytask.NotifAi.NotifAi, completedIds = completedIds, onToggleCompletion = onToggleCompletion, onTaskClick = { /* Reminders Detail Disabled */ }, onEditRequest = onEditRequest)
                    1 -> TasksTab(tasks = calendarTasks, selectedDate = selectedDate, completedIds = completedIds, onToggleCompletion = onToggleCompletion, onTaskUpdate = onTaskUpdate, onTaskClick = onTaskClick, onEditRequest = onEditRequest)
                    2 -> EventsTab(events = calendarEvents, selectedDate = selectedDate, completedIds = completedIds, onToggleCompletion = onToggleCompletion, onEventClick = onEventClick)
                    3 -> CountdownsTab(countdowns = countdowns, onClick = onCountdownClick)
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
