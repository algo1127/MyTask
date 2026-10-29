package com.algo1127.mytask.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.data.TimetableRepository
import com.algo1127.mytask.data.TimetableSlot
import com.algo1127.mytask.ui.Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSlotPickerBottomSheet(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSelectSlot: (TimetableSlot) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TimetableRepository.getInstance(context) }
    var slots by remember { mutableStateOf<List<TimetableSlot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val dayOfWeek = selectedDate.dayOfWeek
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LaunchedEffect(selectedDate) {
        slots = repository.getSlotsForDay(dayOfWeek)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Theme.Teal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Import from Timetable",
                            color = Theme.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Showing slots for ${selectedDate.format(dateFmt)}",
                            color = Theme.White60,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Theme.White60)
                }
            }

            HorizontalDivider(
                color = Theme.White06,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    CircularProgressIndicator(color = Theme.Teal)
                }
            } else if (slots.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No timetable classes for ${dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            color = Theme.White60,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Set up your schedule in Settings -> Timetable",
                            color = Theme.White30,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .padding(bottom = 16.dp)
                ) {
                    items(slots, key = { it.id }) { slot ->
                        val slotColor = Color(slot.colorHex)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Theme.BgMid,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Theme.CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSelectSlot(slot)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(CircleShape)
                                        .background(slotColor)
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slot.subjectName,
                                        color = Theme.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${slot.startTime.format(timeFmt)} - ${slot.endTime.format(timeFmt)}",
                                            color = Theme.White60,
                                            fontSize = 12.sp
                                        )
                                        if (slot.location.isNotBlank()) {
                                            Text(
                                                text = "• ${slot.location}",
                                                color = slotColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = slotColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Select",
                                        color = slotColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
