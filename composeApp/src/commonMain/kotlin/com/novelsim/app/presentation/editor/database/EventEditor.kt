package com.novelsim.app.presentation.editor.database

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.*
import com.novelsim.app.presentation.editor.components.ConditionExpressionEditor
import com.novelsim.app.presentation.editor.EditorScreenModel
import com.novelsim.app.util.PlatformUtils


@Composable
fun EventEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(uiState.events) {
        if (selectedEventId != null && uiState.events.none { it.id == selectedEventId }) {
            selectedEventId = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：事件列表
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = {
                        val newEvent = GameEvent(
                            id = "evt_${PlatformUtils.getCurrentTimeMillis()}",
                            name = "新事件",
                            description = "",
                            startNodeId = "start" // 默认关联开始节点，实际应选择
                        )
                        screenModel.saveEvent(newEvent)
                        selectedEventId = newEvent.id
                    },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("添加事件")
                }

                HorizontalDivider()

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.events) { event ->
                        ListItem(
                            headlineContent = { Text(event.name) },
                            supportingContent = { 
                                Text(
                                    text = "条件: ${event.triggerCondition ?: "无"}", 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            modifier = Modifier
                                .clickable { selectedEventId = event.id }
                                .let {
                                    if (selectedEventId == event.id) {
                                        it.background(MaterialTheme.colorScheme.primaryContainer)
                                    } else it
                                }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        // 右侧：编辑区域
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            if (selectedEventId != null) {
                val event = uiState.events.find { it.id == selectedEventId }
                if (event != null) {
                    EventDetailEditor(
                        event = event,
                        clues = uiState.clues,
                        factions = uiState.factions,
                        onSave = { screenModel.saveEvent(it) },
                        onDelete = { 
                            screenModel.deleteEvent(it) 
                            selectedEventId = null
                        }
                    )
                }
            } else {
                Text(
                    text = "请选择或新建一个事件",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun EventDetailEditor(
    event: GameEvent,
    clues: List<Clue> = emptyList(),
    factions: List<Faction> = emptyList(),
    onSave: (GameEvent) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(event) { mutableStateOf(event.name) }
    var description by remember(event) { mutableStateOf(event.description) }
    var triggerCondition by remember(event) { mutableStateOf(event.triggerCondition ?: "") }
    var priority by remember(event) { mutableStateOf(event.priority) }
    var isRepeatable by remember(event) { mutableStateOf(event.isRepeatable) }
    var startNodeId by remember(event) { mutableStateOf(event.startNodeId) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题与删除按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "编辑事件",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("删除事件")
            }
        }

        // 基本信息
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("基本信息", style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        onSave(event.copy(name = it))
                    },
                    label = { Text("事件名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        onSave(event.copy(description = it))
                    },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }
        
        // 触发逻辑
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("触发逻辑", style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(
                    value = startNodeId ?: "",
                    onValueChange = { 
                        startNodeId = it
                        onSave(event.copy(startNodeId = it.ifEmpty { "start" }))
                    },
                    label = { Text("起始节点 ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                HorizontalDivider()
                
                Text("触发条件", style = MaterialTheme.typography.bodyMedium)
                
                ConditionExpressionEditor(
                    expression = triggerCondition,
                    clues = clues,
                    factions = factions,
                    onExpressionChange = { 
                        triggerCondition = it
                        onSave(event.copy(triggerCondition = it.ifEmpty { null }))
                    }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("优先级")
                     OutlinedTextField(
                        value = priority.toString(),
                        onValueChange = { 
                            if (it.all { c -> c.isDigit() }) {
                                val p = it.toIntOrNull() ?: 0
                                priority = p
                                onSave(event.copy(priority = p))
                            }
                        },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRepeatable,
                        onCheckedChange = { 
                            isRepeatable = it
                            onSave(event.copy(isRepeatable = it))
                        }
                    )
                    Text("可重复触发")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除事件 \"${event.name}\" 吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(event.id)
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
