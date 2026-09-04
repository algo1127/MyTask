package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import com.algo1127.mytask.ui.models.CountdownItem
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountdownDetailScreen(
    item: CountdownItem,
    linkedItemInfo: Pair<String, TaskCategory>? = null,
    onDismiss: () -> Unit,
    onEdit: (CountdownItem) -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = Color(item.color)
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            now.value = LocalDateTime.now()
            delay(1000)
        }
    }

    val remaining by remember { derivedStateOf { Duration.between(now.value, item.targetDateTime) } }
    val total = remember(item.id) { Duration.between(item.createdAt, item.targetDateTime) }
    
    val years by remember { derivedStateOf { remaining.toDays() / 365 } }
    val isExpired by remember { derivedStateOf { remaining.isNegative } }
    val displayRemaining by remember { derivedStateOf { if (isExpired) Duration.ZERO else remaining } }
    val progress by remember { derivedStateOf { if (total.isZero) 1f else (1f - remaining.toMillis().toFloat() / total.toMillis().toFloat()).coerceIn(0f, 1f) } }

    var selectedResolution by remember { mutableStateOf(TimeResolution.MINUTES) }
    var showEditDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.BgDeep)
    ) {
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .blur(100.dp)
                .background(themeColor.copy(alpha = 0.05f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.clip(CircleShape).background(Theme.White06)
                ) {
                    Icon(Icons.Default.Close, null, tint = Theme.White)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.clip(CircleShape).background(Theme.White06)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Theme.White)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.optionalTitle ?: item.title,
                    color = Theme.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                if (linkedItemInfo != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinkedItemPill(info = linkedItemInfo)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            BigTimer(duration = displayRemaining, isExpired = isExpired, years = years)

            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = themeColor, spotColor = themeColor),
                    color = themeColor,
                    trackColor = Theme.White06,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            val totalUnits = when(selectedResolution) {
                TimeResolution.SECONDS -> total.seconds
                TimeResolution.MINUTES -> total.toMinutes()
                TimeResolution.HOURS   -> total.toHours()
                TimeResolution.DAYS    -> total.toDays()
            }.toInt().coerceAtLeast(1)

            val unitsPassed = if (isExpired) totalUnits else when(selectedResolution) {
                TimeResolution.SECONDS -> Duration.between(item.createdAt, now.value).seconds
                TimeResolution.MINUTES -> Duration.between(item.createdAt, now.value).toMinutes()
                TimeResolution.HOURS   -> Duration.between(item.createdAt, now.value).toHours()
                TimeResolution.DAYS    -> Duration.between(item.createdAt, now.value).toDays()
            }.toInt().coerceIn(0, totalUnits)

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Visual Units", color = Theme.White60, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "$unitsPassed / $totalUnits",
                        color = Theme.White30,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Theme.White06)
                        .padding(2.dp)
                ) {
                    val switcherWidth = 180.dp
                    val resOptions = TimeResolution.entries
                    val resWidth = switcherWidth / resOptions.size.toFloat()
                    val selectedIdx = resOptions.indexOf(selectedResolution)
                    
                    val previousIdx = remember { mutableIntStateOf(selectedIdx) }
                    val movingRight = selectedIdx > previousIdx.intValue
                    SideEffect { previousIdx.intValue = selectedIdx }

                    val indicatorStart by animateDpAsState(
                        targetValue = resWidth * selectedIdx,
                        animationSpec = spring(stiffness = if (movingRight) 400f else 900f, dampingRatio = 0.8f),
                        label = "resStart"
                    )
                    val indicatorEnd by animateDpAsState(
                        targetValue = resWidth * (selectedIdx + 1),
                        animationSpec = spring(stiffness = if (movingRight) 900f else 400f, dampingRatio = 0.8f),
                        label = "resEnd"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = indicatorStart)
                            .width(indicatorEnd - indicatorStart)
                            .height(28.dp)
                            .background(themeColor, RoundedCornerShape(8.dp))
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp), ambientColor = themeColor, spotColor = themeColor)
                    )

                    Row(modifier = Modifier.width(switcherWidth)) {
                        resOptions.forEach { res ->
                            val isSelected = res == selectedResolution
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                                        selectedResolution = res 
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res.label,
                                    color = if (isSelected) Theme.BgDeep else Theme.White30,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
            
            Box(modifier = Modifier.weight(1f)) {
                CubeGrid(
                    totalCubes = totalUnits,
                    currentValue = unitsPassed,
                    color = themeColor,
                    resolution = selectedResolution
                )
            }
            
            Text(
                text = "Target: ${item.targetDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm"))}",
                color = Theme.White30,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )
        }
    }

    if (showEditDialog) {
        EditCountdownDialog(
            item = item,
            onDismiss = { showEditDialog = false },
            onSave = { newTitle, newColor ->
                onEdit(item.copy(optionalTitle = newTitle, color = newColor.toArgb()))
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun LinkedItemPill(info: Pair<String, TaskCategory>) {
    val (name, category) = info
    Surface(
        color = category.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, category.color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(category.icon, null, tint = category.color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, color = category.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EditCountdownDialog(
    item: CountdownItem,
    onDismiss: () -> Unit,
    onSave: (String, Color) -> Unit
) {
    var title by remember { mutableStateOf(item.optionalTitle ?: item.title) }
    var selectedColor by remember { mutableStateOf(Color(item.color)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(22.dp),
        title = { Text("Edit Timer", color = Theme.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = Theme.White60) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Theme.Orange,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Theme Color", color = Theme.White60, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val colors = listOf(Theme.Teal, Theme.Blue, Theme.Purple, Theme.Gold, Theme.Rose, Theme.Emerald, Theme.Orange)
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 2.dp else 0.dp,
                                    color = Theme.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, selectedColor) }) {
                Text("Save Changes", color = Theme.Orange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Theme.White60) }
        }
    )
}

@Composable
private fun BigTimer(duration: Duration, isExpired: Boolean = false, years: Long = 0) {
    val days = duration.toDays() % 365
    val hours = duration.toHours() % 24
    val mins = duration.toMinutes() % 60
    val secs = duration.seconds % 60

    if (isExpired) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("TIME'S UP", color = Color.Red, fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("Timer finished", color = Theme.White30, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            if (years > 0) {
                TimerUnit(value = years.toString(), label = if (years == 1L) "YEAR" else "YEARS")
                TimerSeparator()
            }
            if (days > 0 || years > 0) {
                TimerUnit(value = days.toString(), label = if (days == 1L) "DAY" else "DAYS")
                TimerSeparator()
            }
            TimerUnit(value = String.format(java.util.Locale.US, "%02d", hours), label = "HRS")
            TimerSeparator()
            TimerUnit(value = String.format(java.util.Locale.US, "%02d", mins), label = "MIN")
            TimerSeparator()
            TimerUnit(value = String.format(java.util.Locale.US, "%02d", secs), label = "SEC")
        }
    }
}

@Composable
private fun TimerUnit(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Theme.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 2.sp)
        Text(label, color = Theme.White30, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimerSeparator() {
    Text(":", color = Theme.White10, fontSize = 40.sp, modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp))
}

@Composable
private fun CubeGrid(totalCubes: Int, currentValue: Int, color: Color, resolution: TimeResolution) {
    val cubeSize = remember(resolution) {
        when(resolution) {
            TimeResolution.SECONDS -> 4.dp
            TimeResolution.MINUTES -> 8.dp
            TimeResolution.HOURS   -> 14.dp
            TimeResolution.DAYS    -> 24.dp
        }
    }
    val spacing = remember(resolution) {
        when(resolution) {
            TimeResolution.SECONDS -> 2.dp
            else -> 4.dp
        }
    }

    val listState = rememberLazyListState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        
        val gridData = remember(maxWidth, totalCubes, cubeSize, spacing) {
            val cubeSizePx = with(density) { cubeSize.toPx() }
            val spacingPx = with(density) { spacing.toPx() }
            val columns = max(1, (with(density) { maxWidth.toPx() } / (cubeSizePx + spacingPx)).toInt())
            val totalRows = ceil(totalCubes.toFloat() / columns).toInt()
            Triple(columns, totalRows, cubeSizePx to spacingPx)
        }
        
        val columns = gridData.first
        val totalRows = gridData.second
        val (cubeSizePx, spacingPx) = gridData.third
        val currentFillRow = currentValue / columns

        LaunchedEffect(resolution) {
            if (totalRows > 0) {
                listState.animateScrollToItem(currentFillRow.coerceIn(0, totalRows - 1))
            }
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(totalRows) { rowIndex ->
                val startIdx = rowIndex * columns
                val endIdx = min(startIdx + columns, totalCubes)
                val itemsInRow = endIdx - startIdx
                
                Canvas(modifier = Modifier.fillMaxWidth().height(cubeSize)) {
                    for (i in 0 until itemsInRow) {
                        val globalIdx = startIdx + i
                        val x = i * (cubeSizePx + spacingPx)
                        
                        val isFilled = globalIdx < currentValue
                        val alpha = if (isFilled) 1f else 0.08f
                        
                        drawRoundRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                            size = androidx.compose.ui.geometry.Size(cubeSizePx, cubeSizePx),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cubeSizePx * 0.2f),
                            alpha = alpha
                        )
                        
                        if (!isFilled) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                                size = androidx.compose.ui.geometry.Size(cubeSizePx, cubeSizePx),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cubeSizePx * 0.2f),
                                alpha = 0.05f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class TimeResolution(val label: String) {
    SECONDS("Sec"), MINUTES("Min"), HOURS("Hrs"), DAYS("Days")
}
