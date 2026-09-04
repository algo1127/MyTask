package com.algo1127.mytask.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.Theme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCountdownDialog(
    tasks: List<com.algo1127.mytask.ui.TaskItem>,
    events: List<com.algo1127.mytask.ui.EventItem>,
    notifAi: com.algo1127.mytask.NotifAi.NotifAi,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        target: LocalDateTime,
        color: Color,
        linkedId: Long?,
        linkedType: String?,
        optionalTitle: String?
    ) -> Unit
) {
    var wizardStep by remember { mutableIntStateOf(0) }
    
    var title by remember { mutableStateOf("") }
    var optionalTitle by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(12, 0)) }
    var selectedColor by remember { mutableStateOf(Theme.Teal) }
    
    var linkedId by remember { mutableStateOf<Long?>(null) }
    var linkedType by remember { mutableStateOf<String?>(null) }
    var linkedName by remember { mutableStateOf<String?>(null) }

    // AI Guidance State
    var aroundHour by remember { mutableStateOf<Int?>(null) }
    var betweenHours by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showHourPicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    val accentColor = selectedColor

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier
            .padding(1.dp)
            .background(
                Brush.linearGradient(listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)),
                RoundedCornerShape(26.dp)
            )
            .padding(1.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HourglassEmpty, null, tint = accentColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when(wizardStep) {
                        0 -> "Choose Type"
                        1 -> if (linkedId != null) "Linked Item" else "Custom Target"
                        else -> "Style & Details"
                    },
                    color = Theme.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (wizardStep) {
                    0 -> { // STEP 0: TYPE SELECTION
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TypeCard(
                                title = "Link to Task/Event",
                                subtitle = "Syncs automatically with your schedule",
                                icon = Icons.Default.Link,
                                color = Theme.Blue,
                                onClick = { wizardStep = 1; linkedType = "CHOOSE" }
                            )
                            TypeCard(
                                title = "Custom Timer",
                                subtitle = "Count down to any specific moment",
                                icon = Icons.Default.Add,
                                color = Theme.Teal,
                                onClick = { wizardStep = 1; linkedId = null; linkedType = null }
                            )
                        }
                    }
                    1 -> { // STEP 1: CONFIGURATION
                        if (linkedType == "CHOOSE") {
                            // Link Selection
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Select an item to link:", color = Theme.White60, fontSize = 13.sp)
                                Box(modifier = Modifier.height(200.dp)) {
                                    androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(tasks) { task ->
                                            LinkItemRow(task.title, "Task", Theme.Purple, task.category.icon) {
                                                linkedId = task.id
                                                linkedType = "TASK"
                                                linkedName = task.title
                                                selectedDate = task.date
                                                selectedTime = LocalTime.parse(task.time)
                                                wizardStep = 2
                                            }
                                        }
                                        items(events) { event ->
                                            LinkItemRow(event.title, "Event", Theme.Blue, Icons.Default.Event) {
                                                linkedId = event.id
                                                linkedType = "EVENT"
                                                linkedName = event.title
                                                selectedDate = event.date
                                                selectedTime = LocalTime.parse(event.startTime)
                                                wizardStep = 2
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Custom Date/Time
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("What's the occasion?", color = Theme.White60) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp), // Fixed bottom corners
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Theme.Orange,
                                        unfocusedBorderColor = Theme.White10,
                                        focusedTextColor = Theme.White,
                                        unfocusedTextColor = Theme.White,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                CountdownDateSelector(
                                    selectedDate = selectedDate,
                                    onDateSelected = { selectedDate = it },
                                    accentColor = Theme.Orange
                                )

                                CountdownTimeSelector(
                                    selectedTime = selectedTime,
                                    onTimeSelected = { selectedTime = it },
                                    accentColor = Theme.Orange
                                )
                                
                                var showAiGuidance by remember { mutableStateOf(false) }
                                
                                Button(
                                    onClick = { 
                                        val suggested = notifAi.suggestTime(com.algo1127.mytask.ui.TaskCategory.Personal)
                                        selectedTime = suggested.time
                                        selectedDate = LocalDate.now()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Theme.Orange.copy(alpha = 0.1f))
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Theme.Orange, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("AI Suggest", color = Theme.Orange, fontWeight = FontWeight.Bold)
                                }
                                
                                TextButton(
                                    onClick = { showAiGuidance = !showAiGuidance },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Guided Analytics", color = Theme.Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                if (showAiGuidance) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Guide the AI:", color = Theme.White30, fontSize = 11.sp)
                                        
                                        // Dynamic Guidance Options
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = aroundHour != null,
                                                onClick = { showHourPicker = true; betweenHours = null },
                                                label = { Text(aroundHour?.let { "Around $it:00" } ?: "Around...") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Theme.Orange.copy(alpha = 0.2f),
                                                    selectedLabelColor = Theme.Orange,
                                                    labelColor = Theme.White30
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = aroundHour != null, borderColor = Theme.White10, selectedBorderColor = Theme.Orange)
                                            )
                                            FilterChip(
                                                selected = betweenHours != null,
                                                onClick = { showRangePicker = true; aroundHour = null },
                                                label = { Text(betweenHours?.let { "${it.first}-${it.second}" } ?: "Between...") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Theme.Orange.copy(alpha = 0.2f),
                                                    selectedLabelColor = Theme.Orange,
                                                    labelColor = Theme.White30
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = betweenHours != null, borderColor = Theme.White10, selectedBorderColor = Theme.Orange)
                                            )
                                        }

                                        if (aroundHour != null || betweenHours != null) {
                                            Button(
                                                onClick = { 
                                                    val suggested = notifAi.suggestTime(
                                                        com.algo1127.mytask.ui.TaskCategory.Personal, 
                                                        aroundHour = aroundHour,
                                                        betweenHours = betweenHours
                                                    )
                                                    selectedTime = suggested.time
                                                    selectedDate = LocalDate.now()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Theme.Orange.copy(alpha = 0.15f))
                                            ) {
                                                Text("Get Guided Suggestion", color = Theme.Orange, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // STEP 2: THEMING & FINAL
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (linkedId != null) {
                                Text("Title for this timer (optional):", color = Theme.White60, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = optionalTitle,
                                    onValueChange = { optionalTitle = it },
                                    placeholder = { Text(linkedName ?: "", color = Theme.White30) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Text("Theme Color", color = Theme.White60, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val colors = listOf(Theme.Teal, Theme.Blue, Theme.Purple, Theme.Gold, Theme.Rose, Theme.Emerald, Theme.Orange)
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selectedColor == color) 2.dp else 0.dp,
                                                color = Theme.White,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = color }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (wizardStep == 2) {
                TextButton(
                    onClick = {
                        val finalTitle = if (linkedId != null) (linkedName ?: "Timer") else title
                        onAdd(finalTitle, LocalDateTime.of(selectedDate, selectedTime), selectedColor, linkedId, linkedType, optionalTitle.ifBlank { null })
                        onDismiss()
                    },
                    enabled = (linkedId != null) || title.isNotBlank()
                ) {
                    Text("Start Timer", color = accentColor, fontWeight = FontWeight.Bold)
                }
            } else if (wizardStep == 1 && linkedType != "CHOOSE") {
                TextButton(
                    onClick = { wizardStep = 2 },
                    enabled = title.isNotBlank() || linkedId != null
                ) {
                    Text("Next", color = accentColor)
                }
            }
        },
        dismissButton = {
            if (wizardStep > 0) {
                TextButton(onClick = { wizardStep-- }) {
                    Text("Back", color = Theme.White60)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Theme.White60)
                }
            }
        }
    )

    // --- AI Guidance Dialogs ---

    if (showHourPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = 14,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showHourPicker = false },
            containerColor = Theme.CardBg,
            shape = RoundedCornerShape(22.dp),
            title = { Text("Pick Target Hour", color = Theme.White, fontWeight = FontWeight.Bold) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor = Theme.Orange,
                            timeSelectorSelectedContainerColor = Theme.Orange.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = Theme.Orange
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aroundHour = timePickerState.hour
                    showHourPicker = false 
                }) { Text("Set Hour", color = Theme.Orange, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showHourPicker = false }) { Text("Cancel", color = Theme.White60) }
            }
        )
    }

    if (showRangePicker) {
        var range by remember { mutableStateOf(9f..17f) }
        AlertDialog(
            onDismissRequest = { showRangePicker = false },
            containerColor = Theme.CardBg,
            shape = RoundedCornerShape(22.dp),
            title = { Text("Select Visibility Window", color = Theme.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "${range.start.toInt()}:00 — ${range.endInclusive.toInt()}:00",
                        color = Theme.Orange,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    RangeSlider(
                        value = range,
                        onValueChange = { 
                            // Prevent start == end and ensure at least 1 hour difference
                            if (it.endInclusive - it.start >= 1f) {
                                range = it 
                            }
                        },
                        valueRange = 0f..24f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = Theme.Orange,
                            activeTrackColor = Theme.Orange,
                            inactiveTrackColor = Theme.White10,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
                    
                    Text(
                        "The AI will search for the best moment between these hours.",
                        color = Theme.White30, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    betweenHours = range.start.toInt() to range.endInclusive.toInt()
                    showRangePicker = false 
                }) { Text("Confirm Window", color = Theme.Orange, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("Cancel", color = Theme.White60) }
            }
        )
    }
}

// ─── Date Selector ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownDateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    accentColor: Color
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { showPicker = true },
        color = Theme.White06,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, null, tint = Theme.White60, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Date", color = Theme.White30, fontSize = 11.sp)
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                    color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK", color = accentColor) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel", color = Theme.White60) }
            },
            colors = DatePickerDefaults.colors(containerColor = Theme.CardBg)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(
                containerColor = Theme.CardBg,
                titleContentColor = Theme.White,
                headlineContentColor = accentColor,
                selectedDayContainerColor = accentColor,
                selectedDayContentColor = Theme.BgDeep,
                todayContentColor = accentColor,
                todayDateBorderColor = accentColor
            ))
        }
    }
}

// ─── Time Selector ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownTimeSelector(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    accentColor: Color
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { showPicker = true },
        color = Theme.White06,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Schedule, null, tint = Theme.White60, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Time", color = Theme.White30, fontSize = 11.sp)
                Text(
                    selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor = Theme.CardBg,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Select Time", color = Theme.White) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(
                        selectorColor = accentColor,
                        timeSelectorSelectedContainerColor = accentColor.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = accentColor
                    ))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showPicker = false
                }) { Text("OK", color = accentColor) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel", color = Theme.White60) }
            }
        )
    }
}

@Composable
private fun TypeCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Theme.White06,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Theme.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Theme.White30, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LinkItemRow(title: String, type: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Theme.White, fontSize = 14.sp)
                Text(type, color = Theme.White30, fontSize = 11.sp)
            }
        }
    }
}
