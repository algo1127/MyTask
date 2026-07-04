package com.algo1127.mytask.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.Theme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

data class RepetitionInfo(
    val frequency: String,
    val interval: Int = 1,
    val selectedDays: Set<DayOfWeek>? = null,
    val untilDate: LocalDate? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatingSelector(
    onRepetitionChanged: (RepetitionInfo?) -> Unit,
    accentColor: Color
) {
    var isRepeating by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("Daily") }
    var interval by remember { mutableStateOf(1) }
    var showCustomOptions by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    isRepeating = !isRepeating
                    if (!isRepeating) onRepetitionChanged(null)
                    else onRepetitionChanged(RepetitionInfo(frequency, interval))
                }
                .padding(vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Repeat, null,
                tint = if (isRepeating) accentColor else Theme.White30,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Repeat",
                color = if (isRepeating) Theme.White else Theme.White30,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = isRepeating,
                onCheckedChange = { 
                    isRepeating = it
                    if (!isRepeating) onRepetitionChanged(null)
                    else onRepetitionChanged(RepetitionInfo(frequency, interval))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.3f),
                    uncheckedThumbColor = Theme.White30,
                    uncheckedTrackColor = Theme.White10
                )
            )
        }

        AnimatedVisibility(visible = isRepeating) {
            Column(modifier = Modifier.padding(start = 30.dp, bottom = 10.dp)) {
                // Frequency Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { freq ->
                        val selected = frequency == freq
                        Surface(
                            onClick = { 
                                frequency = freq
                                onRepetitionChanged(RepetitionInfo(frequency, interval))
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) accentColor.copy(alpha = 0.2f) else Theme.White06
                        ) {
                            Text(
                                freq,
                                color = if (selected) accentColor else Theme.White60,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Custom Interval
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Every", color = Theme.White60, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Theme.White06)
                            .clickable { 
                                interval = if (interval < 99) interval + 1 else 1
                                onRepetitionChanged(RepetitionInfo(frequency, interval))
                            }
                    ) {
                        Text(interval.toString(), color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (frequency) {
                            "Daily" -> if (interval == 1) "day" else "days"
                            "Weekly" -> if (interval == 1) "week" else "weeks"
                            "Monthly" -> if (interval == 1) "month" else "months"
                            else -> if (interval == 1) "year" else "years"
                        },
                        color = Theme.White60, fontSize = 13.sp
                    )
                }
            }
        }
    }
}
