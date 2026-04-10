package com.algo1127.mytask.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.algo1127.mytask.ui.*
import com.algo1127.mytask.ui.models.TimePreference
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    defaultDate: LocalDate,
    sourceTab: Int = 1, // 0 = Reminders (teal), 1 = Tasks (purple)
    onDismiss: () -> Unit,
    onAdd: (title: String, timePreference: TimePreference, category: TaskCategory, date: LocalDate, description: String?) -> Unit
) {
    // Derive accent color, title and icon from sourceTab
    val accentColor = if (sourceTab == 0) Theme.Teal else Theme.Purple
    val dialogTitle = if (sourceTab == 0) "Add Reminder" else "Add Task"
    val dialogIcon: ImageVector = if (sourceTab == 0) Icons.Outlined.Schedule else Icons.Default.Task

    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.Study) }
    var selectedDate by remember { mutableStateOf(defaultDate) }

    var timePreference by remember { mutableStateOf<TimePreference>(TimePreference.LaterToday) }
    var fixedTime by remember { mutableStateOf("14:30") }
    var timeError by remember { mutableStateOf<String?>(null) }

    var showBatchAdd by remember { mutableStateOf(false) }
    val itemsToAdd = remember { mutableStateListOf<QueuedItem>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .padding(1.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(accentColor.copy(alpha = 0.08f), Color.Transparent)
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(1.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(dialogIcon, null, tint = accentColor, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(dialogTitle, color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    label = { Text("Title", color = Theme.White60) },
                    singleLine = true,
                    colors = textFieldColors(accentColor),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)", color = Theme.White60) },
                    colors = textFieldColors(accentColor),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Category
                CategorySelector(selected = selectedCategory, onSelected = { selectedCategory = it }, accentColor = accentColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Date Selector
                DateSelector(selectedDate = selectedDate, onDateSelected = { selectedDate = it }, accentColor = accentColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Time Preference
                Text("When?", color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                TimePreferenceSelector(
                    selected = timePreference,
                    onSelected = { timePreference = it },
                    fixedTime = fixedTime,
                    onTimeChanged = {
                        fixedTime = it
                        timeError = validateTime(it)
                    },
                    timeError = timeError,
                    accentColor = accentColor
                )

                // Batch Toggle
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showBatchAdd = !showBatchAdd }
                ) {
                    Checkbox(
                        checked = showBatchAdd,
                        onCheckedChange = { showBatchAdd = it },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                    )
                    Text("Add multiple items", color = Theme.White60, fontSize = 13.sp)
                }

                // Batch Preview
                if (showBatchAdd && itemsToAdd.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Items to add:", color = Theme.White30, fontSize = 12.sp)
                    itemsToAdd.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${item.title}", color = Theme.White80, fontSize = 13.sp)
                            Text(item.timeLabel, color = Theme.White30, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (showBatchAdd) {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank() && (timePreference is TimePreference.Fixed || timeError == null)) {
                                itemsToAdd.add(
                                    QueuedItem(
                                        title = title.trim(),
                                        timePreference = timePreference,
                                        category = selectedCategory,
                                        date = selectedDate,
                                        description = description.ifBlank { null },
                                        timeLabel = getTimeLabel(timePreference, fixedTime)
                                    )
                                )
                                title = ""
                                description = ""
                                timePreference = TimePreference.LaterToday
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Add to Batch", color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                TextButton(
                    onClick = {
                        if (showBatchAdd && itemsToAdd.isNotEmpty()) {
                            itemsToAdd.forEach { item ->
                                onAdd(item.title, item.timePreference, item.category, item.date, item.description)
                            }
                            itemsToAdd.clear()
                        } else if (title.isNotBlank() && (timePreference is TimePreference.Fixed || timeError == null)) {
                            onAdd(title.trim(), timePreference, selectedCategory, selectedDate, description.ifBlank { null })
                        }
                        onDismiss()
                    },
                    enabled = if (showBatchAdd) itemsToAdd.isNotEmpty()
                    else title.isNotBlank() && (timePreference is TimePreference.Fixed || timeError == null)
                ) {
                    Text(if (showBatchAdd) "Add All" else "Add", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Theme.White60)
            }
        }
    )
}

// ==================== REUSABLE COMPONENTS ====================

@Composable
private fun textFieldColors(accentColor: androidx.compose.ui.graphics.Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accentColor,
    unfocusedBorderColor = Theme.White10,
    focusedLabelColor = accentColor,
    unfocusedLabelColor = Theme.White30,
    cursorColor = accentColor,
    focusedTextColor = Theme.White,
    unfocusedTextColor = Theme.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selected: TaskCategory,
    onSelected: (TaskCategory) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category", color = Theme.White60) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = textFieldColors(accentColor),
            modifier = Modifier.menuAnchor().fillMaxWidth().clip(RoundedCornerShape(14.dp))
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Theme.BgSurface,
            shape = RoundedCornerShape(14.dp)
        ) {
            TaskCategory.values().forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.label, color = Theme.White80, fontSize = 14.sp) },
                    onClick = {
                        onSelected(cat)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(textColor = Theme.White80)
                )
            }
        }
    }
}

