package com.algo1127.mytask.ui.timetable

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.data.TimetableRepository
import com.algo1127.mytask.data.TimetableSlot
import com.algo1127.mytask.ui.Theme
import com.algo1127.mytask.ui.timetable.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TimetableDisplayMode {
    WeeklyMatrix,
    DayCarousel
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repository = remember { TimetableRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    val allSlots by repository.allSlotsFlow.collectAsState(initial = emptyList())
    val showInEventsTab by repository.showInEventsTabFlow.collectAsState()

    var displayMode by remember { mutableStateOf(TimetableDisplayMode.WeeklyMatrix) }
    var selectedCarouselDay by remember { mutableStateOf(DayOfWeek.MONDAY) }

    // Multi-selection state
    val selectedSlotIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedSlotIds.isNotEmpty()

    // Dialog states
    var slotToEdit by remember { mutableStateOf<TimetableSlot?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var addDialogDefaultDay by remember { mutableStateOf(DayOfWeek.MONDAY) }

    var cloneSourceDay by remember { mutableStateOf<DayOfWeek?>(null) }

    // FAB spring animation
    var isFabPressed by remember { mutableStateOf(false) }
    val fabScale by animateFloatAsState(
        targetValue = if (isFabPressed) 0.82f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fabScale"
    )

    LaunchedEffect(isFabPressed) {
        if (isFabPressed) {
            delay(150)
            isFabPressed = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Theme.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Timetable",
                        color = Theme.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fluid Spring View Mode Switcher
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Theme.CardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Theme.CardBorder),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(3.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val totalWidth = 160.dp
                            val tabWidth = totalWidth / 2f
                            val selectedIdx = if (displayMode == TimetableDisplayMode.WeeklyMatrix) 0 else 1

                            val previousIdx = remember { mutableIntStateOf(selectedIdx) }
                            val movingRight = selectedIdx > previousIdx.intValue
                            SideEffect { previousIdx.intValue = selectedIdx }

                            val indicatorStart by animateDpAsState(
                                targetValue = tabWidth * selectedIdx,
                                animationSpec = spring(stiffness = if (movingRight) 400f else 900f, dampingRatio = 0.8f),
                                label = "tabStart"
                            )
                            val indicatorEnd by animateDpAsState(
                                targetValue = tabWidth * (selectedIdx + 1),
                                animationSpec = spring(stiffness = if (movingRight) 900f else 400f, dampingRatio = 0.8f),
                                label = "tabEnd"
                            )

                            // Fluid Spring Sliding Pill Background
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorStart)
                                    .width(indicatorEnd - indicatorStart)
                                    .fillMaxHeight()
                                    .background(Theme.Teal, RoundedCornerShape(12.dp))
                            )

                            // Tabs
                            Row(modifier = Modifier.width(totalWidth)) {
                                // Grid Option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { displayMode = TimetableDisplayMode.WeeklyMatrix },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = "Matrix Grid View",
                                            tint = if (selectedIdx == 0) Theme.BgDeep else Theme.White60,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Grid",
                                            color = if (selectedIdx == 0) Theme.BgDeep else Theme.White60,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Cards Option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { displayMode = TimetableDisplayMode.DayCarousel },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ViewDay,
                                            contentDescription = "Carousel View",
                                            tint = if (selectedIdx == 1) Theme.BgDeep else Theme.White60,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Cards",
                                            color = if (selectedIdx == 1) Theme.BgDeep else Theme.White60,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Animated Action FAB
                    Surface(
                        onClick = {
                            isFabPressed = true
                            slotToEdit = null
                            addDialogDefaultDay = selectedCarouselDay
                            showAddEditDialog = true
                        },
                        shape = CircleShape,
                        color = Theme.Teal,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = fabScale
                                scaleY = fabScale
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add slot",
                                tint = Theme.BgDeep,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Events Overlay Preference Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Theme.CardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, Theme.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Timetable in Events Tab",
                            color = Theme.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Overlay timetable slots in daily events view",
                            color = Theme.White30,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = showInEventsTab,
                        onCheckedChange = { repository.setShowInEventsTab(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Theme.Teal,
                            checkedTrackColor = Theme.Teal.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main Content Area
            AnimatedContent(
                targetState = displayMode,
                transitionSpec = {
                    if (targetState == TimetableDisplayMode.WeeklyMatrix) {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    } else {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    }
                },
                label = "displayModeTransition",
                modifier = Modifier.weight(1f)
            ) { mode ->
                if (mode == TimetableDisplayMode.WeeklyMatrix) {
                    // Excel-style Weekly Time Matrix Grid
                    WeeklyTimeMatrixGrid(
                        slots = allSlots,
                        onSlotClick = { slot ->
                            slotToEdit = slot
                            showAddEditDialog = true
                        },
                        onCellClick = { day, _ ->
                            slotToEdit = null
                            addDialogDefaultDay = day
                            showAddEditDialog = true
                        },
                        onDayLongPress = { day ->
                            cloneSourceDay = day
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    // Day Carousel View
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Day Selector Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayOfWeek.entries.forEach { day ->
                                val isSelected = day == selectedCarouselDay
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCarouselDay = day },
                                    label = {
                                        Text(
                                            text = day.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                            color = if (isSelected) Theme.BgDeep else Theme.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Theme.Teal,
                                        containerColor = Theme.CardBg
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        val carouselDaySlots = allSlots.filter { it.dayOfWeek == selectedCarouselDay }.sortedBy { it.startTime }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (carouselDaySlots.isEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp)
                                ) {
                                    Text(
                                        text = "No entries for ${selectedCarouselDay.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        color = Theme.White30,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                carouselDaySlots.forEach { slot ->
                                    val isSelected = selectedSlotIds.contains(slot.id)
                                    TimetableSlotCard(
                                        slot = slot,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        onClick = {
                                            if (isSelectionMode) {
                                                if (isSelected) selectedSlotIds.remove(slot.id) else selectedSlotIds.add(slot.id)
                                            } else {
                                                slotToEdit = slot
                                                showAddEditDialog = true
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelected) selectedSlotIds.add(slot.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Batch Action Toolbar
            AnimatedVisibility(visible = isSelectionMode) {
                BatchActionBar(
                    selectedCount = selectedSlotIds.size,
                    onClearSelection = { selectedSlotIds.clear() },
                    onDeleteSelected = {
                        scope.launch {
                            repository.deleteSlots(selectedSlotIds.toList())
                            selectedSlotIds.clear()
                        }
                    }
                )
            }
        }

        // Dialogs
        if (showAddEditDialog) {
            AddEditSlotDialog(
                initialSlot = slotToEdit,
                defaultDay = addDialogDefaultDay,
                onDismiss = { showAddEditDialog = false },
                onSave = { newOrUpdatedSlot: TimetableSlot ->
                    scope.launch {
                        repository.insertSlot(newOrUpdatedSlot)
                        showAddEditDialog = false
                    }
                },
                onDelete = { slotId ->
                    scope.launch {
                        repository.deleteSlots(listOf(slotId))
                        showAddEditDialog = false
                    }
                }
            )
        }

        cloneSourceDay?.let { sourceDay ->
            CloneDayDialog(
                sourceDay = sourceDay,
                onDismiss = { cloneSourceDay = null },
                onClone = { targetDays: List<DayOfWeek> ->
                    scope.launch {
                        repository.cloneDaySchedule(sourceDay, targetDays)
                        cloneSourceDay = null
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeeklyTimeMatrixGrid(
    slots: List<TimetableSlot>,
    onSlotClick: (TimetableSlot) -> Unit,
    onCellClick: (DayOfWeek, LocalTime) -> Unit,
    onDayLongPress: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = remember { (0..23).map { LocalTime.of(it, 0) } }
    val days = DayOfWeek.entries
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var zoomLevel by remember { mutableFloatStateOf(0.5f) } // Default = 0.5f (Bird's Eye 50%)

    // Performance Optimization: Pre-group and partition slots per day
    val slotsByDay = remember(slots) {
        days.associateWith { day ->
            val sameDay = slots.filter { it.dayOfWeek == day }
            val prevDay = day.minus(1)
            val carryOver = slots.filter {
                it.dayOfWeek == prevDay &&
                (it.endTime.isBefore(it.startTime) || it.endTime == it.startTime) &&
                !(it.startTime.hour == 0 && it.startTime.minute == 0 && it.endTime.hour == 0 && it.endTime.minute == 0)
            }
            sameDay to carryOver
        }
    }

    val hourRowHeight = 65.dp * zoomLevel
    val dayColumnWidth = 140.dp * zoomLevel

    Column(modifier = modifier) {
        // Zoom Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Theme.Teal, modifier = Modifier.size(18.dp))
                Text(
                    text = "Zoom",
                    color = Theme.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Smooth Slider with Magnetic Snap at 0.5f (50%)
                Slider(
                    value = zoomLevel,
                    onValueChange = { raw ->
                        val snapped = if (raw in 0.47f..0.53f) 0.5f else raw
                        zoomLevel = snapped
                    },
                    valueRange = 0.3f..1.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = Theme.Teal,
                        activeTrackColor = Theme.Teal,
                        inactiveTrackColor = Theme.White10
                    ),
                    modifier = Modifier.width(160.dp)
                )

                // Functional Reset Button
                Surface(
                    onClick = { zoomLevel = 0.5f },
                    shape = RoundedCornerShape(10.dp),
                    color = Theme.White06,
                    border = if (zoomLevel == 0.5f) androidx.compose.foundation.BorderStroke(1.dp, Theme.Teal) else null
                ) {
                    Text(
                        text = "${(zoomLevel * 100).toInt()}%",
                        color = if (zoomLevel == 0.5f) Theme.Teal else Theme.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Theme.CardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, Theme.CardBorder),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
            ) {
                // Header Row (Days of Week X-Axis)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                        .background(Theme.BgMid)
                        .padding(vertical = (12 * zoomLevel).dp)
                ) {
                    // Time column header
                    Box(
                        modifier = Modifier
                            .width((65 * zoomLevel).dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Time", color = Theme.White30, fontSize = (11 * zoomLevel).sp, fontWeight = FontWeight.Bold)
                    }

                    days.forEach { day ->
                        Box(
                            modifier = Modifier
                                .width(dayColumnWidth)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onDayLongPress(day) }
                                )
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = Theme.White,
                                fontSize = (14 * zoomLevel).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = Theme.White06)

                // 24-Hour Proportional Matrix Canvas Body
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Left Time Column (00:00 to 23:59) - Pixel Perfect Alignment
                    Column(
                        modifier = Modifier.width((65 * zoomLevel).dp)
                    ) {
                        hours.forEach { hour ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(hourRowHeight)
                                    .border(0.25.dp, Theme.White03)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Text(
                                    text = hour.format(timeFmt),
                                    color = Theme.White30,
                                    fontSize = (11 * zoomLevel).sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    VerticalDivider(color = Theme.White06)

                    // Day Columns (Mon - Sun)
                    days.forEach { day ->
                        val (sameDaySlots, carryOverFromPrevSlots) = slotsByDay[day] ?: (emptyList<TimetableSlot>() to emptyList<TimetableSlot>())

                        Box(
                            modifier = Modifier
                                .width(dayColumnWidth)
                                .height(hourRowHeight * 24)
                                .border(0.5.dp, Theme.White03)
                        ) {
                            // Render hour background guidelines and click targets
                            Column(modifier = Modifier.fillMaxSize()) {
                                hours.forEach { hour ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(hourRowHeight)
                                            .border(0.25.dp, Theme.White03)
                                            .clickable { onCellClick(day, hour) }
                                    )
                                }
                            }

                            // 1. Render slots starting on 'day'
                            sameDaySlots.forEach { slot ->
                                val startMinutes = slot.startTime.hour * 60 + slot.startTime.minute
                                val endMinutes = slot.endTime.hour * 60 + slot.endTime.minute
                                val isOvernight = (endMinutes <= startMinutes) && !(startMinutes == 0 && endMinutes == 0)

                                val durationMinutes = if (isOvernight) {
                                    1440 - startMinutes
                                } else {
                                    endMinutes - startMinutes
                                }.coerceAtLeast(15)

                                val minuteHeight = hourRowHeight.value / 60f
                                val topOffsetDp = startMinutes * minuteHeight
                                val heightDp = durationMinutes * minuteHeight

                                val slotColor = Color(slot.colorHex)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = slotColor.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, slotColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(heightDp.dp)
                                        .offset(y = topOffsetDp.dp)
                                        .padding(horizontal = 3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSlotClick(slot) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(slotColor)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = slot.subjectName,
                                                color = Theme.White,
                                                fontSize = (11 * zoomLevel).sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (zoomLevel > 0.45f && heightDp >= 24) {
                                                Text(
                                                    text = "${slot.startTime.format(timeFmt)} - ${slot.endTime.format(timeFmt)}",
                                                    color = Theme.White60,
                                                    fontSize = (9.5 * zoomLevel).sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Render carry-over slots from previous day
                            carryOverFromPrevSlots.forEach { slot ->
                                val endMinutes = slot.endTime.hour * 60 + slot.endTime.minute
                                if (endMinutes > 0) {
                                    val minuteHeight = hourRowHeight.value / 60f
                                    val topOffsetDp = 0f
                                    val heightDp = (endMinutes * minuteHeight).coerceAtLeast(15f)

                                    val slotColor = Color(slot.colorHex)

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = slotColor.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, slotColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(heightDp.dp)
                                            .offset(y = topOffsetDp.dp)
                                            .padding(horizontal = 3.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSlotClick(slot) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.5.dp)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(slotColor)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = slot.subjectName,
                                                    color = Theme.White,
                                                    fontSize = (11 * zoomLevel).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (zoomLevel > 0.45f && heightDp >= 24) {
                                                    Text(
                                                        text = "${slot.startTime.format(timeFmt)} - ${slot.endTime.format(timeFmt)}",
                                                        color = Theme.White60,
                                                        fontSize = (9.5 * zoomLevel).sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        VerticalDivider(color = Theme.White03)
                    }
                }
            }
        }
    }
}
