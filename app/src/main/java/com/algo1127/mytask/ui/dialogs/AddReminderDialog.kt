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
fun AddReminderDialog(
    defaultDate: LocalDate,
    taskToEdit: TaskItem? = null,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        timePreference: TimePreference,
        category: TaskCategory,
        date: LocalDate,
        description: String?,
        repetition: RepetitionInfo?
    ) -> Unit,
    onEdit: ((TaskItem) -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val accentColor = Theme.Teal
    val isEditMode  = taskToEdit != null
    val dialogTitle = if (isEditMode) "Edit Reminder" else "Add Reminder"

    val context = LocalContext.current
    val notifAi = remember { NotifAi(context) }

    var title            by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var selectedDate     by remember { mutableStateOf(taskToEdit?.date ?: defaultDate) }
    var timePreference   by remember { 
        mutableStateOf<TimePreference>(
            taskToEdit?.let { 
                try { TimePreference.Fixed(LocalTime.parse(it.time)) } catch(_: Exception) { TimePreference.LaterToday }
            } ?: TimePreference.LaterToday
        ) 
    }
    var selectedRepetition by remember { mutableStateOf<RepetitionInfo?>(null) }
    var fixedTime        by remember { mutableStateOf(taskToEdit?.time ?: "14:30") }
    var timeError        by remember { mutableStateOf<String?>(null) }
    var showBatchAdd     by remember { mutableStateOf(false) }
    val itemsToAdd       = remember { mutableStateListOf<QueuedReminderItem>() }

    val defaultCategory  = taskToEdit?.category ?: TaskCategory.Personal

    fun updateFixedTime(time: LocalTime) {
        val hStr = String.format(Locale.US, "%02d", time.hour)
        val mStr = String.format(Locale.US, "%02d", time.minute)
        fixedTime = "$hStr:$mStr"
        timeError = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Theme.CardBg,
        shape            = RoundedCornerShape(26.dp),
        modifier = Modifier
            .padding(1.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)
                ),
                RoundedCornerShape(26.dp)
            )
            .padding(1.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = accentColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = dialogTitle,
                    color = Theme.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Title ──────────────────────────────────────────────────
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("What do you want to be reminded of?", color = Theme.White60) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(16.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = accentColor,
                        unfocusedBorderColor = Theme.White10,
                        focusedLabelColor    = accentColor,
                        unfocusedLabelColor  = Theme.White30,
                        cursorColor          = accentColor,
                        focusedTextColor     = Theme.White,
                        unfocusedTextColor   = Theme.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Date ───────────────────────────────────────────────────
                ReminderDateSelector(
                    selectedDate   = selectedDate,
                    onDateSelected = { selectedDate = it },
                    accentColor    = accentColor
                )

                // ── Time Preference ("When?") ──────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "When?",
                        color      = Theme.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    ReminderTimePreferenceSelector(
                        selected         = timePreference,
                        onSelected       = { 
                            timePreference = it
                            when (it) {
                                TimePreference.LaterToday -> {
                                    selectedDate = LocalDate.now()
                                    updateFixedTime(notifAi.resolveLaterToday(defaultCategory))
                                }
                                TimePreference.Tomorrow -> {
                                    selectedDate = LocalDate.now().plusDays(1)
                                    updateFixedTime(notifAi.resolveTomorrow(defaultCategory))
                                }
                                is TimePreference.Fixed -> {
                                    updateFixedTime(it.time)
                                }
                                else -> {}
                            }
                        },
                        fixedTime        = fixedTime,
                        onTimeChanged    = {
                            fixedTime = it
                            timeError = validateTime(it)
                            try {
                                timePreference = TimePreference.Fixed(LocalTime.parse(it))
                            } catch (_: Exception) {}
                        },
                        accentColor      = accentColor,
                        selectedCategory = defaultCategory,
                        notifAi          = notifAi,
                        onDateUpdate     = { selectedDate = it }
                    )
                }

                // ── Repetition ─────────────────────────────────────────────
                if (!isEditMode) {
                    RepeatingSelector(
                        onRepetitionChanged = { selectedRepetition = it },
                        accentColor = accentColor
                    )
                }

                // ── Batch Toggle & Queue ───────────────────────────────────
                if (!isEditMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clickable { showBatchAdd = !showBatchAdd }
                    ) {
                        Checkbox(
                            checked         = showBatchAdd,
                            onCheckedChange = { showBatchAdd = it },
                            colors          = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Text("Add multiple reminders", color = Theme.White60, fontSize = 13.sp)
                    }
                }

                AnimatedVisibility(visible = showBatchAdd && itemsToAdd.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Reminders to add:", color = Theme.White30, fontSize = 12.sp)
                        for (item in itemsToAdd) {
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${item.title}", color = Theme.White80, fontSize = 13.sp)
                                Text(item.timeLabel,    color = Theme.White30, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditMode && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBatchAdd && !isEditMode) {
                        TextButton(
                            onClick = {
                                if (title.isNotBlank() && (timePreference !is TimePreference.Fixed || timeError == null)) {
                                    itemsToAdd.add(
                                        QueuedReminderItem(
                                            title          = title.trim(),
                                            timePreference = timePreference,
                                            date           = selectedDate,
                                            timeLabel      = getTimeLabel(timePreference, fixedTime),
                                            repetition     = selectedRepetition
                                        )
                                    )
                                    title          = ""
                                    timePreference = TimePreference.LaterToday
                                }
                            },
                            enabled = title.isNotBlank()
                        ) {
                            Text("Add to Batch", color = accentColor, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TextButton(
                        onClick = {
                            if (isEditMode && taskToEdit != null && onEdit != null) {
                                onEdit(
                                    taskToEdit.copy(
                                        title = title.trim(),
                                        time = fixedTime,
                                        date = selectedDate
                                    )
                                )
                            } else if (showBatchAdd && itemsToAdd.isNotEmpty()) {
                                for (item in itemsToAdd) {
                                    onAdd(
                                        item.title,
                                        item.timePreference,
                                        defaultCategory,
                                        item.date,
                                        null,
                                        item.repetition
                                    )
                                }
                                itemsToAdd.clear()
                            } else if (title.isNotBlank() && (timePreference !is TimePreference.Fixed || timeError == null)) {
                                onAdd(
                                    title.trim(),
                                    timePreference,
                                    defaultCategory,
                                    selectedDate,
                                    null,
                                    selectedRepetition
                                )
                            }
                            onDismiss()
                        },
                        enabled = if (showBatchAdd) itemsToAdd.isNotEmpty()
                        else (title.isNotBlank() && (timePreference !is TimePreference.Fixed || timeError == null))
                    ) {
                        Text(
                            text = if (isEditMode) "Save" else if (showBatchAdd) "Add All" else "Add",
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Theme.White60)
                    }
                }
            }
        },
        dismissButton = {}
    )
}

// ─── Date Selector ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    accentColor: Color
) {
    var showQuickPicker  by remember { mutableStateOf(false) }
    var showManualPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { showQuickPicker = true },
        color   = Theme.White06,
        shape   = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, null, tint = Theme.White60, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Date", color = Theme.White30, fontSize = 11.sp)
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                    color      = Theme.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
        }
    }

    if (showQuickPicker) {
        AlertDialog(
            onDismissRequest = { showQuickPicker = false },
            containerColor   = Theme.CardBg,
            shape            = RoundedCornerShape(18.dp),
            title = { Text("Select Date", color = Theme.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Quick select:", color = Theme.White60, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val daysOptions = arrayOf(0, 1, 2, 7, 14)
                        for (days in daysOptions) {
                            val date = LocalDate.now().plusDays(days.toLong())
                            Surface(
                                onClick = { onDateSelected(date); showQuickPicker = false },
                                shape   = RoundedCornerShape(10.dp),
                                color   = if (date == selectedDate) accentColor.copy(alpha = 0.2f) else Theme.White06
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier            = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        date.dayOfWeek.getDisplayName(
                                            java.time.format.TextStyle.SHORT,
                                            Locale.getDefault()
                                        ),
                                        color    = Theme.White60,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        date.dayOfMonth.toString(),
                                        color      = if (date == selectedDate) accentColor else Theme.White,
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold
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
                            .clickable {
                                showQuickPicker  = false
                                showManualPicker = true
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Pick a custom date…", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = accentColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickPicker = false }) {
                    Text("Close", color = accentColor)
                }
            }
        )
    }

    if (showManualPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showManualPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    }
                    showManualPicker = false
                }) {
                    Text("OK", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualPicker = false }) {
                    Text("Cancel", color = Theme.White60)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Theme.CardBg)
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor              = Theme.CardBg,
                    titleContentColor           = Theme.White,
                    headlineContentColor        = accentColor,
                    weekdayContentColor         = Theme.White60,
                    subheadContentColor         = Theme.White60,
                    navigationContentColor      = Theme.White,
                    yearContentColor            = Theme.White,
                    currentYearContentColor     = accentColor,
                    selectedYearContentColor    = Theme.BgDeep,
                    selectedYearContainerColor  = accentColor,
                    dayContentColor             = Theme.White,
                    selectedDayContentColor     = Theme.BgDeep,
                    selectedDayContainerColor   = accentColor,
                    todayContentColor           = accentColor,
                    todayDateBorderColor        = accentColor
                )
            )
        }
    }
}

