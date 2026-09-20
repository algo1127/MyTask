package com.algo1127.mytask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubtaskRow(
    subtask: com.algo1127.mytask.ui.models.Subtask,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCompleted = subtask.isCompleted
    val themeColor = subtask.category.color
    
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.CardBg, RoundedCornerShape(14.dp))
                .combinedClickable(
                    interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isCompleted) themeColor.copy(alpha = 0.2f) else Theme.White06)
                        .clickable { onToggle() }
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCompleted) themeColor else Theme.White30,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subtask.title,
                        color = if (isCompleted) Theme.White30 else Theme.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(themeColor.copy(alpha = 0.1f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                            Text(subtask.category.label, color = themeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        if (subtask.dueDate != null) {
                            Icon(Icons.Default.Event, null, tint = Theme.White30, modifier = Modifier.size(10.dp))
                            Text(subtask.dueDate.toString(), color = Theme.White30, fontSize = 9.sp)
                        }
                        subtask.estimatedEffort?.let { duration ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.Timer, null, tint = Theme.White30, modifier = Modifier.size(10.dp))
                                Text("${duration.toMinutes()}m", color = Theme.White30, fontSize = 9.sp)
                            }
                        }
                    }
                }
                // Right side: show category icon cleanly
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        subtask.category.icon, 
                        null, 
                        tint = themeColor.copy(alpha = 0.8f), 
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
