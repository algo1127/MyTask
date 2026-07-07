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
import androidx.compose.ui.graphics.vector.ImageVector
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

// ─── Public entry point ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    defaultDate: LocalDate,
    sourceTab: Int = 1,          // 0 = Reminders (teal), 1 = Tasks (purple)
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        timePreference: TimePreference,
        category: TaskCategory,
        date: LocalDate,
        description: String?,
        repetition: RepetitionInfo?,
    ) -> Unit
) {
    val accentColor  = if (sourceTab == 0) Theme.Teal else Theme.Purple
    val dialogTitle  = if (sourceTab == 0) "Add Reminder" else "Add Task"
    val dialogIcon: ImageVector =
        if (sourceTab == 0) Icons.Outlined.Schedule else Icons.Default.Task

    val context = LocalContext.current
    val notifAi = remember { NotifAi(context) }

    var title            by remember { mutableStateOf("") }
    var description      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.Study) }
    var selectedDate     by remember { mutableStateOf(defaultDate) }
    var timePreference   by remember { mutableStateOf<TimePreference>(TimePreference.LaterToday) }
    var selectedRepetition by remember { mutableStateOf<RepetitionInfo?>(null) }
    var fixedTime        by remember { mutableStateOf("14:30") }
    var timeError        by remember { mutableStateOf<String?>(null) }
    var showBatchAdd     by remember { mutableStateOf(false) }
    val itemsToAdd       = remember { mutableStateListOf<QueuedItem>() }

    fun updateFixedTime(time: LocalTime) {
        val hStr = String.format(Locale.US, "%02d", time.hour)
        val mStr = String.format(Locale.US, "%02d", time.minute)
        fixedTime = "$hStr:$mStr"
        timeError = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Theme.CardBg,
        shape            = RoundedCornerShape(22.dp),
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
                Spacer(Modifier.width(8.dp))
                Text(
                    dialogTitle,
                    color      = Theme.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // ── Title ──────────────────────────────────────────────────
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Title", color = Theme.White60) },
                    singleLine    = true,
                    colors        = textFieldColors(accentColor),
                    modifier      = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(12.dp))

                // ── Description ────────────────────────────────────────────
                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Description (optional)", color = Theme.White60) },
                    colors        = textFieldColors(accentColor),
                    modifier      = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                    minLines      = 2,
                    maxLines      = 4
                )
                Spacer(Modifier.height(12.dp))

                // ── Category ───────────────────────────────────────────────
                CategorySelector(
                    selected    = selectedCategory,
                    onSelected  = { selectedCategory = it },
                    accentColor = accentColor
                )
                Spacer(Modifier.height(12.dp))

                // ── Date ───────────────────────────────────────────────────
                DateSelector(
                    selectedDate   = selectedDate,
                    onDateSelected = { selectedDate = it },
                    accentColor    = accentColor
                )
                Spacer(Modifier.height(12.dp))

                // ── Repetition ─────────────────────────────────────────────
                RepeatingSelector(
                    onRepetitionChanged = { selectedRepetition = it },
                    accentColor = accentColor
                )
                Spacer(Modifier.height(12.dp))

                // ── Time preference ────────────────────────────────────────
                Text(
                    "When?",
                    color      = Theme.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                TimePreferenceSelector(
                    selected         = timePreference,
                    onSelected       = { 
                        timePreference = it
                        when (it) {
                            TimePreference.LaterToday -> {
                                selectedDate = LocalDate.now()
                                updateFixedTime(notifAi.resolveLaterToday(selectedCategory))
                            }
                            TimePreference.Tomorrow -> {
                                selectedDate = LocalDate.now().plusDays(1)
                                updateFixedTime(notifAi.resolveTomorrow(selectedCategory))
                            }
                            is TimePreference.Fixed -> {
                                // Keep current date, update string
                                updateFixedTime(it.time)
                            }
                            else -> {}
                        }
                    },
                    fixedTime        = fixedTime,
                    onTimeChanged    = {
                        fixedTime  = it
                        timeError  = validateTime(it)
                        // Sync preference if manually changed
                        try {
                            timePreference = TimePreference.Fixed(LocalTime.parse(it))
                        } catch (_: Exception) {}
                    },
                    accentColor      = accentColor,
                    selectedCategory = selectedCategory,
                    notifAi          = notifAi,
                    onDateUpdate     = { selectedDate = it }
                )

                // ── Batch toggle ───────────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.clickable { showBatchAdd = !showBatchAdd }
                ) {
                    Checkbox(
                        checked         = showBatchAdd,
                        onCheckedChange = { showBatchAdd = it },
                        colors          = CheckboxDefaults.colors(checkedColor = accentColor)
                    )
                    Text("Add multiple items", color = Theme.White60, fontSize = 13.sp)
                }

                // ── Batch preview ──────────────────────────────────────────
                AnimatedVisibility(visible = showBatchAdd && itemsToAdd.isNotEmpty()) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Text("Items to add:", color = Theme.White30, fontSize = 12.sp)
                        for (item in itemsToAdd) {
                            Row(
                                modifier            = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
            Row {
                if (showBatchAdd) {
                    TextButton(
                        onClick = {
                            if (title != "" &&
                                (timePreference !is TimePreference.Fixed || (timeError == null))
                            ) {
                                itemsToAdd.add(
                                    QueuedItem(
                                        title          = title.trim(),
                                        timePreference = timePreference,
                                        category       = selectedCategory,
                                        date           = selectedDate,
                                        description    = description.ifBlank { null },
                                        timeLabel      = getTimeLabel(timePreference, fixedTime),
                                        repetition     = selectedRepetition
                                    )
                                )
                                title          = ""
                                description    = ""
                                timePreference = TimePreference.LaterToday
                            }
                        },
                        enabled = title != ""
                    ) {
                        Text(
                            "Add to Batch",
                            color      = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                TextButton(
                    onClick = {
                        if (showBatchAdd && itemsToAdd.isNotEmpty()) {
                            for (item in itemsToAdd) {
                                onAdd(
                                    item.title,
                                    item.timePreference,
                                    item.category,
                                    item.date,
                                    item.description,
                                    item.repetition
                                )
                            }
                            itemsToAdd.clear()
                        } else if (title != "" &&
                            (timePreference !is TimePreference.Fixed || timeError == null)
                        ) {
                            onAdd(
                                title.trim(),
                                timePreference,
                                selectedCategory,
                                selectedDate,
                                description.ifBlank { null },
                                selectedRepetition
                            )
                        }
                        onDismiss()
                    },
                    enabled = if (showBatchAdd) itemsToAdd.isNotEmpty()
                    else ((title != "") &&
                            (timePreference !is TimePreference.Fixed || (timeError == null)))
                ) {
                    Text(
                        if (showBatchAdd) "Add All" else "Add",
                        color      = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
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

// ─── Text field colours ───────────────────────────────────────────────────────

@Composable
private fun textFieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = accentColor,
    unfocusedBorderColor = Theme.White10,
    focusedLabelColor    = accentColor,
    unfocusedLabelColor  = Theme.White30,
    cursorColor          = accentColor,
    focusedTextColor     = Theme.White,
    unfocusedTextColor   = Theme.White
)

// ─── Category selector ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selected:    TaskCategory,
    onSelected:  (TaskCategory) -> Unit,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded        = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value         = selected.label,
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Category", color = Theme.White60) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors        = textFieldColors(accentColor),
            modifier      = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        )
        ExposedDropdownMenu(
            expanded          = expanded,
            onDismissRequest  = { expanded = false },
            containerColor    = Theme.BgSurface,
            shape             = RoundedCornerShape(14.dp)
        ) {
            for (cat in TaskCategory.values()) {
                DropdownMenuItem(
                    text    = { Text(cat.label, color = Theme.White80, fontSize = 14.sp) },
                    onClick = { onSelected(cat); expanded = false },
                    colors  = MenuDefaults.itemColors(textColor = Theme.White80)
                )
            }
        }
    }
}

// ─── Date selector ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(
    selectedDate:   LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    accentColor:    Color
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
        Icon(
            Icons.Default.CalendarToday, null,
            tint     = Theme.White60,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Date", color = Theme.White30, fontSize = 11.sp)
            Text(
                selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                color      = Theme.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Default.ArrowDropDown, null,
            tint     = Theme.White30,
            modifier = Modifier.size(18.dp)
        )
    }

    // Quick picker
    if (showQuickPicker) {
        AlertDialog(
            onDismissRequest = { showQuickPicker = false },
            containerColor   = Theme.CardBg,
            shape            = RoundedCornerShape(18.dp),
            title = { Text("Select Date", color = Theme.White) },
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
                                color   = if (date == selectedDate) accentColor.copy(alpha = 0.2f)
                                else Theme.White06
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
                        Icon(
                            Icons.Default.EditCalendar, null,
                            tint     = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Pick a custom date…",
                            color      = accentColor,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight, null,
                            tint     = accentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
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

    // Full date picker
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

// ─── Time preference selector ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePreferenceSelector(
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

    // Chip row
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
                            // Resolve AI suggestion immediately to a real Fixed time
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
                shape    = RoundedCornerShape(10.dp),
                color    = if (isSelected) accentColor.copy(alpha = 0.2f) else Theme.White06,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    label,
                    color      = if (isSelected) accentColor else Theme.White60,
                    fontSize   = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier   = Modifier.padding(vertical = 8.dp),
                    textAlign  = TextAlign.Center
                )
            }
        }
    }

    // Fixed time display row
    if (selected is TimePreference.Fixed) {
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Theme.White06)
                .clickable { showTimePicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                Icons.Outlined.Schedule, null,
                tint     = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            val hStr = if (initHour < 10) "0$initHour" else initHour.toString()
            val mStr = if (initMinute < 10) "0$initMinute" else initMinute.toString()
            Text(
                "$hStr:$mStr",
                color      = Theme.White,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text("tap to change", color = Theme.White30, fontSize = 11.sp)
        }
    }

    // Time picker dialog
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
            title = {
                Text(
                    "Select Time",
                    color      = Theme.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Box(
                    modifier            = Modifier.fillMaxWidth(),
                    contentAlignment    = Alignment.Center
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
                    val newTime = "$hStr:$mStr"
                    onTimeChanged(newTime)
                    onSelected(
                        TimePreference.Fixed(
                            LocalTime.of(timePickerState.hour, timePickerState.minute)
                        )
                    )
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

private data class QueuedItem(
    val title:          String,
    val timePreference: TimePreference,
    val category:       TaskCategory,
    val date:           LocalDate,
    val description:    String?,
    val timeLabel:      String,
    val repetition:     RepetitionInfo? = null
)
