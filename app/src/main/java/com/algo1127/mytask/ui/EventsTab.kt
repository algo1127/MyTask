package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ─── Status enum ─────────────────────────────────────────────────────────────

private enum class EventStatus { PAST, ACTIVE, UPCOMING }

private fun eventStatus(startTime: String, endTime: String): EventStatus {
    val now   = LocalTime.now()
    val start = runCatching { LocalTime.parse(startTime) }.getOrNull() ?: return EventStatus.UPCOMING
    val end   = runCatching { LocalTime.parse(endTime)   }.getOrNull() ?: return EventStatus.UPCOMING
    return when {
        now.isAfter(end)                          -> EventStatus.PAST
        now.isAfter(start) || now == start        -> EventStatus.ACTIVE
        else                                      -> EventStatus.UPCOMING
    }
}

// ─── Main composable ──────────────────────────────────────────────────────────

@Composable
fun EventsTab(events: List<EventItem>, selectedDate: LocalDate) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val sorted = remember(events, selectedDate) {
        events
            .filter { it.date == selectedDate }
            .sortedWith(
                compareBy(
                    { runCatching { LocalTime.parse(it.startTime) }.getOrElse { LocalTime.MAX } },
                    { runCatching { LocalTime.parse(it.endTime)   }.getOrElse { LocalTime.MAX } }
                )
            )
    }

    if (sorted.isEmpty()) {
        EmptyState(
            icon     = Icons.Outlined.Event,
            title    = "No events",
            subtitle = "Tap + to add an event",
            color    = Theme.Blue
        )
        return
    }

    // Refresh current time every 30 s so ACTIVE status updates without restart
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = LocalTime.now()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(sorted, key = { it.id }) { event ->
            val status = eventStatus(event.startTime, event.endTime)
            MetroEventRow(event = event, status = status)
        }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

// ─── Single metro row ─────────────────────────────────────────────────────────

@Composable
private fun MetroEventRow(event: EventItem, status: EventStatus) {
    var expanded by remember { mutableStateOf(false) }
    val hasDetails = event.location.isNotBlank() || event.notes.isNotBlank()

    val dotColor = when (status) {
        EventStatus.PAST     -> Theme.White30
        EventStatus.ACTIVE   -> Theme.Blue
        EventStatus.UPCOMING -> Theme.Blue.copy(alpha = 0.55f)
    }
    val railColor  = Theme.White10
    val cardAlpha  = if (status == EventStatus.PAST) 0.45f else 1f
    val titleDeco  = if (status == EventStatus.PAST) TextDecoration.LineThrough else TextDecoration.None
    val titleColor = if (status == EventStatus.PAST) Theme.White30 else Theme.White
    val badgeBg    = when (status) {
        EventStatus.PAST     -> Theme.White06
        EventStatus.ACTIVE   -> Theme.Blue.copy(alpha = 0.18f)
        EventStatus.UPCOMING -> Theme.White06
    }
    val badgeText  = when (status) {
        EventStatus.PAST     -> "Done"
        EventStatus.ACTIVE   -> "Now"
        EventStatus.UPCOMING -> ""
    }
    val badgeColor = when (status) {
        EventStatus.PAST     -> Theme.White30
        EventStatus.ACTIVE   -> Theme.Blue
        EventStatus.UPCOMING -> Theme.White30
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // ← add this
            .alpha(cardAlpha)
    ) {
        // ── Rail + dot column ──────────────────────────────────────────
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
        ) {
            // Continuous rail line spanning the full row height
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(railColor)
            )

            // Station dot aligned with the time label
            Box(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .size(if (status == EventStatus.ACTIVE) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            ) {
                if (status == EventStatus.ACTIVE) {
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

        // ── Time + card column ────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 2.dp, bottom = 6.dp, top = 10.dp)
        ) {
            Text(
                text = "${event.startTime} – ${event.endTime}",
                color = if (status == EventStatus.ACTIVE) Theme.Blue else Theme.White30,
                fontSize = 11.sp,
                fontWeight = if (status == EventStatus.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
            )

            Surface(
                onClick = { if (hasDetails) expanded = !expanded },
                shape = RoundedCornerShape(16.dp),
                color = Theme.CardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = if (status == EventStatus.ACTIVE)
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Theme.Blue.copy(alpha = 0.08f))
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
                                    .background(Theme.Blue.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    Icons.Default.Event, null,
                                    tint = dotColor,
                                    modifier = Modifier.size(18.dp)
                                )
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
                                            tint = Theme.Blue.copy(alpha = 0.6f),
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
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = badgeBg
                                ) {
                                    Text(
                                        badgeText,
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (hasDetails) {
                                Spacer(Modifier.width(6.dp))
                                val rotation by animateFloatAsState(
                                    targetValue = if (expanded) 180f else 0f,
                                    animationSpec = tween(200),
                                    label = "chevron"
                                )
                                Icon(
                                    Icons.Default.ExpandMore, null,
                                    tint = Theme.White30,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = rotation }
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                            exit  = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column {
                                if (event.notes.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = Theme.White10, thickness = 0.5.dp)
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Notes, null,
                                            tint = Theme.White30,
                                            modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                        )
                                        Text(
                                            event.notes,
                                            color = Theme.White60,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
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