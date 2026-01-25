package com.novelsim.app.presentation.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.*

/**
 * 变量操作编辑器
 */
@Composable
fun VariableEditor(
    content: NodeContent.VariableAction,
    availableNodes: List<StoryNode>,
    onContentChange: (NodeContent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 变量名
        OutlinedTextField(
            value = content.variableName,
            onValueChange = { onContentChange(content.copy(variableName = it)) },
            label = { Text("变量名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("例如: player_score") }
        )
        
        // 操作类型 (Dropdown)
        Text(
            text = "操作类型",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
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
                    Text(
                        text = when (content.operation) {
                            VariableOperation.SET -> "设置 (=)"
                            VariableOperation.ADD -> "增加 (+=)"
                            VariableOperation.SUBTRACT -> "减少 (-=)"
                            VariableOperation.MULTIPLY -> "乘以 (*=)"
                            VariableOperation.DIVIDE -> "除以 (/=)"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                VariableOperation.entries.forEach { operation ->
                    DropdownMenuItem(
                        text = { 
                            Text(when (operation) {
                                VariableOperation.SET -> "设置 (=)"
                                VariableOperation.ADD -> "增加 (+=)"
                                VariableOperation.SUBTRACT -> "减少 (-=)"
                                VariableOperation.MULTIPLY -> "乘以 (*=)"
                                VariableOperation.DIVIDE -> "除以 (/=)"
                            })
                        },
                        onClick = {
                            onContentChange(content.copy(operation = operation))
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // 值
        OutlinedTextField(
            value = content.value,
            onValueChange = { onContentChange(content.copy(value = it)) },
            label = { Text("值") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("数值或变量名") }
        )
        
        // 预览
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildPreviewText(content),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        HorizontalDivider()
        
        // 下一节点
        NodeSelectorSimple(
            label = "执行后跳转到",
            selectedNodeId = content.nextNodeId,
            availableNodes = availableNodes,
            onNodeSelect = { onContentChange(content.copy(nextNodeId = it)) }
        )
    }
}

private fun buildPreviewText(content: NodeContent.VariableAction): String {
    val opSymbol = when (content.operation) {
        VariableOperation.SET -> "="
        VariableOperation.ADD -> "+="
        VariableOperation.SUBTRACT -> "-="
        VariableOperation.MULTIPLY -> "*="
        VariableOperation.DIVIDE -> "/="
    }
    return "${content.variableName} $opSymbol ${content.value}"
}

/**
 * 战斗节点编辑器
 */
/**
 * 战斗节点编辑器
 */
@Composable
fun BattleEditor(
    content: NodeContent.Battle,
    availableNodes: List<StoryNode>,
    enemies: List<Enemy>,
    onContentChange: (NodeContent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 敌人选择
        Text(
            text = "选择敌人",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        var expanded by remember { mutableStateOf(false) }
        val selectedEnemy = enemies.find { it.id == content.enemyId }
        
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
                    Text(
                        text = selectedEnemy?.name ?: "未选择 (ID: ${content.enemyId})",
                        color = if (selectedEnemy == null) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                if (enemies.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("无可用怪物，请在数据库创建") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                }
                enemies.forEach { enemy ->
                    DropdownMenuItem(
                        text = { Text("${enemy.name} (Lv.${enemy.stats.level})") },
                        onClick = {
                            onContentChange(content.copy(enemyId = enemy.id))
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // 显示选中敌人的简要信息
        if (selectedEnemy != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = selectedEnemy.description.ifEmpty { "暂无描述" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HP: ${selectedEnemy.stats.maxHp} / ATK: ${selectedEnemy.stats.attack} / DEF: ${selectedEnemy.stats.defense}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        HorizontalDivider()
        
        // 战斗结果分支
        Text(
            text = "战斗结果分支",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        NodeSelectorSimple(
            label = "胜利后跳转到",
            selectedNodeId = content.winNextNodeId,
            availableNodes = availableNodes,
            onNodeSelect = { onContentChange(content.copy(winNextNodeId = it)) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        NodeSelectorSimple(
            label = "失败后跳转到",
            selectedNodeId = content.loseNextNodeId,
            availableNodes = availableNodes,
            onNodeSelect = { onContentChange(content.copy(loseNextNodeId = it)) }
        )
    }
}

/**
 * 道具操作编辑器
 */
@Composable
fun ItemActionEditor(
    content: NodeContent.ItemAction,
    availableNodes: List<StoryNode>,
    onContentChange: (NodeContent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 操作类型 (Dropdown)
        Text(
            text = "操作类型",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
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
                    Text(
                        text = when (content.action) {
                            ItemActionType.GIVE -> "给予 (获得)"
                            ItemActionType.REMOVE -> "移除 (失去)"
                            ItemActionType.CHECK -> "检查 (是否拥有)"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                ItemActionType.entries.forEach { actionType ->
                    DropdownMenuItem(
                        text = { 
                            Text(when (actionType) {
                                ItemActionType.GIVE -> "给予 (获得)"
                                ItemActionType.REMOVE -> "移除 (失去)"
                                ItemActionType.CHECK -> "检查 (是否拥有)"
                            })
                        },
                        onClick = {
                            onContentChange(content.copy(action = actionType))
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // 道具 ID
        OutlinedTextField(
            value = content.itemId,
            onValueChange = { onContentChange(content.copy(itemId = it)) },
            label = { Text("道具 ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("例如: potion_hp") }
        )
        
        // 数量
        OutlinedTextField(
            value = content.quantity.toString(),
            onValueChange = { 
                val qty = it.toIntOrNull() ?: 1
                onContentChange(content.copy(quantity = qty.coerceAtLeast(1)))
            },
            label = { Text("数量") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        HorizontalDivider()
        
        // 下一节点
        NodeSelectorSimple(
            label = "执行后跳转到",
            selectedNodeId = content.nextNodeId,
            availableNodes = availableNodes,
            onNodeSelect = { onContentChange(content.copy(nextNodeId = it)) }
        )
    }
}

/**
 * 简化版节点选择器
 */
@Composable
fun NodeSelectorSimple(
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
                    Text(
                        text = if (selectedNodeId.isEmpty()) "未选择" else selectedNodeId,
                        color = if (selectedNodeId.isEmpty()) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("无 (清除选择)") },
                    onClick = {
                        onNodeSelect("")
                        expanded = false
                    }
                )
                HorizontalDivider()
                availableNodes.forEach { node ->
                    DropdownMenuItem(
                        text = { Text("${node.id} (${node.type.name})") },
                        onClick = {
                            onNodeSelect(node.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
