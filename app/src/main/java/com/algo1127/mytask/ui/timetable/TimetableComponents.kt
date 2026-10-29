package com.algo1127.mytask.ui.timetable

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.data.TimetableSlot
import com.algo1127.mytask.ui.TaskCategory
import com.algo1127.mytask.ui.Theme
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val TIMETABLE_ACCENT_COLORS = listOf(
    0xFF4DFFD2, // Teal
    0xFFB57BFF, // Purple
    0xFFFF7597, // Rose
    0xFF50FA7B, // Emerald
    0xFFFFBD2E, // Gold
    0xFF3FC3F7, // Blue
    0xFFFFB86C  // Orange
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slotColor = Color(slot.colorHex)
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) slotColor.copy(alpha = 0.25f) else Theme.CardBg,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, slotColor) else androidx.compose.foundation.BorderStroke(1.dp, Theme.CardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent side pill
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(slotColor)
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.subjectName,
                    color = Theme.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${slot.startTime.format(timeFmt)} - ${slot.endTime.format(timeFmt)}",
                        color = Theme.White60,
                        fontSize = 11.sp
                    )
                    if (slot.location.isNotBlank()) {
                        Text(
                            text = "• ${slot.location}",
                            color = slotColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            if (isSelectionMode) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = slotColor)
                )
            }
        }
    }
}

@Composable
fun AddEditSlotDialog(
    initialSlot: TimetableSlot? = null,
    defaultDay: DayOfWeek = DayOfWeek.MONDAY,
    onDismiss: () -> Unit,
    onSave: (TimetableSlot) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    var subjectName by remember { mutableStateOf(initialSlot?.subjectName ?: "") }
    var selectedDay by remember { mutableStateOf(initialSlot?.dayOfWeek ?: defaultDay) }
    var startTime by remember { mutableStateOf(initialSlot?.startTime ?: LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(initialSlot?.endTime ?: LocalTime.of(10, 0)) }
    var location by remember { mutableStateOf(initialSlot?.location ?: "") }
    var teacher by remember { mutableStateOf(initialSlot?.teacher ?: "") }
    var colorHex by remember { mutableStateOf(initialSlot?.colorHex ?: TIMETABLE_ACCENT_COLORS.first()) }
    var selectedCategory by remember { mutableStateOf(initialSlot?.category ?: TaskCategory.Study) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                if (initialSlot == null) "Add Schedule Slot" else "Edit Schedule Slot",
                color = Theme.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Title", color = Theme.White60) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Theme.White,
                        unfocusedTextColor = Theme.White,
                        focusedBorderColor = Color(colorHex),
                        unfocusedBorderColor = Theme.White30
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Day Selector Chips
                Text("Day of Week", color = Theme.White80, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val isDaySelected = day == selectedDay
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDaySelected) Color(colorHex) else Theme.White06)
                                .clickable { selectedDay = day }
                        ) {
                            Text(
                                text = day.name.take(1),
                                color = if (isDaySelected) Theme.BgDeep else Theme.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Time Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePickerField(
                        label = "Start",
                        time = startTime,
                        color = Color(colorHex),
                        onTimeChange = { startTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = "End",
                        time = endTime,
                        color = Color(colorHex),
                        onTimeChange = { endTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Location & Teacher
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (optional)", color = Theme.White60) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Theme.White,
                        unfocusedTextColor = Theme.White,
                        focusedBorderColor = Color(colorHex),
                        unfocusedBorderColor = Theme.White30
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Notes (optional)", color = Theme.White60) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Theme.White,
                        unfocusedTextColor = Theme.White,
                        focusedBorderColor = Color(colorHex),
                        unfocusedBorderColor = Theme.White30
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Selection
                Text("Accent Color", color = Theme.White80, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TIMETABLE_ACCENT_COLORS.forEach { hex ->
                        val isColorSelected = hex == colorHex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (isColorSelected) 2.dp else 0.dp,
                                    color = if (isColorSelected) Theme.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (initialSlot != null && onDelete != null) {
                    Button(
                        onClick = { onDelete(initialSlot.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Theme.Rose),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Theme.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = Theme.White, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        if (subjectName.isNotBlank()) {
                            val slot = (initialSlot ?: TimetableSlot(
                                subjectName = subjectName,
                                dayOfWeek = selectedDay,
                                startTime = startTime,
                                endTime = endTime,
                                location = location,
                                teacher = teacher,
                                colorHex = colorHex,
                                category = selectedCategory
                            )).copy(
                                subjectName = subjectName,
                                dayOfWeek = selectedDay,
                                startTime = startTime,
                                endTime = endTime,
                                location = location,
                                teacher = teacher,
                                colorHex = colorHex,
                                category = selectedCategory
                            )
                            onSave(slot)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(colorHex)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Slot", color = Theme.BgDeep, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    time: LocalTime,
    color: Color,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(label, color = Theme.White60, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Theme.White06,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = time.format(fmt),
                    color = Theme.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Pick time",
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDialog) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Theme.CardBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Select $label Time", color = Theme.White, fontWeight = FontWeight.Bold) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = state,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Theme.White06,
                            selectorColor = color
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeChange(LocalTime.of(state.hour, state.minute))
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", color = Theme.BgDeep, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Theme.White60)
                }
            }
        )
    }
}

@Composable
fun CloneDayDialog(
    sourceDay: DayOfWeek,
    onDismiss: () -> Unit,
    onClone: (targetDays: List<DayOfWeek>) -> Unit
) {
    val selectedTargets = remember { mutableStateListOf<DayOfWeek>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Clone ${sourceDay.name.lowercase().replaceFirstChar { it.uppercase() }} Schedule",
                color = Theme.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Select target days to copy this schedule to:",
                    color = Theme.White60,
                    fontSize = 13.sp
                )

                DayOfWeek.entries.filter { it != sourceDay }.forEach { day ->
                    val isChecked = selectedTargets.contains(day)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (isChecked) selectedTargets.remove(day) else selectedTargets.add(day)
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                if (it) selectedTargets.add(day) else selectedTargets.remove(day)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Theme.Teal)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = day.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = Theme.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTargets.isNotEmpty()) {
                        onClone(selectedTargets.toList())
                    }
                },
                enabled = selectedTargets.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Theme.Teal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clone Schedule", color = Theme.BgDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Theme.White60)
            }
        }
    )
}

@Composable
fun BatchActionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Theme.BgSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Theme.Teal.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearSelection, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear selection", tint = Theme.White60)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "$selectedCount selected",
                    color = Theme.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = onDeleteSelected,
                colors = ButtonDefaults.buttonColors(containerColor = Theme.Rose),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = Theme.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete", color = Theme.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
