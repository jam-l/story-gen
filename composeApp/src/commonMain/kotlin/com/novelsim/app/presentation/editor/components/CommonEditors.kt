package com.novelsim.app.presentation.editor.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.*

@Composable
fun StatInput(
    label: String, 
    value: Int, 
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    
    OutlinedTextField(
        value = text,
        onValueChange = { 
            text = it
            if (it.isNotEmpty() && it.all { char -> char.isDigit() }) {
                onValueChange(it.toInt())
            }
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true
    )
}

/**
 * 通用的节点选择器，显示节点详情预览
 */
@Composable
fun NodeSelector(
    label: String,
    selectedNodeId: String,
    availableNodes: List<StoryNode>,
    onNodeSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedNode = availableNodes.find { it.id == selectedNodeId }
    
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Box {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedNode != null) {
                            Text(
                                text = selectedNode.id,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = getNodePreview(selectedNode),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = if (selectedNodeId.isEmpty()) "未选择" else selectedNodeId,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                // 清除选项
                DropdownMenuItem(
                    text = { Text("无 (清除选择)", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        onNodeSelect("")
                        expanded = false
                    }
                )
                
                if (availableNodes.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("没有可用节点") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                     // 限制最大显示数量，防止卡顿
                    val displayNodes = availableNodes.take(50) 
                    
                    displayNodes.forEach { node ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(node.id, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = getNodePreview(node),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onNodeSelect(node.id)
                                expanded = false
                            }
                        )
                        HorizontalDivider()
                    }
                    
                    if (availableNodes.size > 50) {
                         DropdownMenuItem(
                            text = { Text("...以及更多 (${availableNodes.size - 50} 个)") },
                            onClick = { },
                            enabled = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * 获取节点内容预览文本
 */
fun getNodePreview(node: StoryNode): String {
    return when (val c = node.content) {
        is NodeContent.Dialogue -> c.text.take(30) + if (c.text.length > 30) "..." else ""
        is NodeContent.Choice -> c.prompt.take(30)
        is NodeContent.Condition -> "条件: ${c.expression}"
        is NodeContent.Battle -> "战斗: ${c.enemyId}"
        is NodeContent.ItemAction -> "${c.action}: ${c.itemId} x${c.quantity}"
        is NodeContent.VariableAction -> "${c.variableName} ${c.operation}"
        is NodeContent.Random -> "随机: ${c.branches.size} 个分支"
        is NodeContent.Ending -> "结局: ${c.title}"
    }
}
