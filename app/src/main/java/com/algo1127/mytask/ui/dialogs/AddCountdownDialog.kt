package com.algo1127.mytask.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCountdownDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, target: LocalDateTime) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(12, 0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .padding(1.dp)
            .background(
                Brush.linearGradient(listOf(Theme.Gold.copy(alpha = 0.08f), Color.Transparent)),
                RoundedCornerShape(22.dp)
            )
            .padding(1.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HourglassEmpty, null, tint = Theme.Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Countdown", color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are we waiting for?", color = Theme.White60) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Theme.Gold,
                        unfocusedBorderColor = Theme.White10,
                        focusedTextColor = Theme.White,
                        unfocusedTextColor = Theme.White
                    ),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                )

                // Date and Time selectors would go here - for brevity using placeholders
                Text("Target: ${selectedDate} at ${selectedTime}", color = Theme.White60, fontSize = 13.sp)
                
                Text(
                    "This will pin a live timer to your dashboard.",
                    color = Theme.White30, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title.trim(), LocalDateTime.of(selectedDate, selectedTime))
                    }
                    onDismiss()
                },
                enabled = title.isNotBlank()
            ) {
                Text("Start", color = Theme.Gold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Theme.White60) }
        }
    )
}
