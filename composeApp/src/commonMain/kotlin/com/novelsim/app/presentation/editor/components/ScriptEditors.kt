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
    availableVariables: List<String> = emptyList(),
    items: List<Item> = emptyList(),
    characters: List<Character> = emptyList(),
    locations: List<Location> = emptyList(),
    onContentChange: (NodeContent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 变量类型选择
        var isEntityVariable by remember(content.variableName) { 
            mutableStateOf(content.variableName.startsWith("@")) 
        }
        
        Text("变量类型", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !isEntityVariable,
                onClick = { 
                    isEntityVariable = false
                    if (availableVariables.isNotEmpty()) {
                        onContentChange(content.copy(variableName = availableVariables.first()))
                    } else {
                        onContentChange(content.copy(variableName = ""))
                    }
                },
                label = { Text("全局变量") }
            )
            FilterChip(
                selected = isEntityVariable,
                onClick = { 
                    isEntityVariable = true
                    // Initialize with default if switching to entity (e.g. first char)
                    val defaultName = if (characters.isNotEmpty()) "@char:${characters.first().id}:" else "@char::"
                    onContentChange(content.copy(variableName = defaultName))
                },
                label = { Text("实体变量") }
            )
        }

        if (!isEntityVariable) {
            // 原有的变量名选择逻辑
            var expandedVar by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = content.variableName,
                    onValueChange = { /* 禁止编辑 */ },
                    label = { Text("变量名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true, // 禁止手动编辑
                    placeholder = { Text("请在下拉菜单中选择") },
                    trailingIcon = {
                        IconButton(onClick = { expandedVar = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "从现有变量选择")
                        }
                    }
                )
                
                DropdownMenu(
                    expanded = expandedVar,
                    onDismissRequest = { expandedVar = false },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    if (availableVariables.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("无可用变量") },
                            onClick = { expandedVar = false },
                            enabled = false
                        )
                    } else {
                        availableVariables.forEach { variable ->
                            DropdownMenuItem(
                                text = { Text(variable) },
                                onClick = {
                                    onContentChange(content.copy(variableName = variable))
                                    expandedVar = false
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // 实体变量选择器
            // Parse current: @type:id:key
            val parts = content.variableName.removePrefix("@").split(":")
            val currentType = parts.getOrNull(0) ?: "char"
            val currentId = parts.getOrNull(1) ?: ""
            val currentKey = parts.getOrNull(2) ?: ""
            
            val entityTypes = listOf("char" to "角色", "loc" to "地点", "item" to "道具")
            
            // 实体类型
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                entityTypes.forEach { (type, label) ->
                    FilterChip(
                        selected = currentType == type,
                        onClick = { 
                            // Switch type, try to pick first entity of that type
                            val newId = when(type) {
                                "char" -> characters.firstOrNull()?.id ?: ""
                                "loc" -> locations.firstOrNull()?.id ?: ""
                                "item" -> items.firstOrNull()?.id ?: ""
                                else -> ""
                            }
                            onContentChange(content.copy(variableName = "@$type:$newId:"))
                        },
                        label = { Text(label) }
                    )
                }
            }
            
            // 实体选择
            val availableEntities = when(currentType) {
                "char" -> characters.map { it.id to it.name }
                "loc" -> locations.map { it.id to it.name }
                "item" -> items.map { it.id to it.name }
                else -> emptyList()
            }
            
            var entityExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = entityExpanded,
                onExpandedChange = { entityExpanded = !entityExpanded }
            ) {
                OutlinedTextField(
                    value = availableEntities.find { it.first == currentId }?.second ?: currentId,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择实体") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entityExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = entityExpanded,
                    onDismissRequest = { entityExpanded = false }
                ) {
                    availableEntities.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onContentChange(content.copy(variableName = "@$currentType:$id:$currentKey"))
                                entityExpanded = false
                            }
                        )
                    }
                }
            }
            
            // 变量名选择
            val availableEntityVars = when(currentType) {
                "char" -> characters.find { it.id == currentId }?.variables?.keys?.toList() ?: emptyList()
                "loc" -> locations.find { it.id == currentId }?.variables?.keys?.toList() ?: emptyList()
                "item" -> items.find { it.id == currentId }?.variables?.keys?.toList() ?: emptyList()
                else -> emptyList()
            }
            
            var varExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = varExpanded,
                onExpandedChange = { varExpanded = !varExpanded }
            ) {
                OutlinedTextField(
                    value = currentKey,
                    onValueChange = { newKey ->
                         onContentChange(content.copy(variableName = "@$currentType:$currentId:$newKey"))
                    },
                    label = { Text("变量名") },
                    trailingIcon = { 
                        if (availableEntityVars.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = varExpanded) 
                        }
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    placeholder = { Text("例如: loyalty") }
                )
                if (availableEntityVars.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = varExpanded,
                        onDismissRequest = { varExpanded = false }
                    ) {
                        availableEntityVars.forEach { vName ->
                            DropdownMenuItem(
                                text = { Text(vName) },
                                onClick = {
                                    onContentChange(content.copy(variableName = "@$currentType:$currentId:$vName"))
                                    varExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
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
        // 下一节点
        NodeSelector(
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
        
        NodeSelector(
            label = "胜利后跳转到",
            selectedNodeId = content.winNextNodeId,
            availableNodes = availableNodes,
            onNodeSelect = { onContentChange(content.copy(winNextNodeId = it)) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        NodeSelector(
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
    items: List<Item> = emptyList(),
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
        if (items.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "暂无可用道具，请先在数据库中添加",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            var expandedItem by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                val selectedItem = items.find { it.id == content.itemId }
                OutlinedTextField(
                    value = selectedItem?.let { "${it.name} (${it.id})" } ?: content.itemId,
                    onValueChange = { /* 只能通过选择修改 */ },
                    label = { Text("执行道具") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true, // 只能选择
                    placeholder = { Text("例如: potion_hp") },
                    supportingText = { Text(if (content.itemId.isEmpty()) "请在下方菜单选择道具" else "ID: ${content.itemId}") },
                    trailingIcon = {
                        IconButton(onClick = { expandedItem = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择道具")
                        }
                    },
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                        expandedItem = true
                                    }
                                }
                            }
                        }
                )
                
                DropdownMenu(
                    expanded = expandedItem,
                    onDismissRequest = { expandedItem = false },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text(item.id, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                onContentChange(content.copy(
                                    itemId = item.id,
                                    itemName = item.name
                                ))
                                expandedItem = false
                            }
                        )
                    }
                }
            }
        }
        
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
        // 下一节点
        NodeSelector(
            label = "执行后跳转到",
            selectedNodeId = content.nextNodeId,
            availableNodes = availableNodes,
            onNodeSelect = { onContentChange(content.copy(nextNodeId = it)) }
        )
    }
}


/**
 * 随机分支编辑器
 */
@Composable
fun RandomEditor(
    content: NodeContent.Random,
    availableNodes: List<StoryNode>,
    onContentChange: (NodeContent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "随机分支列表 (总权重: ${content.branches.sumOf { it.weight }})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        content.branches.forEachIndexed { index, branch ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "分支 ${index + 1}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        IconButton(
                            onClick = {
                                val newBranches = content.branches.toMutableList().apply { removeAt(index) }
                                onContentChange(content.copy(branches = newBranches))
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除分支")
                        }
                    }
                    
                    // 权重
                    OutlinedTextField(
                        value = branch.weight.toString(),
                        onValueChange = { 
                            val w = it.toIntOrNull() ?: 0
                            val newBranches = content.branches.toMutableList()
                            newBranches[index] = branch.copy(weight = w.coerceAtLeast(0))
                            onContentChange(content.copy(branches = newBranches))
                        },
                        label = { Text("权重 (概率)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 跳转目标
                    NodeSelector(
                        label = "跳转到",
                        selectedNodeId = branch.nextNodeId,
                        availableNodes = availableNodes,
                        onNodeSelect = { nodeId ->
                            val newBranches = content.branches.toMutableList()
                            newBranches[index] = branch.copy(nextNodeId = nodeId)
                            onContentChange(content.copy(branches = newBranches))
                        }
                    )
                }
            }
        }
        
        OutlinedButton(
            onClick = {
                val newBranches = content.branches + RandomBranch(nextNodeId = "", weight = 10)
                onContentChange(content.copy(branches = newBranches))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加随机分支")
        }
    }
}

