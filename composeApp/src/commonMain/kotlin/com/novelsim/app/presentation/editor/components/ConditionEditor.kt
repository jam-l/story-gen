package com.novelsim.app.presentation.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.*

/**
 * 条件编辑器 - 用于编辑条件节点
 */
@Composable
fun ConditionEditor(
    content: NodeContent.Condition,
    availableNodes: List<StoryNode>,
    clues: List<Clue> = emptyList(),
    factions: List<Faction> = emptyList(),
    characters: List<Character> = emptyList(),
    locations: List<Location> = emptyList(),
    items: List<Item> = emptyList(),
    variables: Map<String, String> = emptyMap(),
    onContentChange: (NodeContent) -> Unit
) {
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConditionExpressionEditor(
            expression = content.expression,
            clues = clues,
            factions = factions,
            items = items,
            characters = characters,
            locations = locations,
            variables = variables.keys.toList(),
            onExpressionChange = { newExpression ->
                onContentChange(content.copy(expression = newExpression))
            }
        )

        HorizontalDivider()
        
        // 目标节点选择
        Text(
            text = "分支目标",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        HorizontalDivider()
        
        
        // 目标节点选择
        Text(
            text = "分支目标",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        NodeSelector(
            label = "条件为真时跳转到",
            selectedNodeId = content.trueNextNodeId,
            availableNodes = availableNodes,
            onNodeSelect = { onContentChange(content.copy(trueNextNodeId = it)) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        NodeSelector(
            label = "条件为假时跳转到",
            selectedNodeId = content.falseNextNodeId,
            availableNodes = availableNodes,
            onNodeSelect = { onContentChange(content.copy(falseNextNodeId = it)) }
        )
    }
}

/**
 * 独立的条件表达式编辑器，可复用
 */
@Composable
fun ConditionExpressionEditor(
    expression: String,
    clues: List<Clue> = emptyList(),
    factions: List<Faction> = emptyList(),
    items: List<Item> = emptyList(),
    characters: List<Character> = emptyList(),
    locations: List<Location> = emptyList(),
    variables: List<String> = emptyList(),
    onExpressionChange: (String) -> Unit
) {
    var expressionType by remember(expression) { mutableStateOf(parseExpressionType(expression)) }
    var variableName by remember(expression) { mutableStateOf(parseVariableName(expression)) }
    var operator by remember(expression) { mutableStateOf(parseOperator(expression)) }
    var value by remember(expression) { mutableStateOf(parseValue(expression)) }

    // Helper to constructing expression string and notify change
    // For ENTITY_VAR, we use extra state
    var entityType by remember(expression) { mutableStateOf(parseEntityType(expression)) }
    var entityId by remember(expression) { mutableStateOf(parseEntityId(expression)) }
    var entityVarKey by remember(expression) { mutableStateOf(parseEntityVarKey(expression)) }

    fun update() {
        val newExpr = when (expressionType) {
            ExpressionType.VARIABLE -> "$variableName $operator $value"
            ExpressionType.ITEM -> "has_item:$variableName"
            ExpressionType.CLUE -> "has_clue:$variableName"
            ExpressionType.FACTION -> "reputation:$variableName $operator $value"
            ExpressionType.FLAG -> "flag:$variableName"
            ExpressionType.ENTITY_VAR -> "@$entityType:$entityId:$entityVarKey $operator $value"
        }
        onExpressionChange(newExpr)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 条件类型选择
        Text(
            text = "条件类型",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConditionTypeChip("变量", expressionType == ExpressionType.VARIABLE) { 
                expressionType = ExpressionType.VARIABLE
                if (variables.isNotEmpty()) { variableName = variables.first() }
                update() 
            }
            ConditionTypeChip("道具", expressionType == ExpressionType.ITEM) { 
                expressionType = ExpressionType.ITEM
                if (items.isNotEmpty()) { variableName = items.first().id }
                update() 
            }
            ConditionTypeChip("标记", expressionType == ExpressionType.FLAG) { 
                expressionType = ExpressionType.FLAG
                // For flags, we don't have a list to select from, keep current or empty
                update() 
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             ConditionTypeChip("线索", expressionType == ExpressionType.CLUE) { 
                expressionType = ExpressionType.CLUE
                if (clues.isNotEmpty()) { variableName = clues.first().id }
                update() 
            }
            ConditionTypeChip("阵营", expressionType == ExpressionType.FACTION) { 
                expressionType = ExpressionType.FACTION
                if (factions.isNotEmpty()) { variableName = factions.first().id }
                update() 
            }
            ConditionTypeChip("实体变量", expressionType == ExpressionType.ENTITY_VAR) {
                expressionType = ExpressionType.ENTITY_VAR
                entityType = "char"
                if (characters.isNotEmpty()) { entityId = characters.first().id }
                update()
            }
        }
        
        HorizontalDivider()
        
        when {
            expressionType == ExpressionType.VARIABLE -> {
                VariableConditionEditor(
                    variableName = variableName,
                    operator = operator,
                    value = value,
                    availableVariables = variables,
                    onVariableChange = { variableName = it; update() },
                    onOperatorChange = { operator = it; update() },
                    onValueChange = { value = it; update() }
                )
            }
            expressionType == ExpressionType.ITEM -> {
                ItemConditionEditor(
                    itemId = variableName,
                    items = items,
                    onItemChange = { variableName = it; update() }
                )
            }
            expressionType == ExpressionType.CLUE -> {
                ClueConditionEditor(
                    clueId = variableName,
                    clues = clues,
                    onClueChange = { variableName = it; update() }
                )
            }
            expressionType == ExpressionType.FACTION -> {
                FactionConditionEditor(
                    factionId = variableName,
                    operator = operator,
                    value = value,
                    factions = factions,
                    onFactionChange = { variableName = it; update() },
                    onOperatorChange = { operator = it; update() },
                    onValueChange = { value = it; update() }
                )
            }
            expressionType == ExpressionType.FLAG -> {
                FlagConditionEditor(
                    flagName = variableName,
                    onFlagChange = { variableName = it; update() }
                )
            }
            expressionType == ExpressionType.ENTITY_VAR -> {
                EntityVariableConditionEditor(
                    entityType = entityType,
                    entityId = entityId,
                    variableKey = entityVarKey,
                    operator = operator,
                    value = value,
                    characters = characters,
                    locations = locations,
                    items = items,
                    onEntityTypeChange = { entityType = it; update() },
                    onEntityIdChange = { entityId = it; update() },
                    onVariableKeyChange = { entityVarKey = it; update() },
                    onOperatorChange = { operator = it; update() },
                    onValueChange = { value = it; update() }
                )
            }
        }
    }
}

@Composable
private fun ConditionTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}

@Composable
private fun VariableConditionEditor(
    variableName: String,
    operator: String,
    value: String,
    availableVariables: List<String>,
    onVariableChange: (String) -> Unit,
    onOperatorChange: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var expanded by remember { mutableStateOf(false) }
        
        if (availableVariables.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "暂无可用变量，请先定义变量",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = variableName,
                    onValueChange = {}, // 禁止手动输入内容
                    label = { Text("变量名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true, // 禁止手动编辑
                    placeholder = { Text("请点击右侧图标选择") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择现有变量")
                        }
                    },
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                        expanded = true
                                    }
                                }
                            }
                        }
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    availableVariables.forEach { variable ->
                        DropdownMenuItem(
                            text = { Text(variable) },
                            onClick = {
                                onVariableChange(variable)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 运算符选择
            val operators = listOf(">" to "大于", ">=" to "大于等于", "==" to "等于", "!=" to "不等于", "<" to "小于", "<=" to "小于等于")
            
            var expanded by remember { mutableStateOf(false) }
            
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(operators.find { it.first == operator }?.second ?: "选择运算符")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    operators.forEach { (op, label) ->
                        DropdownMenuItem(
                            text = { Text("$label ($op)") },
                            onClick = {
                                onOperatorChange(op)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("比较值") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("例如: 10") }
        )
    }
}

@Composable
private fun ItemConditionEditor(
    itemId: String,
    items: List<Item>,
    onItemChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "检查玩家是否拥有指定道具",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        var expanded by remember { mutableStateOf(false) }
        
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
            Box(modifier = Modifier.fillMaxWidth()) {
                val selectedItem = items.find { it.id == itemId }
                OutlinedTextField(
                    value = selectedItem?.let { "${it.name} (${it.id})" } ?: itemId,
                    onValueChange = onItemChange,
                    label = { Text("道具") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true, // 只能选择
                    placeholder = { Text("例如: key_gold") },
                    supportingText = { Text(if (itemId.isEmpty()) "请在菜单中选择" else "ID: $itemId") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择道具")
                        }
                    },
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                        expanded = true
                                    }
                                }
                            }
                        }
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.7f)
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
                                onItemChange(item.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagConditionEditor(
    flagName: String,
    onFlagChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "检查指定标记是否已设置",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = flagName,
            onValueChange = onFlagChange,
            label = { Text("标记名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("例如: boss_defeated") }
        )
    }
}



// 辅助函数
private enum class ExpressionType { VARIABLE, ITEM, CLUE, FACTION, FLAG, ENTITY_VAR }

private fun parseExpressionType(expression: String): ExpressionType {
    return when {
        expression.startsWith("@") -> ExpressionType.ENTITY_VAR
        expression.startsWith("has_item:") -> ExpressionType.ITEM
        expression.startsWith("has_clue:") -> ExpressionType.CLUE
        expression.startsWith("reputation:") -> ExpressionType.FACTION
        expression.startsWith("flag:") -> ExpressionType.FLAG
        else -> ExpressionType.VARIABLE
    }
}

private fun parseVariableName(expression: String): String {
    return when {
        expression.startsWith("has_item:") -> expression.removePrefix("has_item:")
        expression.startsWith("has_clue:") -> expression.removePrefix("has_clue:")
        expression.startsWith("reputation:") -> {
             val part = expression.removePrefix("reputation:")
             part.split(Regex("[><=!]+")).firstOrNull()?.trim() ?: ""
        }
        expression.startsWith("flag:") -> expression.removePrefix("flag:")
        else -> expression.split(Regex("[><=!]+")).firstOrNull()?.trim() ?: ""
    }
}

private fun parseOperator(expression: String): String {
    val match = Regex("(>=|<=|==|!=|>|<)").find(expression)
    return match?.value ?: ">"
}

private fun parseValue(expression: String): String {
    val parts = expression.split(Regex("[><!=]+"))
    return if (parts.size > 1) parts.last().trim() else ""
}

// 解析实体变量表达式 @type:id:key 格式
private fun parseEntityType(expression: String): String {
    if (!expression.startsWith("@")) return "char"
    val parts = expression.removePrefix("@").split(":")
    return parts.getOrNull(0) ?: "char"
}

private fun parseEntityId(expression: String): String {
    if (!expression.startsWith("@")) return ""
    val parts = expression.removePrefix("@").split(":")
    return parts.getOrNull(1) ?: ""
}

private fun parseEntityVarKey(expression: String): String {
    if (!expression.startsWith("@")) return ""
    val parts = expression.removePrefix("@").split(":")
    // key 可能包含空格和操作符，需要分割
    val keyPart = parts.getOrNull(2) ?: return ""
    return keyPart.split(Regex("[><!=]+")).firstOrNull()?.trim() ?: ""
}

// private fun updateExpression removed as it is now local inside ConditionExpressionEditor

// ) {
// Removed
// }



@Composable
private fun ClueConditionEditor(
    clueId: String,
    clues: List<Clue>,
    onClueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "检查玩家是否已获得线索",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
    if (clues.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "暂无可用线索，请先在数据库中添加",
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            val selectedClue = clues.find { it.id == clueId }
            OutlinedTextField(
                value = selectedClue?.let { "${it.name} (${it.id})" } ?: clueId,
                onValueChange = onClueChange,
                label = { Text("线索") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true, // 只能选择
                supportingText = { Text(if (clueId.isEmpty()) "请选择线索" else "ID: $clueId") },
                trailingIcon = {
                     IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择线索")
                    }
                },
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    expanded = true
                                }
                            }
                        }
                    }
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                clues.forEach { clue ->
                    DropdownMenuItem(
                        text = { Text(clue.name) },
                        onClick = {
                            onClueChange(clue.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun FactionConditionEditor(
    factionId: String,
    operator: String,
    value: String,
    factions: List<Faction>,
    onFactionChange: (String) -> Unit,
    onOperatorChange: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "检查阵营声望值",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (factions.isEmpty()) {
             Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "暂无可用阵营，请先在数据库中添加",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
             Box(modifier = Modifier.fillMaxWidth()) {
                val selectedFaction = factions.find { it.id == factionId }
                OutlinedTextField(
                    value = selectedFaction?.let { "${it.name} (${it.id})" } ?: factionId,
                    onValueChange = onFactionChange,
                    label = { Text("阵营") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true, // 只能选择
                    supportingText = { Text(if (factionId.isEmpty()) "请选择阵营" else "ID: $factionId") },
                    trailingIcon = {
                         IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择阵营")
                        }
                    },
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                        expanded = true
                                    }
                                }
                            }
                        }
                )
                 DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    factions.forEach { faction ->
                        DropdownMenuItem(
                            text = { Text(faction.name) },
                            onClick = {
                                onFactionChange(faction.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
         Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 运算符选择
            var opExpanded by remember { mutableStateOf(false) }
            val operators = listOf(">" to "大于", ">=" to "大于等于", "==" to "等于", "<" to "小于", "<=" to "小于等于")
            
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { opExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(operators.find { it.first == operator }?.second ?: "运算符")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                DropdownMenu(
                    expanded = opExpanded,
                    onDismissRequest = { opExpanded = false }
                ) {
                    operators.forEach { (op, label) ->
                        DropdownMenuItem(
                            text = { Text("$label ($op)") },
                            onClick = {
                                onOperatorChange(op)
                                opExpanded = false
                            }
                        )
                    }
                }
            }
            
             OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("声望值") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

/**
 * 实体变量条件编辑器
 * 用于编辑 @type:id:key 格式的实体变量条件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityVariableConditionEditor(
    entityType: String,
    entityId: String,
    variableKey: String,
    operator: String,
    value: String,
    characters: List<Character>,
    locations: List<Location>,
    items: List<Item>,
    onEntityTypeChange: (String) -> Unit,
    onEntityIdChange: (String) -> Unit,
    onVariableKeyChange: (String) -> Unit,
    onOperatorChange: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    val entityTypes = listOf(
        "char" to "角色",
        "loc" to "地点",
        "item" to "道具"
    )
    
    // 根据选中的实体类型获取可选实体列表
    val availableEntities = when (entityType) {
        "char" -> characters.map { it.id to it.name }
        "loc" -> locations.map { it.id to it.name }
        "item" -> items.map { it.id to it.name }
        else -> emptyList()
    }
    
    // 获取选中实体的变量列表
    val availableVariables = when (entityType) {
        "char" -> characters.find { it.id == entityId }?.variables?.keys?.toList() ?: emptyList()
        "loc" -> locations.find { it.id == entityId }?.variables?.keys?.toList() ?: emptyList()
        "item" -> items.find { it.id == entityId }?.variables?.keys?.toList() ?: emptyList()
        else -> emptyList()
    }
    
    val operators = listOf(
        ">" to "大于",
        "<" to "小于",
        ">=" to "大于等于",
        "<=" to "小于等于",
        "==" to "等于",
        "!=" to "不等于"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 实体类型选择
        Text("实体类型", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            entityTypes.forEach { (type, label) ->
                FilterChip(
                    selected = entityType == type,
                    onClick = { onEntityTypeChange(type) },
                    label = { Text(label) }
                )
            }
        }
        
        // 实体选择
        var entityExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = entityExpanded,
            onExpandedChange = { entityExpanded = !entityExpanded }
        ) {
            OutlinedTextField(
                value = availableEntities.find { it.first == entityId }?.second ?: entityId,
                onValueChange = {},
                readOnly = true,
                label = { Text("选择${entityTypes.find { it.first == entityType }?.second ?: "实体"}") },
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
                            onEntityIdChange(id)
                            entityExpanded = false
                        }
                    )
                }
            }
        }
        
        // 变量选择
        var varExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = varExpanded,
            onExpandedChange = { varExpanded = !varExpanded }
        ) {
            OutlinedTextField(
                value = variableKey,
                onValueChange = onVariableKeyChange,
                label = { Text("变量名") },
                trailingIcon = { 
                    if (availableVariables.isNotEmpty()) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = varExpanded) 
                    }
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                placeholder = { Text("例如: loyalty, durability") }
            )
            if (availableVariables.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = varExpanded,
                    onDismissRequest = { varExpanded = false }
                ) {
                    availableVariables.forEach { varName ->
                        DropdownMenuItem(
                            text = { Text(varName) },
                            onClick = {
                                onVariableKeyChange(varName)
                                varExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        // 操作符和值
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var opExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = operators.find { it.first == operator }?.second ?: operator,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("操作符") },
                    modifier = Modifier.fillMaxWidth().clickable { opExpanded = true }
                )
                DropdownMenu(
                    expanded = opExpanded,
                    onDismissRequest = { opExpanded = false }
                ) {
                    operators.forEach { (op, label) ->
                        DropdownMenuItem(
                            text = { Text("$label ($op)") },
                            onClick = {
                                onOperatorChange(op)
                                opExpanded = false
                            }
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("值") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

