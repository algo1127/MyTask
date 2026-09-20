package com.algo1127.mytask.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.NotifAi.NotifAi
import com.algo1127.mytask.ui.*
import com.algo1127.mytask.ui.models.TimePreference
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    defaultDate: LocalDate,
    sourceTab: Int = 1,          // 0 = Reminders (teal), 1 = Tasks (purple)
    taskToEdit: com.algo1127.mytask.ui.models.Task? = null,
    taskItemToEdit: TaskItem? = null,
    subtaskToEdit: com.algo1127.mytask.ui.models.Subtask? = null,
    categories: List<TaskCategory> = TaskCategory.values(),
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        timePreference: TimePreference,
        category: TaskCategory,
        date: LocalDate,
        description: String?,
        repetition: RepetitionInfo?,
        dueDate: LocalDate?,
        estimatedDuration: java.time.Duration?,
        reminderDateTime: LocalDateTime?
    ) -> Unit,
    onEdit: ((com.algo1127.mytask.ui.models.Task) -> Unit)? = null,
    onEditItem: ((TaskItem) -> Unit)? = null,
    onEditSubtask: ((com.algo1127.mytask.ui.models.Subtask) -> Unit)? = null
) {
    val accentColor  = if (sourceTab == 0) Theme.Teal else Theme.Purple
    val isEditMode   = taskToEdit != null || taskItemToEdit != null || subtaskToEdit != null
    val dialogTitle  = if (isEditMode) "Edit ${if (sourceTab == 0) "Reminder" else "Task"}" else if (sourceTab == 0) "Add Reminder" else "Add Task"

    val context = LocalContext.current
    val notifAi = remember { NotifAi(context) }

    var title            by remember { mutableStateOf(taskToEdit?.title ?: taskItemToEdit?.title ?: subtaskToEdit?.title ?: "") }
    var description      by remember { mutableStateOf(taskToEdit?.description ?: taskItemToEdit?.notes ?: subtaskToEdit?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(taskToEdit?.category ?: taskItemToEdit?.category ?: subtaskToEdit?.category ?: categories.firstOrNull() ?: TaskCategory.Study) }
    var selectedDate     by remember { mutableStateOf(taskToEdit?.startDate ?: taskItemToEdit?.date ?: subtaskToEdit?.dueDate ?: defaultDate) }
    
    var hasDueDate       by remember { mutableStateOf(taskToEdit?.dueDate != null || subtaskToEdit?.dueDate != null) }
    var dueDate          by remember { mutableStateOf(taskToEdit?.dueDate ?: subtaskToEdit?.dueDate ?: defaultDate) }
    var estimatedMinutes by remember { mutableStateOf(taskToEdit?.estimatedEffort?.toMinutes()?.toString() ?: subtaskToEdit?.estimatedEffort?.toMinutes()?.toString() ?: "") }

    // Reminder state
    var hasReminder      by remember { mutableStateOf(taskToEdit?.reminderDateTime != null || taskItemToEdit?.reminderDateTime != null || subtaskToEdit?.reminderDateTime != null) }
    var reminderDateTime by remember { mutableStateOf(taskToEdit?.reminderDateTime ?: taskItemToEdit?.reminderDateTime ?: subtaskToEdit?.reminderDateTime ?: LocalDateTime.now().plusHours(1)) }
    var showReminderOverlay by remember { mutableStateOf(false) }

    // Quick Category state
    var showQuickCatDialog by remember { mutableStateOf(false) }

    var timePreference   by remember { 
        mutableStateOf<TimePreference>(
            taskToEdit?.timePreference ?: subtaskToEdit?.timePreference ?: taskItemToEdit?.let { 
                try { TimePreference.Fixed(LocalTime.parse(it.time)) } catch(_: Exception) { TimePreference.LaterToday }
            } ?: TimePreference.LaterToday
        ) 
    }
    var selectedRepetition by remember { mutableStateOf<RepetitionInfo?>(null) }
    var fixedTime        by remember { mutableStateOf(taskItemToEdit?.time ?: "14:30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Theme.CardBg,
        shape            = RoundedCornerShape(22.dp),
        modifier = Modifier.padding(1.dp).background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.08f), Color.Transparent)), RoundedCornerShape(22.dp)).padding(1.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (sourceTab == 0) Icons.Outlined.Schedule else Icons.Default.Task, null, tint = accentColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(dialogTitle, color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title", color = Theme.White60) }, singleLine = true, colors = textFieldColors(accentColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)", color = Theme.White60) }, colors = textFieldColors(accentColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                Spacer(Modifier.height(16.dp))

                // ── Category ───────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        CategorySelector(selected = selectedCategory, onSelected = { selectedCategory = it }, accentColor = accentColor, categories = categories)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showQuickCatDialog = true }, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Theme.White06)) {
                        Icon(Icons.Default.Add, null, tint = accentColor)
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ── Reminders ─────────────────────────────────────────────
                Surface(
                    onClick = { showReminderOverlay = true },
                    shape = RoundedCornerShape(16.dp),
                    color = if (hasReminder) accentColor.copy(alpha = 0.1f) else Theme.White06,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = if (hasReminder) accentColor else Theme.White30, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Reminder", color = if (hasReminder) accentColor else Theme.White30, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (hasReminder) reminderDateTime.format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) else "Tap to set reminder",
                                color = Theme.White, fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ── Date (Reminders Tab Only) ──────────────────────────────
                if (sourceTab == 0) {
                    DateSelector(selectedDate = selectedDate, onDateSelected = { selectedDate = it }, accentColor = accentColor)
                    Spacer(Modifier.height(12.dp))
                }

                // ── Task Specifics ──────────────────
                if (sourceTab == 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasDueDate, onCheckedChange = { hasDueDate = it }, colors = CheckboxDefaults.colors(checkedColor = accentColor))
                        Text("Has Due Date", color = Theme.White60, fontSize = 13.sp)
                    }
                    if (hasDueDate) {
                        DateSelector(selectedDate = dueDate, onDateSelected = { dueDate = it }, accentColor = accentColor)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("Est. Duration", color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("15m" to "15", "30m" to "30", "1h" to "60", "2h" to "120", "4h" to "240").forEach { (label, value) ->
                            val isSel = estimatedMinutes == value
                            Surface(onClick = { estimatedMinutes = if (isSel) "" else value }, shape = RoundedCornerShape(10.dp), color = if (isSel) accentColor.copy(alpha = 0.2f) else Theme.White06, modifier = Modifier.weight(1f)) {
                                Text(label, color = if (isSel) accentColor else Theme.White60, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = estimatedMinutes, onValueChange = { if (it.all { c -> c.isDigit() }) estimatedMinutes = it }, label = { Text("Custom minutes", color = Theme.White30, fontSize = 11.sp) }, singleLine = true, colors = textFieldColors(accentColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), trailingIcon = { if (estimatedMinutes.isNotEmpty()) Text("min", color = accentColor, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp)) })
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = estimatedMinutes.toLongOrNull()?.let { java.time.Duration.ofMinutes(it) }
                    val finalReminder = if (hasReminder) reminderDateTime else null
                    if (taskToEdit != null && onEdit != null) {
                        onEdit(taskToEdit.copy(title = title.trim(), description = description, category = selectedCategory, dueDate = if (hasDueDate) dueDate else null, estimatedEffort = duration, reminderDateTime = finalReminder, timePreference = timePreference))
                    } else if (taskItemToEdit != null && onEditItem != null) {
                        onEditItem(taskItemToEdit.copy(title = title.trim(), time = fixedTime, category = selectedCategory, date = selectedDate, reminderDateTime = finalReminder))
                    } else if (subtaskToEdit != null && onEditSubtask != null) {
                        onEditSubtask(subtaskToEdit.copy(title = title.trim(), description = description, category = selectedCategory, dueDate = if (hasDueDate) dueDate else null, estimatedEffort = duration, reminderDateTime = finalReminder, timePreference = timePreference))
                    } else {
                        onAdd(title.trim(), timePreference, selectedCategory, selectedDate, description.ifBlank { null }, selectedRepetition, if (hasDueDate) dueDate else null, duration, finalReminder)
                    }
                    onDismiss()
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEditMode) "Save" else "Add", color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Theme.White60) }
        }
    )

    if (showQuickCatDialog) {
        QuickCategoryDialog(
            onDismiss = { showQuickCatDialog = false },
            onConfirm = { 
                selectedCategory = it
                showQuickCatDialog = false 
            }
        )
    }

    if (showReminderOverlay) {
        ReminderPickerOverlay(
            initialDateTime = reminderDateTime,
            onDismiss = { showReminderOverlay = false },
            onConfirm = { 
                reminderDateTime = it
                hasReminder = true
                showReminderOverlay = false 
            },
            onRemove = {
                hasReminder = false
                showReminderOverlay = false
            },
            accentColor = accentColor
        )
    }
}

