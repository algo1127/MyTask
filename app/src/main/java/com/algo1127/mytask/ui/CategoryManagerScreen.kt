package com.algo1127.mytask.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algo1127.mytask.ui.dialogs.RepetitionInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    categories: List<TaskCategory>,
    onAdd: (String, String, String) -> Unit,
    onUpdate: (TaskCategory) -> Unit,
    onDelete: (TaskCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<TaskCategory?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Theme.BgDeep)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(Theme.White06)) {
                    Icon(Icons.Default.Close, null, tint = Theme.White)
                }
                Text("Categories", color = Theme.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                IconButton(
                    onClick = { categoryToEdit = null; showEditor = true },
                    modifier = Modifier.clip(CircleShape).background(Theme.Teal)
                ) {
                    Icon(Icons.Default.Add, null, tint = Theme.BgDeep)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                categories.forEach { category ->
                    CategoryRow(
                        category = category,
                        onClick = { categoryToEdit = category; showEditor = true },
                        onDelete = { onDelete(category) }
                    )
                }
            }
        }

        if (showEditor) {
            CategoryEditor(
                initialCategory = categoryToEdit,
                onSave = { label, icon, color ->
                    if (categoryToEdit == null) onAdd(label, icon, color)
                    else onUpdate(categoryToEdit!!.copy(label = label, iconName = icon, colorHex = color))
                    showEditor = false
                },
                onDismiss = { showEditor = false }
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: TaskCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Theme.White03,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(category.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, null, tint = category.color, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(category.label, color = Theme.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditor(
    initialCategory: TaskCategory?,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(initialCategory?.label ?: "") }
    var selectedIconName by remember { mutableStateOf(initialCategory?.iconName ?: "Task") }
    var selectedColor by remember { mutableStateOf(initialCategory?.color ?: Theme.Teal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Theme.CardBg,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.padding(16.dp),
        title = {
            Text(if (initialCategory == null) "New Category" else "Edit Category", color = Theme.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label", color = Theme.White60) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor,
                        unfocusedBorderColor = Theme.White10,
                        focusedTextColor = Theme.White,
                        unfocusedTextColor = Theme.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", color = Theme.White60, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                
                Box(modifier = Modifier.height(200.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CategoryUtils.icons.keys.toList()) { iconName ->
                            val icon = CategoryUtils.getIcon(iconName)
                            val isSelected = selectedIconName == iconName
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) selectedColor.copy(alpha = 0.2f) else Theme.White06)
                                    .border(if (isSelected) 2.dp else 0.dp, selectedColor, RoundedCornerShape(12.dp))
                                    .clickable { selectedIconName = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = if (isSelected) selectedColor else Theme.White30, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                Text("Color", color = Theme.White60, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategoryUtils.colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (selectedColor == color) 2.dp else 0.dp, Theme.White, CircleShape)
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label, selectedIconName, CategoryUtils.colorToHex(selectedColor)) },
                enabled = label.isNotBlank()
            ) {
                Text("Save", color = selectedColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Theme.White60) }
        }
    )
}
