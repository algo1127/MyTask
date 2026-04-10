package com.algo1127.mytask.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Event
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
import com.algo1127.mytask.ui.TaskItem
import com.algo1127.mytask.ui.Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onAdd: (title: String, reminders: List<TaskItem>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .padding(1.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(Theme.Blue.copy(alpha = 0.08f), Color.Transparent)
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(1.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Event, null,
                    tint = Theme.Blue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Add Event",
                    color = Theme.White,
                    fontSize = 18.sp,
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
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event title", color = Theme.White60) },
                    singleLine = true,
                    colors = eventTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (optional)", color = Theme.White60) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, null, tint = Theme.White30, modifier = Modifier.size(18.dp))
                    },
                    colors = eventTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", color = Theme.White60) },
                    colors = eventTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Date display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Theme.White06)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, null, tint = Theme.Blue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Date", color = Theme.White30, fontSize = 11.sp)
                        Text(
                            defaultDate.format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy")),
                            color = Theme.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title.trim(), emptyList())
                    }
                    onDismiss()
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add", color = Theme.Blue, fontWeight = FontWeight.SemiBold)
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
private fun eventTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Theme.Blue,
    unfocusedBorderColor = Theme.White10,
    focusedLabelColor = Theme.Blue,
    unfocusedLabelColor = Theme.White30,
    cursorColor = Theme.Blue,
    focusedTextColor = Theme.White,
    unfocusedTextColor = Theme.White
)