package com.algo1127.mytask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.models.Task

@Composable
fun TaskSidebar(
    unscheduledTasks: List<Task>,
    onAddTask: () -> Unit,
    onDragTask: (Task) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(Theme.BgDeep.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.List, null, tint = Theme.Teal)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Inbox",
                color = Theme.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, null, tint = Theme.White60)
            }
        }

        if (unscheduledTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No unscheduled tasks", color = Theme.White30, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(unscheduledTasks) { task ->
                    UnscheduledTaskItem(task = task)
                }
            }
        }
    }
}

@Composable
fun UnscheduledTaskItem(task: Task) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = Theme.White06
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                task.title,
                color = Theme.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (task.description.isNotEmpty()) {
                Text(
                    task.description,
                    color = Theme.White60,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(task.category.color.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    task.category.label,
                    color = task.category.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
