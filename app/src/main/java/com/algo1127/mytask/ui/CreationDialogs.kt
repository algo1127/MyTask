
package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onAdd: (title: String, time: String, category: TaskCategory, date: LocalDate) -> Unit
) {
    val context = LocalContext.current // Moved to composable scope
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(TaskCategory.Design) }
    var date by remember { mutableStateOf(defaultDate) }
    var timeError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Reminder") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = {
                        time = it
                        timeError = validateTime(it)
                    },
                    label = { Text("Time (e.g. 14:30)") },
                    singleLine = true,
                    isError = timeError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    supportingText = { if (timeError != null) Text(timeError!!, color = Color.Red, fontSize = 12.sp) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .animateContentSize()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TaskCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    timeError = validateTime(time)
                    if (timeError == null && title.isNotBlank()) {
                        val task = TaskItem(title.trim(), time.trim(), selectedCategory, date)
                        CalendarUtils.addTaskToCalendar(context, task) // Use context from composable scope
                        onAdd(title.trim(), time.trim(), selectedCategory, date)
                    }
                },
                enabled = title.isNotBlank() && time.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onAdd: (eventItems: List<EventItem>, reminders: List<TaskItem>) -> Unit
) {
    val context = LocalContext.current // Moved to composable scope
    var step by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(defaultDate) }
    var isRepeating by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(Frequency.Daily) }
    var untilDate by remember { mutableStateOf(defaultDate.plusMonths(1)) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var addReminders by remember { mutableStateOf(false) }
    var reminder1Title by remember { mutableStateOf("") }
    var reminder1Time by remember { mutableStateOf("") }
    var reminder1Category by remember { mutableStateOf(TaskCategory.Personal) }
    var reminder1Expanded by remember { mutableStateOf(false) }
    var reminder2Title by remember { mutableStateOf("") }
    var reminder2Time by remember { mutableStateOf("") }
    var reminder2Category by remember { mutableStateOf(TaskCategory.Personal) }
    var reminder2Expanded by remember { mutableStateOf(false) }
    var startTimeError by remember { mutableStateOf<String?>(null) }
    var endTimeError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Event - Step $step/3") },
        text = {
            Column {
                when (step) {
                    1 -> {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Event Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = {
                                startTime = it
                                startTimeError = validateTime(it)
                            },
                            label = { Text("Start Time (e.g. 14:30)") },
                            singleLine = true,
                            isError = startTimeError != null,
                            supportingText = { if (startTimeError != null) Text(startTimeError!!, color = Color.Red, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = {
                                endTime = it
                                endTimeError = validateTime(it)
                            },
                            label = { Text("End Time (e.g. 15:30)") },
                            singleLine = true,
                            isError = endTimeError != null,
                            supportingText = { if (endTimeError != null) Text(endTimeError!!, color = Color.Red, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isRepeating, onCheckedChange = { isRepeating = it })
                            Text("Repeating Event")
                        }
                    }
                    2 -> {
                        if (isRepeating) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = frequency.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Frequency") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    Frequency.values().forEach { freq ->
                                        DropdownMenuItem(
                                            text = { Text(freq.name) },
                                            onClick = {
                                                frequency = freq
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = startDate.format(DateTimeFormatter.ISO_DATE),
                                onValueChange = { /* Use date picker in future */ },
                                label = { Text("Start Date") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = untilDate.format(DateTimeFormatter.ISO_DATE),
                                onValueChange = { /* Use date picker */ },
                                label = { Text("Until Date") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (frequency == Frequency.Weekly) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Select Days:")
                                DayOfWeek.values().forEach { day ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedDays.contains(day),
                                            onCheckedChange = {
                                                selectedDays = if (it) selectedDays + day else selectedDays - day
                                            }
                                        )
                                        Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                                    }
                                }
                            }
                        } else {
                            Text("No recurrence selected. Proceed to reminders.")
                        }
                    }
                    3 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = addReminders, onCheckedChange = { addReminders = it })
                            Text("Add Reminders")
                        }
                        AnimatedVisibility(visible = addReminders) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Reminder 1")
                                OutlinedTextField(
                                    value = reminder1Title,
                                    onValueChange = { reminder1Title = it },
                                    label = { Text("Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = reminder1Time,
                                    onValueChange = { reminder1Time = it },
                                    label = { Text("Time") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ExposedDropdownMenuBox(
                                    expanded = reminder1Expanded,
                                    onExpandedChange = { reminder1Expanded = !reminder1Expanded }
                                ) {
                                    OutlinedTextField(
                                        value = reminder1Category.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Category") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminder1Expanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = reminder1Expanded,
                                        onDismissRequest = { reminder1Expanded = false }
                                    ) {
                                        TaskCategory.values().forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.label) },
                                                onClick = {
                                                    reminder1Category = cat
                                                    reminder1Expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Reminder 2")
                                OutlinedTextField(
                                    value = reminder2Title,
                                    onValueChange = { reminder2Title = it },
                                    label = { Text("Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = reminder2Time,
                                    onValueChange = { reminder2Time = it },
                                    label = { Text("Time") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ExposedDropdownMenuBox(
                                    expanded = reminder2Expanded,
                                    onExpandedChange = { reminder2Expanded = !reminder2Expanded }
                                ) {
                                    OutlinedTextField(
                                        value = reminder2Category.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Category") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminder2Expanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = reminder2Expanded,
                                        onDismissRequest = { reminder2Expanded = false }
                                    ) {
                                        TaskCategory.values().forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.label) },
                                                onClick = {
                                                    reminder2Category = cat
                                                    reminder2Expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (step > 1) {
                    TextButton(onClick = { step-- }) { Text("Back") }
                }
                TextButton(
                    onClick = {
                        if (step < 3) {
                            startTimeError = validateTime(startTime)
                            endTimeError = validateTime(endTime)
                            if (startTimeError == null && endTimeError == null) {
                                step++
                            }
                        } else {
                            if (title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                                val eventItems = generateEvents(
                                    title.trim(),
                                    startTime.trim(),
                                    endTime.trim(),
                                    location.trim(),
                                    startDate,
                                    isRepeating,
                                    frequency,
                                    untilDate,
                                    selectedDays
                                )
                                val reminders = mutableListOf<TaskItem>()
                                if (addReminders) {
                                    if (reminder1Title.isNotBlank() && reminder1Time.isNotBlank() && validateTime(reminder1Time) == null) {
                                        reminders.add(TaskItem(reminder1Title.trim(), reminder1Time.trim(), reminder1Category, startDate))
                                    }
                                    if (reminder2Title.isNotBlank() && reminder2Time.isNotBlank() && validateTime(reminder2Time) == null) {
                                        reminders.add(TaskItem(reminder2Title.trim(), reminder2Time.trim(), reminder2Category, startDate))
                                    }
                                }
                                // Add to calendar using context from composable scope
                                eventItems.forEach { event ->
                                    val rrule = if (isRepeating && event == eventItems.first()) {
                                        CalendarUtils.generateRRule(frequency, untilDate, selectedDays)
                                    } else null
                                    CalendarUtils.addEventToCalendar(context, event, rrule)
                                }
                                reminders.forEach { task ->
                                    CalendarUtils.addTaskToCalendar(context, task)
                                }
                                onAdd(eventItems, reminders)
                            }
                        }
                    },
                    enabled = when (step) {
                        1 -> title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank() && startTimeError == null && endTimeError == null
                        2 -> true
                        3 -> title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()
                        else -> false
                    }
                ) {
                    Text(if (step < 3) "Next" else "Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun validateTime(time: String): String? {
    return try {
        val parts = time.split(":")
        if (parts.size != 2) return "Use HH:mm format"
        val hours = parts[0].toIntOrNull() ?: return "Invalid hours"
        val minutes = parts[1].toIntOrNull() ?: return "Invalid minutes"
        if (hours !in 0..23) return "Hours must be 0-23"
        if (minutes !in 0..59) return "Minutes must be 0-59"
        LocalTime.parse(time)
        null
    } catch (e: Exception) {
        "Invalid time format"
    }
}

private fun generateEvents(
    title: String,
    startTime: String,
    endTime: String,
    location: String,
    startDate: LocalDate,
    isRepeating: Boolean,
    frequency: Frequency,
    untilDate: LocalDate,
    selectedDays: Set<DayOfWeek>
): List<EventItem> {
    val events = mutableListOf<EventItem>()
    var currentDate = startDate
    while (!currentDate.isAfter(untilDate)) {
        if (!isRepeating ||
            (frequency == Frequency.Daily) ||
            (frequency == Frequency.Weekly && selectedDays.contains(currentDate.dayOfWeek)) ||
            (frequency == Frequency.Monthly && currentDate.dayOfMonth == startDate.dayOfMonth)
        ) {
            events.add(EventItem(title, startTime, endTime, location, currentDate))
        }
        currentDate = when (frequency) {
            Frequency.Daily -> currentDate.plusDays(1)
            Frequency.Weekly -> currentDate.plusDays(7)
            Frequency.Monthly -> currentDate.plusMonths(1)
        }
    }
    return events
}

enum class Frequency {
    Daily, Weekly, Monthly
}
