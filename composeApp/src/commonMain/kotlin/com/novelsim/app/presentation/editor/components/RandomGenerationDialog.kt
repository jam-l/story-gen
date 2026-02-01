package com.novelsim.app.presentation.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.source.NameTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomGenerationDialog(
    title: String,
    templates: List<NameTemplate>,
    onDismissRequest: () -> Unit,
    onConfirm: (templateId: String?, count: Int) -> Unit
) {
    var count by remember { mutableStateOf(1) }
    var selectedTemplate by remember { mutableStateOf<NameTemplate?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 数量选择
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("生成数量")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (count > 1) count-- },
                            enabled = count > 1
                        ) {
                            Text("-", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            text = count.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (count < 20) count++ }, // 上限20
                            enabled = count < 20
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "增加")
                        }
                    }
                }

                // 模板选择
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedTemplate?.let { "${it.id} (${it.description})" } ?: "随机模版",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择模版") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("随机模版") },
                            onClick = {
                                selectedTemplate = null
                                expanded = false
                            }
                        )
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(template.id)
                                        if (template.description.isNotEmpty()) {
                                            Text(template.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedTemplate = template
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // 提示
                if (selectedTemplate != null) {
                    Text(
                        text = "将使用模版: ${selectedTemplate?.description ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedTemplate?.id, count) }) {
                Text("生成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