// ─── Time Preference Selector ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePreferenceSelector(
    selected:         TimePreference,
    onSelected:       (TimePreference) -> Unit,
    fixedTime:        String,
    onTimeChanged:    (String) -> Unit,
    accentColor:      Color,
    selectedCategory: TaskCategory,
    notifAi:          NotifAi,
    onDateUpdate:     (LocalDate) -> Unit
) {
    val parsedTime = try { LocalTime.parse(fixedTime) } catch (_: Exception) { LocalTime.of(14, 30) }
    val initHour = parsedTime.hour
    val initMinute = parsedTime.minute

    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val optionPrefs = arrayOf(
            TimePreference.Fixed(LocalTime.of(initHour, initMinute)),
            TimePreference.LaterToday,
            TimePreference.Tomorrow,
            TimePreference.AiDecide
        )
        val optionLabels = arrayOf("Fixed", "Later", "Tomorrow", "AI")
        
        for (i in 0..3) {
            val pref = optionPrefs[i]
            val label = optionLabels[i]
            val isSelected = when (pref) {
                is TimePreference.Fixed -> selected is TimePreference.Fixed
                TimePreference.LaterToday -> selected is TimePreference.LaterToday
                TimePreference.Tomorrow -> selected is TimePreference.Tomorrow
                TimePreference.AiDecide -> selected is TimePreference.AiDecide
                else -> false
            }
            Surface(
                onClick = {
                    when (pref) {
                        is TimePreference.AiDecide -> {
                            val suggested = notifAi.suggestTime(selectedCategory)
                            onSelected(suggested)
                            val hVal = suggested.time.hour
                            val mVal = suggested.time.minute
                            val hStr = String.format(Locale.US, "%02d", hVal)
                            val mStr = String.format(Locale.US, "%02d", mVal)
                            onTimeChanged("$hStr:$mStr")
                        }
                        TimePreference.LaterToday -> {
                            onDateUpdate(LocalDate.now())
                            onSelected(pref)
                        }
                        TimePreference.Tomorrow -> {
                            onDateUpdate(LocalDate.now().plusDays(1))
                            onSelected(pref)
                        }
                        else -> {
                            onSelected(pref)
                        }
                    }
                },
                shape    = RoundedCornerShape(12.dp),
                color    = if (isSelected) accentColor.copy(alpha = 0.2f) else Theme.White06,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    label,
                    color      = if (isSelected) accentColor else Theme.White60,
                    fontSize   = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier   = Modifier.padding(vertical = 10.dp),
                    textAlign  = TextAlign.Center
                )
            }
        }
    }

    if (selected is TimePreference.Fixed) {
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = { showTimePicker = true },
            color   = Theme.White06,
            shape   = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Outlined.Schedule, null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                val hStr = if (initHour < 10) "0$initHour" else initHour.toString()
                val mStr = if (initMinute < 10) "0$initMinute" else initMinute.toString()
                Text("$hStr:$mStr", color = Theme.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("tap to change", color = Theme.White30, fontSize = 11.sp)
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour   = initHour,
            initialMinute = initMinute,
            is24Hour      = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor   = Theme.CardBg,
            shape            = RoundedCornerShape(18.dp),
            title = { Text("Select Time", color = Theme.White, fontWeight = FontWeight.SemiBold) },
            text = {
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state  = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor                      = Theme.White06,
                            clockDialSelectedContentColor       = Theme.BgDeep,
                            clockDialUnselectedContentColor     = Theme.White,
                            selectorColor                       = accentColor,
                            containerColor                      = Theme.CardBg,
                            periodSelectorBorderColor           = accentColor,
                            timeSelectorSelectedContainerColor  = accentColor.copy(alpha = 0.2f),
                            timeSelectorUnselectedContainerColor= Theme.White06,
                            timeSelectorSelectedContentColor    = accentColor,
                            timeSelectorUnselectedContentColor  = Theme.White60
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val hVal = timePickerState.hour
                    val mVal = timePickerState.minute
                    val hStr = if (hVal < 10) "0$hVal" else hVal.toString()
                    val mStr = if (mVal < 10) "0$mVal" else mVal.toString()
                    onTimeChanged("$hStr:$mStr")
                    onSelected(TimePreference.Fixed(LocalTime.of(hVal, mVal)))
                    showTimePicker = false
                }) {
                    Text("OK", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = Theme.White60)
                }
            }
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun getTimeLabel(pref: TimePreference, fixedTime: String): String = when (pref) {
    is TimePreference.Fixed    -> fixedTime
    TimePreference.LaterToday  -> "Later today"
    TimePreference.Tomorrow    -> "Tomorrow"
    TimePreference.AiDecide    -> "AI decides"
    is TimePreference.Window   -> "${pref.startHour}:00-${pref.endHour}:00"
}

private fun validateTime(time: String): String? {
    return try {
        LocalTime.parse(time)
        null
    } catch (_: Exception) { "Invalid" }
}

private data class QueuedReminderItem(
    val title:          String,
    val timePreference: TimePreference,
    val date:           LocalDate,
    val timeLabel:      String,
    val repetition:     RepetitionInfo? = null
)
