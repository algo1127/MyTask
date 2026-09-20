package com.algo1127.mytask.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.Theme
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPicker(dateTime: LocalDateTime, onDateTimeChanged: (LocalDateTime) -> Unit, accentColor: Color) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(onClick = { showDatePicker = true }, shape = RoundedCornerShape(12.dp), color = Theme.White06, modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(dateTime.format(DateTimeFormatter.ofPattern("MMM d")), color = Theme.White, fontSize = 14.sp)
            }
        }
        Surface(onClick = { showTimePicker = true }, shape = RoundedCornerShape(12.dp), color = Theme.White06, modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(dateTime.format(DateTimeFormatter.ofPattern("HH:mm")), color = Theme.White, fontSize = 14.sp)
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onDateTimeChanged(LocalDateTime.of(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(), dateTime.toLocalTime())) }; showDatePicker = false }) { Text("OK", color = accentColor) } }) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Theme.CardBg, titleContentColor = Theme.White, headlineContentColor = accentColor, selectedDayContainerColor = accentColor))
        }
    }
    if (showTimePicker) {
        val state = rememberTimePickerState(dateTime.hour, dateTime.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = { onDateTimeChanged(LocalDateTime.of(dateTime.toLocalDate(), LocalTime.of(state.hour, state.minute))); showTimePicker = false }) { Text("OK", color = accentColor) } }, text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state, colors = TimePickerDefaults.colors(clockDialColor = Theme.White06, selectorColor = accentColor)) } })
    }
}

@Composable
fun ReminderPickerOverlay(
    initialDateTime: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
    onRemove: () -> Unit,
    accentColor: Color = Theme.Teal
) {
    var currentDt by remember { mutableStateOf(initialDateTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Set Reminder", color = Theme.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select date and time for the notification.", color = Theme.White60, fontSize = 14.sp)
                ReminderPicker(dateTime = currentDt, onDateTimeChanged = { currentDt = it }, accentColor = accentColor)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentDt) }) {
                Text("Set Reminder", color = accentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRemove) {
                    Text("Remove", color = Color.Red.copy(alpha = 0.7f))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Theme.White60)
                }
            }
        }
    )
}