@Composable
private fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Theme.White06)
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(Icons.Default.CalendarToday, null, tint = Theme.White60, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text("Date", color = Theme.White30, fontSize = 11.sp)
            Text(
                selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                color = Theme.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select Date", color = Theme.White) },
            text = {
                Column {
                    Text("Quick select:", color = Theme.White60, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 1, 2, 7, 14).forEach { days ->
                            val date = LocalDate.now().plusDays(days.toLong())
                            Surface(
                                onClick = {
                                    onDateSelected(date)
                                    showPicker = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (date == selectedDate) accentColor.copy(alpha = 0.2f) else Theme.White06
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()),
                                        color = Theme.White60, fontSize = 11.sp
                                    )
                                    Text(
                                        date.dayOfMonth.toString(),
                                        color = if (date == selectedDate) accentColor else Theme.White,
                                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Or pick manually (v2)", color = Theme.White30, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Close", color = accentColor)
                }
            },
            containerColor = Theme.CardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun TimePreferenceSelector(
    selected: TimePreference,
    onSelected: (TimePreference) -> Unit,
    fixedTime: String,
    onTimeChanged: (String) -> Unit,
    timeError: String?,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            TimePreference.Fixed(LocalTime.NOON) to "Fixed",
            TimePreference.LaterToday to "Later",
            TimePreference.Tomorrow to "Tomorrow",
            TimePreference.AiDecide to "AI"
        ).forEach { (pref, label) ->
            val isSelected = when {
                pref is TimePreference.Fixed && selected is TimePreference.Fixed -> true
                pref::class == selected::class -> true
                else -> false
            }

            Surface(
                onClick = { onSelected(pref) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.2f) else Theme.White06,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    label,
                    color = if (isSelected) accentColor else Theme.White60,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    if (selected is TimePreference.Fixed) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = fixedTime,
            onValueChange = onTimeChanged,
            label = { Text("Time (HH:mm)", color = Theme.White60) },
            singleLine = true,
            isError = timeError != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Theme.White10,
                focusedLabelColor = accentColor,
                unfocusedLabelColor = Theme.White30,
                cursorColor = accentColor,
                focusedTextColor = Theme.White,
                unfocusedTextColor = Theme.White,
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            ),
            supportingText = { if (timeError != null) Text(timeError, color = Color.Red, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        )
    }
}

// ==================== HELPERS ====================
private fun getTimeLabel(pref: TimePreference, fixedTime: String): String {
    return when (pref) {
        is TimePreference.Fixed -> fixedTime
        TimePreference.LaterToday -> "Later today"
        TimePreference.Tomorrow -> "Tomorrow"
        TimePreference.AiDecide -> "AI decides"
        is TimePreference.Window -> "${pref.startHour}:00-${pref.endHour}:00"
    }
}

private fun validateTime(time: String): String? {
    return try {
        val parts = time.split(":")
        if (parts.size != 2) return "Use HH:mm"
        val hours = parts[0].toIntOrNull() ?: return "Invalid hours"
        val minutes = parts[1].toIntOrNull() ?: return "Invalid minutes"
        if (hours !in 0..23) return "0-23"
        if (minutes !in 0..59) return "0-59"
        LocalTime.parse(time)
        null
    } catch (e: Exception) { "Invalid" }
}

private data class QueuedItem(
    val title: String,
    val timePreference: TimePreference,
    val category: TaskCategory,
    val date: LocalDate,
    val description: String?,
    val timeLabel: String
)