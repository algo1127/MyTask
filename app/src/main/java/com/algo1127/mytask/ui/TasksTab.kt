package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTab(
    tasks: List<TaskItem>,
    selectedDate: LocalDate,
    completedIds: Set<Long>,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onTaskUpdate: (TaskItem) -> Unit = {},
    onEditRequest: (TaskItem) -> Unit = {}
) {
    var isGridView by remember { mutableStateOf(false) }

    val visibleTasks = remember(tasks, selectedDate) {
        tasks.filter { it.date == selectedDate && !it.isReminder }
    }

    if (isGridView) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                        contentDescription = "Toggle View",
                        tint = Theme.White60
                    )
                }
            }
            EisenhowerGrid(
                tasks = visibleTasks,
                onTaskUpdate = onTaskUpdate,
                onToggleCompletion = onToggleCompletion,
                completedIds = completedIds
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = "Toggle View",
                            tint = Theme.White60
                        )
                    }
                }
            }

            if (visibleTasks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Task,
                        title = "No tasks",
                        subtitle = "Tap + to add a task",
                        color = Theme.Purple
                    )
                }
            } else {
                items(visibleTasks, key = { it.id }) { item ->
                    val isCompleted = completedIds.contains(item.id)
                    
                    val swipeState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onTaskUpdate(item.copy(date = item.date.plusDays(1)))
                                    true
                                }
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    onToggleCompletion(item.id, !isCompleted)
                                    false
                                }
                                else -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = swipeState,
                        backgroundContent = {
                            val direction = swipeState.dismissDirection
                            val color = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Theme.Teal
                                SwipeToDismissBoxValue.EndToStart -> Theme.Gold
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(color),
                                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Check else Icons.Default.Schedule
                                Icon(icon, null, tint = Theme.BgDeep, modifier = Modifier.padding(horizontal = 20.dp))
                            }
                        },
                        content = {
                            TaskRow(
                                item = item,
                                isCompleted = isCompleted,
                                onToggle = { onToggleCompletion(item.id, !isCompleted) },
                                onLongClick = { onEditRequest(item) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(300),
                                    placementSpec = spring(stiffness = Spring.StiffnessLow)
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    item: TaskItem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {}
) {
    var isTapped by remember { mutableStateOf(false) }
    LaunchedEffect(isTapped) { if (isTapped) { delay(150); isTapped = false } }

    val scale by animateFloatAsState(
        targetValue = if (isTapped) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "taskScale"
    )

    Surface(
        modifier = modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isCompleted)
                        Brush.linearGradient(colors = listOf(Theme.Purple.copy(alpha = 0.12f), Theme.Purple.copy(alpha = 0.06f)))
                    else
                        Brush.linearGradient(colors = listOf(Theme.CardBg, Theme.CardBg))
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { isTapped = true; onToggle() },
                    onLongClick = onLongClick
                )
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .background(if (isCompleted) Theme.Purple else item.category.color.copy(alpha = 0.8f))
                    .align(Alignment.CenterStart)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                        .background(if (isCompleted) Theme.Purple.copy(alpha = 0.2f) else item.category.color.copy(alpha = 0.12f))
                ) {
                    AnimatedContent(isCompleted, label = "iconAnim") { completed ->
                        Icon(
                            imageVector = if (completed) Icons.Default.CheckCircle else item.category.icon,
                            contentDescription = null,
                            tint = if (completed) Theme.Purple else item.category.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = if (isCompleted) Theme.White30 else Theme.White,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(item.category.color.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(item.category.label, color = item.category.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.Schedule, null, tint = Theme.White30, modifier = Modifier.size(12.dp))
                        Text(item.time, color = Theme.White60, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                AnimatedVisibility(
                    visible = isCompleted,
                    enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).clip(CircleShape).background(Theme.Purple)) {
                        Icon(Icons.Default.Check, null, tint = Theme.BgDeep, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
