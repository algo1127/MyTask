package com.algo1127.mytask.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.Theme
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        date: LocalDate,
        startTime: String,
        endTime: String,
        location: String,
        notes: String
    ) -> Unit
) {
    var title    by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes    by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(defaultDate) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime   by remember { mutableStateOf("10:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .padding(1.dp)
            .background(
                Brush.linearGradient(listOf(Theme.Blue.copy(alpha = 0.08f), Color.Transparent)),
                RoundedCornerShape(22.dp)
            )
            .padding(1.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Event, null, tint = Theme.Blue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Event", color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event title", color = Theme.White60) },
                    singleLine = true,
                    colors = eventFieldColors(),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(12.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (optional)", color = Theme.White60) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
                    },
                    colors = eventFieldColors(),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", color = Theme.White60) },
                    colors = eventFieldColors(),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(Modifier.height(12.dp))

                // Date selector (reuses the same pattern as AddTaskDialog)
                EventDateSelector(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
                Spacer(Modifier.height(12.dp))

                // Start / End time row
                Text("Time", color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EventTimePicker(
                        label = "Start",
                        time = startTime,
                        onTimeChanged = { startTime = it },
                        accentColor = Theme.Blue,
                        modifier = Modifier.weight(1f)
                    )
                    EventTimePicker(
                        label = "End",
                        time = endTime,
                        onTimeChanged = { endTime = it },
                        accentColor = Theme.Blue,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Warn if end is before start
                val timeWarning = remember(startTime, endTime) {
                    val s = parseTimeOrNull(startTime)
                    val e = parseTimeOrNull(endTime)
                    if (s != null && e != null && !e.isAfter(s)) "End time must be after start" else null
                }
                if (timeWarning != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(timeWarning, color = Color(0xFFFF6B6B), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            title.trim(),
                            selectedDate,
                            startTime,
                            endTime,
                            location.trim(),
                            notes.trim()
                        )
                    }
                    onDismiss()
                },
                enabled = title.isNotBlank() &&
                        parseTimeOrNull(startTime) != null &&
                        parseTimeOrNull(endTime) != null &&
                        parseTimeOrNull(endTime)!!.isAfter(parseTimeOrNull(startTime)!!)
            ) {
                Text("Add", color = Theme.Blue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Theme.White60) }
        }
    )
}

// ─── Date Selector ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var showQuickPicker  by remember { mutableStateOf(false) }
    var showManualPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Theme.White06)
            .clickable { showQuickPicker = true }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(Icons.Default.CalendarToday, null, tint = Theme.White60, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Date", color = Theme.White30, fontSize = 11.sp)
            Text(
                selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy")),
                color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
    }

    if (showQuickPicker) {
        AlertDialog(
            onDismissRequest = { showQuickPicker = false },
            containerColor = Theme.CardBg,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Select Date", color = Theme.White) },
            text = {
                Column {
                    Text("Quick select:", color = Theme.White60, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 1, 2, 7, 14).forEach { days ->
                            val date = LocalDate.now().plusDays(days.toLong())
                            Surface(
                                onClick = { onDateSelected(date); showQuickPicker = false },
                                shape = RoundedCornerShape(10.dp),
                                color = if (date == selectedDate) Theme.Blue.copy(alpha = 0.2f) else Theme.White06
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        date.dayOfWeek.getDisplayName(
                                            java.time.format.TextStyle.SHORT,
                                            java.util.Locale.getDefault()
                                        ),
                                        color = Theme.White60, fontSize = 11.sp
                                    )
                                    Text(
                                        date.dayOfMonth.toString(),
                                        color = if (date == selectedDate) Theme.Blue else Theme.White,
                                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showQuickPicker = false; showManualPicker = true }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, null, tint = Theme.Blue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Pick a custom date…", color = Theme.Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = Theme.Blue.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickPicker = false }) { Text("Close", color = Theme.Blue) }
            }
        )
    }

    if (showManualPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showManualPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showManualPicker = false
                }) { Text("OK", color = Theme.Blue, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showManualPicker = false }) { Text("Cancel", color = Theme.White60) }
            },
            colors = DatePickerDefaults.colors(containerColor = Theme.CardBg)
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = Theme.CardBg,
                    titleContentColor = Theme.White,
                    headlineContentColor = Theme.Blue,
                    weekdayContentColor = Theme.White60,
                    subheadContentColor = Theme.White60,
                    navigationContentColor = Theme.White,
                    yearContentColor = Theme.White,
                    currentYearContentColor = Theme.Blue,
                    selectedYearContentColor = Theme.BgDeep,
                    selectedYearContainerColor = Theme.Blue,
                    dayContentColor = Theme.White,
                    selectedDayContentColor = Theme.BgDeep,
                    selectedDayContainerColor = Theme.Blue,
                    todayContentColor = Theme.Blue,
                    todayDateBorderColor = Theme.Blue
                )
            )
        }
    }
}

// ─── Time Picker ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTimePicker(
    label: String,
    time: String,
    onTimeChanged: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val (initH, initM) = remember(time) {
        val p = time.split(":")
        (p.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9) to
                (p.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0)
    }
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(label, color = Theme.White30, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Theme.White06)
                .clickable { showPicker = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Outlined.Schedule, null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                String.format("%02d:%02d", initH, initM),
                color = Theme.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showPicker) {
        val state = rememberTimePickerState(initH, initM, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor = Theme.CardBg,
            shape = RoundedCornerShape(18.dp),
            title = { Text("$label time", color = Theme.White, fontWeight = FontWeight.SemiBold) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = state,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Theme.White06,
                            clockDialSelectedContentColor = Theme.BgDeep,
                            clockDialUnselectedContentColor = Theme.White,
                            selectorColor = accentColor,
                            containerColor = Theme.CardBg,
                            timeSelectorSelectedContainerColor = accentColor.copy(alpha = 0.2f),
                            timeSelectorUnselectedContainerColor = Theme.White06,
                            timeSelectorSelectedContentColor = accentColor,
                            timeSelectorUnselectedContentColor = Theme.White60
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChanged(String.format("%02d:%02d", state.hour, state.minute))
                    showPicker = false
                }) { Text("OK", color = accentColor, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel", color = Theme.White60) }
            }
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun parseTimeOrNull(t: String): LocalTime? = try { LocalTime.parse(t) } catch (_: Exception) { null }

@Composable
private fun eventFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Theme.Blue,
    unfocusedBorderColor = Theme.White10,
    focusedLabelColor = Theme.Blue,
    unfocusedLabelColor = Theme.White30,
    cursorColor = Theme.Blue,
    focusedTextColor = Theme.White,
    unfocusedTextColor = Theme.White
)