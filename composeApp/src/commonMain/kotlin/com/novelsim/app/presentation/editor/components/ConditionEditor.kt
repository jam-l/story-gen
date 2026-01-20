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
    onContentChange: (NodeContent) -> Unit
) {
    var expressionType by remember { mutableStateOf(parseExpressionType(content.expression)) }
    var variableName by remember { mutableStateOf(parseVariableName(content.expression)) }
    var operator by remember { mutableStateOf(parseOperator(content.expression)) }
    var value by remember { mutableStateOf(parseValue(content.expression)) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 条件类型选择
        Text(
            text = "条件类型",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConditionTypeChip(
                label = "变量",
                selected = expressionType == ExpressionType.VARIABLE,
                onClick = { 
                    expressionType = ExpressionType.VARIABLE
                    updateExpression(expressionType, variableName, operator, value, onContentChange, content)
                }
            )
            ConditionTypeChip(
                label = "道具",
                selected = expressionType == ExpressionType.ITEM,
                onClick = { 
                    expressionType = ExpressionType.ITEM
                    updateExpression(expressionType, variableName, operator, value, onContentChange, content)
                }
            )
            ConditionTypeChip(
                label = "标记",
                selected = expressionType == ExpressionType.FLAG,
                onClick = { 
                    expressionType = ExpressionType.FLAG
                    updateExpression(expressionType, variableName, operator, value, onContentChange, content)
                }
            )
        }
        
        HorizontalDivider()
        
        // 根据类型显示不同的编辑器
        when (expressionType) {
            ExpressionType.VARIABLE -> {
                VariableConditionEditor(
                    variableName = variableName,
                    operator = operator,
                    value = value,
                    onVariableChange = { 
                        variableName = it
                        updateExpression(expressionType, it, operator, value, onContentChange, content)
                    },
                    onOperatorChange = { 
                        operator = it
                        updateExpression(expressionType, variableName, it, value, onContentChange, content)
                    },
                    onValueChange = { 
                        value = it
                        updateExpression(expressionType, variableName, operator, it, onContentChange, content)
                    }
                )
            }
            ExpressionType.ITEM -> {
                ItemConditionEditor(
                    itemId = variableName,
                    onItemChange = {
                        variableName = it
                        onContentChange(content.copy(expression = "has_item:$it"))
                    }
                )
            }
            ExpressionType.FLAG -> {
                FlagConditionEditor(
                    flagName = variableName,
                    onFlagChange = {
                        variableName = it
                        onContentChange(content.copy(expression = "flag:$it"))
                    }
                )
            }
        }
        
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
private enum class ExpressionType { VARIABLE, ITEM, FLAG }

private fun parseExpressionType(expression: String): ExpressionType {
    return when {
        expression.startsWith("has_item:") -> ExpressionType.ITEM
        expression.startsWith("flag:") -> ExpressionType.FLAG
        else -> ExpressionType.VARIABLE
    }
}

private fun parseVariableName(expression: String): String {
    return when {
        expression.startsWith("has_item:") -> expression.removePrefix("has_item:")
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

private fun updateExpression(
    type: ExpressionType,
    variableName: String,
    operator: String,
    value: String,
    onContentChange: (NodeContent) -> Unit,
    content: NodeContent.Condition
) {
    val expression = when (type) {
        ExpressionType.VARIABLE -> "$variableName $operator $value"
        ExpressionType.ITEM -> "has_item:$variableName"
        ExpressionType.FLAG -> "flag:$variableName"
    }
    onContentChange(content.copy(expression = expression))
}

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
