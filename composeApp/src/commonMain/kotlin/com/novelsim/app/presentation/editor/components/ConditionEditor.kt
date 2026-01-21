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
    onContentChange: (NodeContent) -> Unit
) {
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConditionExpressionEditor(
            expression = content.expression,
            clues = clues,
            factions = factions,
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
    onExpressionChange: (String) -> Unit
) {
    var expressionType by remember(expression) { mutableStateOf(parseExpressionType(expression)) }
    var variableName by remember(expression) { mutableStateOf(parseVariableName(expression)) }
    var operator by remember(expression) { mutableStateOf(parseOperator(expression)) }
    var value by remember(expression) { mutableStateOf(parseValue(expression)) }

    // Helper to constructing expression string and notify change
    fun update() {
        val newExpr = when (expressionType) {
            ExpressionType.VARIABLE -> "$variableName $operator $value"
            ExpressionType.ITEM -> "has_item:$variableName"
            ExpressionType.CLUE -> "has_clue:$variableName"
            ExpressionType.FACTION -> "reputation:$variableName $operator $value"
            ExpressionType.FLAG -> "flag:$variableName"
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
                expressionType = ExpressionType.VARIABLE; update() 
            }
            ConditionTypeChip("道具", expressionType == ExpressionType.ITEM) { 
                expressionType = ExpressionType.ITEM; update() 
            }
            ConditionTypeChip("标记", expressionType == ExpressionType.FLAG) { 
                expressionType = ExpressionType.FLAG; update() 
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             ConditionTypeChip("线索", expressionType == ExpressionType.CLUE) { 
                expressionType = ExpressionType.CLUE; update() 
            }
            ConditionTypeChip("阵营", expressionType == ExpressionType.FACTION) { 
                expressionType = ExpressionType.FACTION; update() 
            }
        }
        
        HorizontalDivider()
        
        when {
            expressionType == ExpressionType.VARIABLE -> {
                VariableConditionEditor(
                    variableName = variableName,
                    operator = operator,
                    value = value,
                    onVariableChange = { variableName = it; update() },
                    onOperatorChange = { operator = it; update() },
                    onValueChange = { value = it; update() }
                )
            }
            expressionType == ExpressionType.ITEM -> {
                ItemConditionEditor(
                    itemId = variableName,
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
    onVariableChange: (String) -> Unit,
    onOperatorChange: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = variableName,
            onValueChange = onVariableChange,
            label = { Text("变量名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("例如: player_level") }
        )
        
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
    onItemChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "检查玩家是否拥有指定道具",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = itemId,
            onValueChange = onItemChange,
            label = { Text("道具 ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("例如: key_gold") }
        )
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

@Composable
private fun NodeSelector(
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
                if (availableNodes.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("没有可用节点") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    availableNodes.forEach { node ->
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
                    }
                }
            }
        }
    }
}

// 辅助函数
private enum class ExpressionType { VARIABLE, ITEM, CLUE, FACTION, FLAG }

private fun parseExpressionType(expression: String): ExpressionType {
    return when {
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
    val parts = expression.split(Regex("[><=!]+"))
    return if (parts.size > 1) parts.last().trim() else ""
}

// private fun updateExpression removed as it is now local inside ConditionExpressionEditor

// ) {
// Removed
// }

private fun getNodePreview(node: StoryNode): String {
    return when (val c = node.content) {
        is NodeContent.Dialogue -> c.text.take(30) + if (c.text.length > 30) "..." else ""
        is NodeContent.Choice -> c.prompt.take(30)
        is NodeContent.Condition -> "条件: ${c.expression}"
        is NodeContent.Battle -> "战斗"
        is NodeContent.ItemAction -> "${c.action}: ${c.itemId}"
        is NodeContent.VariableAction -> "${c.variableName} ${c.operation}"
        is NodeContent.Ending -> "结局: ${c.title}"
    }
}

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
        
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = clueId,
                onValueChange = onClueChange,
                label = { Text("线索 ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                     IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择线索")
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
        
         Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = factionId,
                onValueChange = onFactionChange,
                label = { Text("阵营 ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                     IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择阵营")
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
