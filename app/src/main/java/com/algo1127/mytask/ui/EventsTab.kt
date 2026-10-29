package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.data.TimetableRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// ─── Status enum ─────────────────────────────────────────────────────────────

private enum class EventStatus { PAST, ACTIVE, UPCOMING }

private fun eventStatus(eventDate: LocalDate, startTime: String, endTime: String): EventStatus {
    val today = LocalDate.now()
    val nowTime = LocalTime.now()

    val start = runCatching { LocalTime.parse(startTime) }.getOrNull() ?: return EventStatus.UPCOMING
    val end   = runCatching { LocalTime.parse(endTime)   }.getOrNull() ?: return EventStatus.UPCOMING

    return when {
        eventDate.isBefore(today) -> EventStatus.PAST
        eventDate.isAfter(today) -> EventStatus.UPCOMING
        else -> {
            when {
                nowTime.isAfter(end) -> EventStatus.PAST
                nowTime.isAfter(start) || nowTime == start -> EventStatus.ACTIVE
                else -> EventStatus.UPCOMING
            }
        }
    }
}

// ─── Main composable ──────────────────────────────────────────────────────────

@Composable
fun EventsTab(
    events: List<EventItem>, 
    selectedDate: LocalDate,
    completedIds: Set<Long>,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onEventClick: (EventItem) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val context = LocalContext.current
    val repository = remember { TimetableRepository.getInstance(context) }
    val showInEventsTab by repository.showInEventsTabFlow.collectAsState()
    val allTimetableSlots by repository.allSlotsFlow.collectAsState(initial = emptyList())

    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // Dynamic Live Synthesis of Timetable Events for selectedDate
    val synthesizedTimetableEvents = remember(showInEventsTab, allTimetableSlots, selectedDate) {
        if (!showInEventsTab || allTimetableSlots.isEmpty()) {
            emptyList()
        } else {
            val dayOfWeek = selectedDate.dayOfWeek
            val prevDayOfWeek = dayOfWeek.minus(1)

            val sameDaySlots = allTimetableSlots.filter { it.dayOfWeek == dayOfWeek }
            val prevDayCarryOverSlots = allTimetableSlots.filter { 
                it.dayOfWeek == prevDayOfWeek && 
                (it.endTime.isBefore(it.startTime) || it.endTime == it.startTime) && 
                !(it.startTime.hour == 0 && it.startTime.minute == 0 && it.endTime.hour == 0 && it.endTime.minute == 0)
            }

            val list = mutableListOf<EventItem>()

            sameDaySlots.forEach { slot ->
                val notesText = listOfNotNull(
                    slot.teacher.ifBlank { null }?.let { "Instructor: $it" },
                    "Recurring Timetable Class"
                ).joinToString("\n")

                list.add(
                    EventItem(
                        id = -(abs(slot.id) + selectedDate.toEpochDay()),
                        title = slot.subjectName,
                        startTime = slot.startTime.format(timeFmt),
                        endTime = slot.endTime.format(timeFmt),
                        location = slot.location,
                        date = selectedDate,
                        notes = notesText
                    )
                )
            }

            prevDayCarryOverSlots.forEach { slot ->
                val notesText = listOfNotNull(
                    slot.teacher.ifBlank { null }?.let { "Instructor: $it" },
                    "Recurring Timetable Class (Cont. from yesterday)"
                ).joinToString("\n")

                list.add(
                    EventItem(
                        id = -(abs(slot.id) + selectedDate.toEpochDay() + 500000),
                        title = "${slot.subjectName} (Cont.)",
                        startTime = "00:00",
                        endTime = slot.endTime.format(timeFmt),
                        location = slot.location,
                        date = selectedDate,
                        notes = notesText
                    )
                )
            }

            list
        }
    }

    val mergedSortedEvents = remember(events, synthesizedTimetableEvents, selectedDate) {
        (events.filter { it.date == selectedDate } + synthesizedTimetableEvents)
            .sortedWith(
                compareBy(
                    { runCatching { LocalTime.parse(it.startTime) }.getOrElse { LocalTime.MAX } },
                    { runCatching { LocalTime.parse(it.endTime)   }.getOrElse { LocalTime.MAX } }
                )
            )
    }

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = LocalTime.now()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 100.dp + bottomPadding)
    ) {
        if (mergedSortedEvents.isEmpty()) {
            item {
                EmptyState(
                    icon     = Icons.Outlined.Event,
                    title    = "No events",
                    subtitle = "Tap + to add an event",
                    color    = Theme.Blue
                )
            }
        } else {
            items(mergedSortedEvents, key = { it.id }) { event ->
                val status = eventStatus(event.date, event.startTime, event.endTime)
                val isCompleted = completedIds.contains(event.id)
                MetroEventRow(
                    event = event, 
                    status = status, 
                    isCompleted = isCompleted,
                    onToggle = { onToggleCompletion(event.id, !isCompleted) },
                    onClick = { onEventClick(event) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun MetroEventRow(
    event: EventItem, 
    status: EventStatus,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTimetableEvent = event.id < 0
    val hasDetails = event.location.isNotBlank() || event.notes.isNotBlank()
    val canMarkDone = status == EventStatus.PAST || isTimetableEvent

    val accentColor = if (isTimetableEvent) Theme.Teal else Theme.Blue

    val dotColor by animateColorAsState(
        targetValue = when {
            isCompleted -> Theme.Teal
            status == EventStatus.PAST     -> Theme.White30
            status == EventStatus.ACTIVE   -> accentColor
            else -> accentColor.copy(alpha = 0.55f)
        },
        label = "dotColor"
    )
    
    val railColor  = Theme.White10
    val cardAlpha  by animateFloatAsState(if (status == EventStatus.PAST && !isCompleted) 0.55f else 1f, label = "cardAlpha")
    val titleDeco  = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
    val titleColor by animateColorAsState(if (isCompleted) Theme.White30 else Theme.White, label = "titleColor")
    
    val badgeBg = when {
        isCompleted -> Theme.Teal.copy(alpha = 0.18f)
        isTimetableEvent -> Theme.Teal.copy(alpha = 0.18f)
        status == EventStatus.PAST -> Theme.White06
        status == EventStatus.ACTIVE -> Theme.Blue.copy(alpha = 0.18f)
        else -> Theme.White06
    }
    val badgeText = when {
        isCompleted -> "Done"
        isTimetableEvent -> "Timetable"
        status == EventStatus.PAST -> "Past"
        status == EventStatus.ACTIVE -> "Now"
        else -> ""
    }
    val badgeColor = when {
        isCompleted -> Theme.Teal
        isTimetableEvent -> Theme.Teal
        status == EventStatus.PAST -> Theme.White30
        status == EventStatus.ACTIVE -> Theme.Blue
        else -> Theme.White30
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .alpha(cardAlpha)
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
        ) {
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(railColor)
            )

            Box(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .size(if (status == EventStatus.ACTIVE) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            ) {
                if (status == EventStatus.ACTIVE && !isCompleted) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Theme.BgDeep)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 2.dp, bottom = 6.dp, top = 10.dp)
        ) {
            Text(
                text = "${event.startTime} – ${event.endTime}",
                color = if (status == EventStatus.ACTIVE || isTimetableEvent) accentColor else Theme.White30,
                fontSize = 11.sp,
                fontWeight = if (status == EventStatus.ACTIVE || isTimetableEvent) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
            )

            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
                color = Theme.CardBg,
                border = if (isTimetableEvent) androidx.compose.foundation.BorderStroke(1.dp, Theme.Teal.copy(alpha = 0.25f)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = if (status == EventStatus.ACTIVE)
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.08f))
                    else Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCompleted) Theme.Teal.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.12f))
                                    .clickable(enabled = canMarkDone) { onToggle() }
                            ) {
                                AnimatedContent(isCompleted, label = "iconAnim") { completed ->
                                    Icon(
                                        imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.Event,
                                        contentDescription = null,
                                        tint = if (completed) Theme.Teal else dotColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    event.title,
                                    color = titleColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = titleDeco,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (event.location.isNotBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn, null,
                                            tint = accentColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            event.location,
                                            color = Theme.White60,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            if (badgeText.isNotEmpty()) {
                                AnimatedContent(badgeText, label = "badgeText") { text ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = badgeBg
                                    ) {
                                        Text(
                                            text,
                                            color = badgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            if (hasDetails) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.ChevronRight, null,
                                    tint = Theme.White30,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