@Composable
private fun textFieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = Theme.White10, focusedLabelColor = accentColor, unfocusedLabelColor = Theme.White30, cursorColor = accentColor, focusedTextColor = Theme.White, unfocusedTextColor = Theme.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(selected: TaskCategory, onSelected: (TaskCategory) -> Unit, accentColor: Color, categories: List<TaskCategory>) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selected.label, onValueChange = {}, readOnly = true, label = { Text("Category", color = Theme.White60) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, colors = textFieldColors(accentColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Theme.BgSurface, shape = RoundedCornerShape(14.dp)) {
            for (cat in categories) {
                DropdownMenuItem(text = { Text(cat.label, color = Theme.White80, fontSize = 14.sp) }, onClick = { onSelected(cat); expanded = false }, colors = MenuDefaults.itemColors(textColor = Theme.White80))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit, accentColor: Color) {
    var showQuickPicker by remember { mutableStateOf(false) }
    var showManualPicker by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Theme.White06).clickable { showQuickPicker = true }.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Icon(Icons.Default.CalendarToday, null, tint = Theme.White60, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp))
        Column { Text("Date", color = Theme.White30, fontSize = 11.sp); Text(selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")), color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
    }
    if (showQuickPicker) {
        AlertDialog(onDismissRequest = { showQuickPicker = false }, containerColor = Theme.CardBg, shape = RoundedCornerShape(18.dp), title = { Text("Select Date", color = Theme.White) }, text = { Column { Text("Quick select:", color = Theme.White60, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { val daysOptions = arrayOf(0, 1, 2, 7, 14); for (days in daysOptions) { val date = LocalDate.now().plusDays(days.toLong()); Surface(onClick = { onDateSelected(date); showQuickPicker = false }, shape = RoundedCornerShape(10.dp), color = if (date == selectedDate) accentColor.copy(alpha = 0.2f) else Theme.White06) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) { Text(date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()), color = Theme.White60, fontSize = 11.sp); Text(date.dayOfMonth.toString(), color = if (date == selectedDate) accentColor else Theme.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } } } }; Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showQuickPicker = false; showManualPicker = true }.padding(horizontal = 14.dp, vertical = 11.dp)) { Icon(Icons.Default.EditCalendar, null, tint = accentColor, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Pick a custom date…", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = accentColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) } } }, confirmButton = { TextButton(onClick = { showQuickPicker = false }) { Text("Close", color = accentColor) } })
    }
    if (showManualPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showManualPicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()) }; showManualPicker = false }) { Text("OK", color = accentColor, fontWeight = FontWeight.SemiBold) } }, dismissButton = { TextButton(onClick = { showManualPicker = false }) { Text("Cancel", color = Theme.White60) } }, colors = DatePickerDefaults.colors(containerColor = Theme.CardBg)) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Theme.CardBg, titleContentColor = Theme.White, headlineContentColor = accentColor, selectedDayContainerColor = accentColor))
        }
    }
}
