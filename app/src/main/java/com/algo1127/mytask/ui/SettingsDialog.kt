package com.algo1127.mytask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text("Settings", color = Theme.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "AI Personality",
                    color = Theme.Teal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                listOf(
                    "noRoast" to "Disable roasts",
                    "soulless" to "Plain notifications",
                    "moodcast" to "Dynamic moods"
                ).forEach { (flag, desc) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Theme.Teal.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(flag, color = Theme.White80, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(desc, color = Theme.White30, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = Theme.Teal, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}