package com.novelsim.app.presentation.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.CharacterStats

/**
 * 自定义属性编辑器
 * 用于编辑 CharacterStats 中的自定义数值属性
 */
@Composable
fun CustomStatsEditor(
    stats: CharacterStats,
    onStatsChange: (CharacterStats) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    // 排除标准属性，只显示自定义属性
    val standardKeys = setOf(
        "maxHp", "currentHp", "maxMp", "currentMp", 
        "attack", "defense", "speed", "luck", 
        "level", "exp", "expToNextLevel"
    )
    
    val customStatsLines = stats.data.filterKeys { !standardKeys.contains(it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "自定义属性 (数值)",
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (customStatsLines.isEmpty()) {
                Text(
                    text = "暂无自定义数值属性",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customStatsLines.forEach { (key, value) ->
                        CustomStatRow(
                            name = key,
                            value = value,
                            onValueChange = { newValue ->
                                val newStats = stats.copy()
                                newStats[key] = newValue
                                onStatsChange(newStats)
                            },
                            onDelete = {
                                val newStats = stats.copy()
                                newStats.data.remove(key)
                                onStatsChange(newStats)
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddStatDialog(
            existingKeys = stats.data.keys,
            onConfirm = { key, value ->
                val newStats = stats.copy()
                newStats[key] = value
                onStatsChange(newStats)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun CustomStatRow(
    name: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var editValue by remember(value) { mutableStateOf(value.toString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 属性名（只读）
        OutlinedTextField(
            value = name,
            onValueChange = { },
            label = { Text("属性名") },
            modifier = Modifier.weight(0.4f),
            enabled = false,
            singleLine = true
        )
        
        // 属性值（可编辑）
        OutlinedTextField(
            value = editValue,
            onValueChange = { 
                editValue = it
                it.toIntOrNull()?.let { intVal -> onValueChange(intVal) }
            },
            label = { Text("值") },
            modifier = Modifier.weight(0.5f),
            singleLine = true,
            isError = editValue.toIntOrNull() == null
        )
        
        // 删除按钮
        IconButton(onClick = { showDeleteConfirm = true }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除属性 \"$name\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AddStatDialog(
    existingKeys: Set<String>,
    onConfirm: (key: String, value: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加自定义数值属性") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { 
                        newKey = it
                        error = null
                    },
                    label = { Text("属性名") },
                    placeholder = { Text("例如: sanity") },
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    label = { Text("初始值") },
                    placeholder = { Text("例如: 100") },
                    singleLine = true,
                    isError = newValue.toIntOrNull() == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newKey.isBlank() -> error = "属性名不能为空"
                        existingKeys.contains(newKey) -> error = "属性名已存在"
                        newValue.toIntOrNull() == null -> error = "必须输入有效的整数"
                        else -> onConfirm(newKey, newValue.toInt())
                    }
                }
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
