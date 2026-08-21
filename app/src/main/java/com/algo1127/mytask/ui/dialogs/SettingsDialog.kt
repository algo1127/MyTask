package com.algo1127.mytask.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.Theme

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    var selectedAccent by remember { mutableStateOf(Theme.Teal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text("Settings", color = Theme.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // --- Accent Colors ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Accent Colour",
                        color = Theme.White60,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val colors = listOf(Theme.Teal, Theme.Blue, Theme.Purple, Theme.Gold, Theme.Rose, Theme.Emerald, Theme.Orange)
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .clickable { selectedAccent = color }
                                    .padding(4.dp)
                            ) {
                                if (selectedAccent == color) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = Theme.BgDeep,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Theme.White10)

                // --- AI Settings ---
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "AI Personality",
                        color = Theme.White60,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    val options = listOf(
                        "noRoast" to "Disable roasting",
                        "soulless" to "Plain notifications",
                        "moodcast" to "Dynamic moods"
                    )
                    options.forEach { (flag, desc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var checked by remember { mutableStateOf(false) }
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { checked = it },
                                colors = CheckboxDefaults.colors(checkedColor = selectedAccent)
                            )
                            Column {
                                Text(flag, color = Theme.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(desc, color = Theme.White30, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = selectedAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}
