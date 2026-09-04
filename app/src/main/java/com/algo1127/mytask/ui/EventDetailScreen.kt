package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.max

@Composable
fun EventDetailScreen(
    event: EventItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // Animation for gradient position (orbiting)
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    // Animation for color shifting
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "colorPhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.BgDeep)
    ) {
        // High-quality animated radial gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(
                x = size.width * (0.5f + 0.25f * cos(phase)),
                y = size.height * (0.5f + 0.25f * sin(phase * 0.8f))
            )
            
            val baseColor = lerp(Theme.Blue, Theme.Purple, colorPhase)
            val secondaryColor = lerp(Theme.Teal, Theme.Blue, colorPhase)
            
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.12f),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = max(size.width, size.height) * 1.1f
                )
            )
            
            // Secondary glow for depth
            val center2 = Offset(
                x = size.width * (0.5f + 0.2f * sin(phase * 1.2f)),
                y = size.height * (0.5f + 0.2f * cos(phase * 0.5f))
            )
            
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Theme.Teal.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center2,
                    radius = max(size.width, size.height) * 0.8f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header Actions
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
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Event Title
            Text(
                text = event.title,
                color = Theme.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Start,
                lineHeight = 40.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Time and Date Section
            Surface(
                color = Theme.White03,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailItem(
                        icon = Icons.Default.Event,
                        label = "Date",
                        value = event.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
                    )
                    
                    HorizontalDivider(color = Theme.White06)
                    
                    DetailItem(
                        icon = Icons.Default.Schedule,
                        label = "Time",
                        value = "${event.startTime} - ${event.endTime}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Location Section
            if (event.location.isNotBlank()) {
                Surface(
                    color = Theme.White03,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DetailItem(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = event.location,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Notes Section
            if (event.notes.isNotBlank()) {
                Surface(
                    color = Theme.White03,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DetailItem(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        label = "Notes",
                        value = event.notes,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Bottom decoration or additional info
            Text(
                text = "Event ID: ${event.id}",
                color = Theme.White10,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Theme.Blue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Theme.Blue, modifier = Modifier.size(18.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = label,
                color = Theme.White30,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Theme.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
