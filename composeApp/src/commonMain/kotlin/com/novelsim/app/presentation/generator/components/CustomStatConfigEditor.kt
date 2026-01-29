package com.novelsim.app.presentation.generator.components

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
import com.novelsim.app.domain.generator.RandomStoryGenerator.CustomStatConfig

/**
 * 自定义属性生成配置编辑器
 * 允许用户添加多条配置，每条包含：属性名、最小值、最大值
 */
@Composable
fun CustomStatConfigEditor(
    configs: List<CustomStatConfig>,
    onConfigsChange: (List<CustomStatConfig>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
                    text = "自定义属性生成配置",
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
            
            if (configs.isEmpty()) {
                Text(
                    text = "未配置自定义属性（仅生成标准属性）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    configs.forEachIndexed { index, config ->
                        CustomStatConfigRow(
                            config = config,
                            onDelete = {
                                val newConfigs = configs.toMutableList()
                                newConfigs.removeAt(index)
                                onConfigsChange(newConfigs)
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddStatConfigDialog(
            existingNames = configs.map { it.name }.toSet(),
            onConfirm = { name, min, max ->
                val newConfigs = configs.toMutableList()
                newConfigs.add(CustomStatConfig(name, min, max))
                onConfigsChange(newConfigs)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun CustomStatConfigRow(
    config: CustomStatConfig,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 属性名
        Text(
            text = config.name,
            modifier = Modifier.weight(0.4f),
            style = MaterialTheme.typography.bodyLarge
        )
        
        // 范围
        Text(
            text = "${config.min} - ${config.max}",
            modifier = Modifier.weight(0.4f),
            style = MaterialTheme.typography.bodyMedium
        )
        
        // 删除按钮
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AddStatConfigDialog(
    existingNames: Set<String>,
    onConfirm: (name: String, min: Int, max: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var minVal by remember { mutableStateOf("0") }
    var maxVal by remember { mutableStateOf("100") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加属性生成规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        error = null
                    },
                    label = { Text("属性名") },
                    placeholder = { Text("例如: sanity") },
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minVal,
                        onValueChange = { minVal = it },
                        label = { Text("最小值") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxVal,
                        onValueChange = { maxVal = it },
                        label = { Text("最大值") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val min = minVal.toIntOrNull()
                    val max = maxVal.toIntOrNull()
                    
                    when {
                        name.isBlank() -> error = "属性名不能为空"
                        existingNames.contains(name) -> error = "属性名已存在"
                        min == null || max == null -> error = "必须输入有效的整数"
                        min > max -> error = "最小值不能大于最大值"
                        else -> onConfirm(name, min, max)
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
