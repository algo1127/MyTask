package com.algo1127.mytask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun EisenhowerGrid(
    tasks: List<TaskItem>,
    onTaskUpdate: (TaskItem) -> Unit,
    onToggleCompletion: (Long, Boolean) -> Unit,
    completedIds: Set<Long>
) {
    val quadrants = listOf(
        QuadrantInfo("Do Now", true, true, Theme.Teal),
        QuadrantInfo("Schedule", false, true, Theme.Blue),
        QuadrantInfo("Delegate", true, false, Theme.Gold),
        QuadrantInfo("Delete", false, false, Color.Red.copy(alpha = 0.7f))
    )

    var draggedTask by remember { mutableStateOf<TaskItem?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                Quadrant(
                    info = quadrants[0],
                    tasks = tasks.filter { it.isUrgent && it.isImportant },
                    modifier = Modifier.weight(1f),
                    onTaskDropped = { task -> onTaskUpdate(task.copy(isUrgent = true, isImportant = true)) },
                    onTaskDragStart = { draggedTask = it },
                    onToggleCompletion = onToggleCompletion,
                    completedIds = completedIds
                )
                Quadrant(
                    info = quadrants[1],
                    tasks = tasks.filter { !it.isUrgent && it.isImportant },
                    modifier = Modifier.weight(1f),
                    onTaskDropped = { task -> onTaskUpdate(task.copy(isUrgent = false, isImportant = true)) },
                    onTaskDragStart = { draggedTask = it },
                    onToggleCompletion = onToggleCompletion,
                    completedIds = completedIds
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                Quadrant(
                    info = quadrants[2],
                    tasks = tasks.filter { it.isUrgent && !it.isImportant },
                    modifier = Modifier.weight(1f),
                    onTaskDropped = { task -> onTaskUpdate(task.copy(isUrgent = true, isImportant = false)) },
                    onTaskDragStart = { draggedTask = it },
                    onToggleCompletion = onToggleCompletion,
                    completedIds = completedIds
                )
                Quadrant(
                    info = quadrants[3],
                    tasks = tasks.filter { !it.isUrgent && !it.isImportant },
                    modifier = Modifier.weight(1f),
                    onTaskDropped = { task -> onTaskUpdate(task.copy(isUrgent = false, isImportant = false)) },
                    onTaskDragStart = { draggedTask = it },
                    onToggleCompletion = onToggleCompletion,
                    completedIds = completedIds
                )
            }
        }

        // Overlay for dragged item (simplified)
        draggedTask?.let { task ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .zIndex(10f)
                    .width(150.dp)
            ) {
                TaskGridItem(task = task, isCompleted = completedIds.contains(task.id), onToggle = {})
            }
        }
    }
}

data class QuadrantInfo(
    val label: String,
    val urgent: Boolean,
    val important: Boolean,
    val color: Color
)

@Composable
fun Quadrant(
    info: QuadrantInfo,
    tasks: List<TaskItem>,
    modifier: Modifier = Modifier,
    onTaskDropped: (TaskItem) -> Unit,
    onTaskDragStart: (TaskItem) -> Unit,
    onToggleCompletion: (Long, Boolean) -> Unit,
    completedIds: Set<Long>
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .border(1.dp, Theme.White10, RoundedCornerShape(12.dp))
            .background(Theme.White03, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = info.label,
                color = info.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tasks, key = { it.id }) { task ->
                    TaskGridItem(
                        task = task,
                        isCompleted = completedIds.contains(task.id),
                        onToggle = { onToggleCompletion(task.id, !completedIds.contains(task.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskGridItem(
    task: TaskItem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Theme.White10 else Theme.CardBg
        ),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragStart = onDragStart,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragIndicator,
                null,
                tint = Theme.White30,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = task.title,
                color = if (isCompleted) Theme.White30 else Theme.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
