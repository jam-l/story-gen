package com.novelsim.app.presentation.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novelsim.app.data.model.NodeContent
import com.novelsim.app.data.model.NodeType
import com.novelsim.app.presentation.editor.EditorScreenModel.EditorNode

/**
 * 节点列表编辑器
 */
@Composable
fun NodeListEditor(
    nodes: List<EditorNode>,
    enemies: List<com.novelsim.app.data.model.Enemy> = emptyList(),
    onSelectNode: (String) -> Unit,
    onDeleteNode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(nodes, key = { it.node.id }) { editorNode ->
            NodeListItem(
                node = editorNode,
                enemies = enemies,
                onClick = { onSelectNode(editorNode.node.id) },
                onDelete = { onDeleteNode(editorNode.node.id) }
            )
        }
    }
}

@Composable
private fun NodeListItem(
    node: EditorNode,
    enemies: List<com.novelsim.app.data.model.Enemy>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val storyNode = node.node
    val containerColor = when {
        node.isSelected -> MaterialTheme.colorScheme.primaryContainer
        storyNode.type == NodeType.END -> Color(0xFFFFE0E0) // 淡红色
        storyNode.type == NodeType.CHOICE -> Color(0xFFE0F7FA) // 淡青色
        storyNode.type == NodeType.BATTLE -> Color(0xFFFFEBEE) // 淡粉色
        else -> MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标/标签
            Badge(
                containerColor = when {
                    storyNode.id == "start" -> MaterialTheme.colorScheme.primary
                    storyNode.type == NodeType.END -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                }
            ) {
                Text(
                    text = if (storyNode.id == "start") "始" else storyNode.type.name.take(1),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 内容摘要
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = storyNode.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = getNodeSummary(storyNode.content, enemies),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 操作按钮
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
            }
            
            if (storyNode.id != "start") {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun getNodeSummary(content: NodeContent, enemies: List<com.novelsim.app.data.model.Enemy>): String {
    return when (content) {
        is NodeContent.Dialogue -> "${content.speaker ?: "旁白"}: ${content.text}"
        is NodeContent.Choice -> "选择: ${content.prompt}"
        is NodeContent.Ending -> "结局: ${content.title}"
        is NodeContent.Battle -> "战斗: ${enemies.find { it.id == content.enemyId }?.name ?: content.enemyId}"
        is NodeContent.Condition -> "条件: ${content.expression}"
        is NodeContent.ItemAction -> "物品: ${content.action} ${content.itemId}"
        is NodeContent.VariableAction -> "变量: ${content.variableName} ${content.operation} ${content.value}"
    }
}
