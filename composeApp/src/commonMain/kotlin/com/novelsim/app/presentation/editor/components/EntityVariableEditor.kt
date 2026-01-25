package com.novelsim.app.presentation.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 实体变量编辑器 - 可复用组件
 * 用于在角色、道具、地点等实体的编辑页面中嵌入变量管理功能
 * 
 * @param variables 当前实体的变量 Map
 * @param onVariablesChange 变量变更回调
 */
@Composable
fun EntityVariableEditor(
    variables: Map<String, String>,
    onVariablesChange: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "自定义属性",
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
            
            if (variables.isEmpty()) {
                Text(
                    text = "暂无自定义属性",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    variables.forEach { (key, value) ->
                        EntityVariableRow(
                            name = key,
                            value = value,
                            onValueChange = { newValue ->
                                onVariablesChange(variables.toMutableMap().apply { this[key] = newValue })
                            },
                            onDelete = {
                                onVariablesChange(variables.toMutableMap().apply { remove(key) })
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddVariableDialog(
            existingKeys = variables.keys,
            onConfirm = { key, value ->
                onVariablesChange(variables.toMutableMap().apply { this[key] = value })
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun EntityVariableRow(
    name: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var editValue by remember(value) { mutableStateOf(value) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 变量名（只读）
        OutlinedTextField(
            value = name,
            onValueChange = { },
            label = { Text("属性名") },
            modifier = Modifier.weight(0.4f),
            enabled = false,
            singleLine = true
        )
        
        // 变量值（可编辑）
        OutlinedTextField(
            value = editValue,
            onValueChange = { 
                editValue = it
                onValueChange(it)
            },
            label = { Text("值") },
            modifier = Modifier.weight(0.5f),
            singleLine = true
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
private fun AddVariableDialog(
    existingKeys: Set<String>,
    onConfirm: (key: String, value: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加自定义属性") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { 
                        newKey = it
                        error = null
                    },
                    label = { Text("属性名") },
                    placeholder = { Text("例如: loyalty") },
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
                    placeholder = { Text("例如: 50") },
                    singleLine = true,
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
                        else -> onConfirm(newKey, newValue)
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
