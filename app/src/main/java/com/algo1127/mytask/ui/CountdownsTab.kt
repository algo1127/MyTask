package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.models.CountdownItem
import java.time.Duration
import java.time.LocalDateTime

@Composable
fun CountdownsTab(
    countdowns: List<CountdownItem>,
    onClick: (CountdownItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
    ) {
        if (countdowns.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, null, tint = Theme.White10, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No active countdowns", color = Theme.White30, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(countdowns, key = { it.id }) { item ->
                CountdownCard(item = item, onClick = { onClick(item) })
            }
        }
    }
}

@Composable
private fun CountdownCard(
    item: CountdownItem,
    onClick: () -> Unit
) {
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    
    // Smooth ticking
    LaunchedEffect(Unit) {
        while(true) {
            now.value = LocalDateTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    val remaining = Duration.between(now.value, item.targetDateTime)
    val isExpired = remaining.isNegative

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Theme.CardBg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(item.color).copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Default.HourglassEmpty, 
                    null, 
                    tint = Color(item.color), 
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.optionalTitle ?: item.title,
                    color = Theme.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = if (isExpired) "Time's up!" else formatRemaining(remaining),
                    color = if (isExpired) Color.Red else Color(item.color).copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Theme.White10
            )
        }
    }
}

private fun formatRemaining(duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val mins = duration.toMinutes() % 60
    val secs = duration.seconds % 60
    
    return when {
        days > 0 -> "$days d, $hours h, $mins m"
        hours > 0 -> "$hours h, $mins m, $secs s"
        else -> "$mins m, $secs s"
    }
}
