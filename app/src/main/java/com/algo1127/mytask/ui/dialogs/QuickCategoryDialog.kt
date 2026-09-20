package com.algo1127.mytask.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.CategoryUtils
import com.algo1127.mytask.ui.TaskCategory
import com.algo1127.mytask.ui.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (TaskCategory) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(CategoryUtils.colors.first()) }
    var selectedIconName by remember { mutableStateOf("Label") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("One-time Category", color = Theme.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name", color = Theme.White30) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor,
                        unfocusedBorderColor = Theme.White10,
                        focusedTextColor = Theme.White,
                        unfocusedTextColor = Theme.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pick a Color", color = Theme.White60, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(100.dp)
                ) {
                    items(CategoryUtils.colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(Icons.Default.Check, null, tint = Theme.BgDeep, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Text("Pick an Icon", color = Theme.White60, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryUtils.icons.keys.forEach { iconName ->
                        val icon = CategoryUtils.getIcon(iconName)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedIconName == iconName) selectedColor.copy(alpha = 0.2f) else Theme.White06)
                                .clickable { selectedIconName = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = if (selectedIconName == iconName) selectedColor else Theme.White60, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(TaskCategory(label = name.trim(), iconName = selectedIconName, colorHex = CategoryUtils.colorToHex(selectedColor)))
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create", color = selectedColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Theme.White60)
            }
        }
    )
}
