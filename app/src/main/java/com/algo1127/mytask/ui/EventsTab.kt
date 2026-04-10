package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun EventsTab(events: List<EventItem>, selectedDate: LocalDate) {
    val visibleEvents = events.filter { it.date == selectedDate }

    if (visibleEvents.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Event,
            title = "No events",
            subtitle = "Tap + to add an event",
            color = Theme.Blue
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visibleEvents, key = { it.id }) { event ->
                EventRow(event = event)
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun EventRow(event: EventItem) {
    // ... your existing EventRow code
}