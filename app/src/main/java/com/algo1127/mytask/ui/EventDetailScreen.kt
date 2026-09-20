package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.max

@Composable
fun EventDetailScreen(
    event: EventItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (EventItem) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    Box(modifier = Modifier.fillMaxSize().background(Theme.BgDeep)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scrollState).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Theme.White06)) { Icon(Icons.Default.Close, null, tint = Theme.White) }
                IconButton(onClick = onDelete, modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(text = event.title, color = Theme.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start, lineHeight = 40.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(32.dp))

            // Reminder Section
            var showReminderPicker by remember { mutableStateOf(false) }
            Surface(color = if (event.reminderDateTime != null) Theme.Teal.copy(alpha = 0.1f) else Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().clickable { showReminderPicker = true }) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = if (event.reminderDateTime != null) Theme.Teal else Theme.White30, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Reminder", color = Theme.White30, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = event.reminderDateTime?.format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) ?: "No reminder set", color = if (event.reminderDateTime != null) Theme.Teal else Theme.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (showReminderPicker) {
                com.algo1127.mytask.ui.dialogs.ReminderPickerOverlay(initialDateTime = event.reminderDateTime ?: LocalDateTime.now().plusHours(1), onDismiss = { showReminderPicker = false }, onConfirm = { onUpdate(event.copy(reminderDateTime = it)); showReminderPicker = false }, onRemove = { onUpdate(event.copy(reminderDateTime = null)); showReminderPicker = false }, accentColor = Theme.Blue)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(color = Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailItem(icon = Icons.Default.Event, label = "Date", value = event.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")))
                    DetailItem(icon = Icons.Default.Schedule, label = "Time", value = "${event.startTime} - ${event.endTime}")
                }
            }
            if (event.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(color = Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { DetailItem(icon = Icons.Default.LocationOn, label = "Location", value = event.location, modifier = Modifier.padding(20.dp)) }
            }
            if (event.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = Theme.White03, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { DetailItem(icon = Icons.AutoMirrored.Filled.Notes, label = "Notes", value = event.notes, modifier = Modifier.padding(20.dp)) }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Theme.Blue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Theme.Blue, modifier = Modifier.size(18.dp)) }
        Spacer(modifier = Modifier.width(16.dp))
        Column { Text(text = label, color = Theme.White30, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(2.dp)); Text(text = value, color = Theme.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}
