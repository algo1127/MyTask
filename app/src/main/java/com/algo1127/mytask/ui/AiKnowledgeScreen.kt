package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.NotifAi.PermissionManager
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@Composable
fun AiKnowledgeScreen(
    knowledge: AiKnowledge,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(Theme.BgDeep)) {
        // Shifting background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .blur(100.dp)
                .background(Theme.Teal.copy(alpha = 0.05f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Theme.White06)) {
                    Icon(Icons.Default.Close, null, tint = Theme.White)
                }
                Text("AI Knowledge", color = Theme.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Box(modifier = Modifier.size(48.dp)) // Placeholder for balance
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            val hasUsageAccess = remember { PermissionManager.hasUsageAccess(context) }
            Surface(
                color = if (hasUsageAccess) Theme.Teal.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = if (hasUsageAccess) Theme.Teal else Color.Red
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (hasUsageAccess) "AI is actively learning from your usage" else "AI is currently offline (Usage Access needed)",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Core AI Stats ---
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    label = "Trust Level",
                    value = "${(knowledge.trustScore * 100).roundToInt()}%",
                    icon = Icons.Default.VerifiedUser,
                    color = Theme.Teal,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Effectiveness",
                    value = "${(knowledge.effectiveness * 100).roundToInt()}%",
                    icon = Icons.Default.Bolt,
                    color = Theme.Gold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    label = "Data Points",
                    value = knowledge.dataPoints.toString(),
                    icon = Icons.Default.BarChart,
                    color = Theme.Blue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Mood",
                    value = knowledge.mood.lowercase().replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Psychology,
                    color = Theme.Purple,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Category Profiles", color = Theme.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Engagement patterns learned per category", color = Theme.White30, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))

            knowledge.categoryInsights.forEach { insight ->
                CategoryInsightCard(insight)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Theme.White03,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Theme.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Theme.White30, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryInsightCard(insight: CategoryInsight) {
    val catColor = CategoryUtils.hexToColor(insight.colorHex)
    Surface(
        color = Theme.White03,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(catColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(CategoryUtils.getIcon(insight.iconName), null, tint = catColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(insight.label, color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(insight.summary, color = Theme.White80, fontSize = 14.sp, lineHeight = 20.sp)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Engagement Heatmap", color = Theme.White30, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Heatmap Canvas
            Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                val step = size.width / 24f
                insight.hourlyProfile.forEachIndexed { hour, score ->
                    val color = lerp(Theme.White06, catColor, score)
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(hour * step + 2f, 0f),
                        size = androidx.compose.ui.geometry.Size(step - 4f, size.height),
                        alpha = 0.8f
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("00:00", color = Theme.White10, fontSize = 9.sp)
                Text("12:00", color = Theme.White10, fontSize = 9.sp)
                Text("23:59", color = Theme.White10, fontSize = 9.sp)
            }
        }
    }
}
