package com.novelsim.app.presentation.editor.database

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.novelsim.app.presentation.editor.EditorScreenModel
// import com.novelsim.app.presentation.editor.components.HorizontalDivider // Removed incorrect import

@Composable
fun VariableListEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    val story = uiState.story ?: return
    val variables = story.variables.entries.toList()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "全局变量管理",
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("新建变量")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (variables.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无全局变量，请点击右上角新建",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "变量名 (Key)",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                "初始值 (Initial Value)",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(48.dp)) // Action space
                        }
                        HorizontalDivider()
                    }
                    
                    items(variables) { (key, value) ->
                        VariableRow(
                            name = key,
                            initialValue = value,
                            onSave = { newValue ->
                                screenModel.saveVariable(key, newValue)
                            },
                            onDelete = {
                                screenModel.deleteVariable(key)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        var newKey by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("0") }
        var error by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新建全局变量") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { 
                            newKey = it 
                            error = null
                        },
                        label = { Text("变量名 (唯一标识)") },
                        placeholder = { Text("例如: gold") },
                        isError = error != null,
                        singleLine = true
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
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isBlank()) {
                            error = "变量名不能为空"
                            return@Button
                        }
                        if (story.variables.containsKey(newKey)) {
                            error = "变量名已存在"
                            return@Button
                        }
                        screenModel.saveVariable(newKey, newValue)
                        showAddDialog = false
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun VariableRow(
    name: String,
    initialValue: String,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(initialValue) { mutableStateOf(initialValue) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Variable Name (Read-only)
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Variable Value (Editable)
        if (isEditing) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                TextButton(onClick = { 
                    onSave(editValue)
                    isEditing = false 
                }) {
                    Text("保存")
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { isEditing = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = initialValue,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        // Actions
        IconButton(onClick = { showDeleteConfirm = true }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除变量",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除变量 \"$name\" 吗？") },
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
