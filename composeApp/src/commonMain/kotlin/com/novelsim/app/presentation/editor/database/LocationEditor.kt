package com.novelsim.app.presentation.editor.database

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.Location
import com.novelsim.app.presentation.editor.EditorScreenModel
import com.novelsim.app.util.PlatformUtils
import com.novelsim.app.presentation.editor.components.EntityVariableEditor

@Composable
fun LocationEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    val nameTemplates by screenModel.nameTemplates.collectAsState()
    var showRandomDialog by remember { mutableStateOf(false) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }

    if (showRandomDialog) {
        com.novelsim.app.presentation.editor.components.RandomGenerationDialog(
            title = "生成随机地点",
            templates = nameTemplates,
            onDismissRequest = { showRandomDialog = false },
            onConfirm = { templateId, count ->
                screenModel.generateRandomLocation(templateId, count)
                showRandomDialog = false
            }
        )
    }
    
    // 监听地点列表变化，如果选中地点被删除，取消选中
    LaunchedEffect(uiState.locations) {
        if (selectedLocationId != null && uiState.locations.none { it.id == selectedLocationId }) {
            selectedLocationId = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：地点列表
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.locations) { location ->
                        ListItem(
                            headlineContent = { Text(location.name) },
                            supportingContent = { 
                                Text(
                                    text = location.description.take(20), 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            modifier = Modifier
                                .clickable { selectedLocationId = location.id }
                                .let {
                                    if (selectedLocationId == location.id) {
                                        it.background(MaterialTheme.colorScheme.primaryContainer)
                                    } else it
                                }
                        )
                        HorizontalDivider()
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val newLocation = Location(
                                        id = "loc_${PlatformUtils.getCurrentTimeMillis()}",
                                        name = "新地点",
                                        description = ""
                                    )
                                    screenModel.saveLocation(newLocation)
                                    selectedLocationId = newLocation.id
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("新建")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    showRandomDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("随机")
                            }
                        }
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
            if (selectedLocationId != null) {
                val location = uiState.locations.find { it.id == selectedLocationId }
                if (location != null) {
                    LocationDetailEditor(
                        location = location,
                        allCharacters = uiState.characters,
                        allEnemies = uiState.enemies,
                        allItems = uiState.items,
                        allNodes = uiState.nodes,
                        onNodeClick = { nodeId ->
                            screenModel.toggleDatabaseEditor(false)
                            screenModel.selectNode(nodeId)
                        },
                        onSave = { screenModel.saveLocation(it) },
                        onDelete = { 
                            screenModel.deleteLocation(it) 
                            selectedLocationId = null
                        }
                    )
                }
            } else {
                Text(
                    text = "请选择或新建一个地点",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationDetailEditor(
    location: Location,
    allCharacters: List<com.novelsim.app.data.model.Character>,
    allEnemies: List<com.novelsim.app.data.model.Enemy>,
    allItems: List<com.novelsim.app.data.model.Item>,
    allNodes: List<EditorScreenModel.EditorNode>,
    onNodeClick: (String) -> Unit,
    onSave: (Location) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(location) { mutableStateOf(location.name) }
    var description by remember(location) { mutableStateOf(location.description) }
    var background by remember(location) { mutableStateOf(location.background ?: "") }
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
                text = "编辑地点",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("删除地点")
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
                        onSave(location.copy(name = it))
                    },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        onSave(location.copy(description = it))
                    },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                OutlinedTextField(
                    value = background,
                    onValueChange = { 
                        background = it
                        onSave(location.copy(background = it.ifEmpty { null }))
                    },
                    label = { Text("背景图片路径 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        
        // 连接的地点编辑器 (Connected Locations)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("连接的地点", style = MaterialTheme.typography.titleMedium)
                if (location.connectedLocationIds.isEmpty()) {
                    Text("暂无连接地点", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        location.connectedLocationIds.forEach { targetId ->
                            AssistChip(onClick = {}, label = { Text(targetId) })
                        }
                    }
                }
            }
        }

        // 关联剧情 (Associated Story Nodes)
        val locationNodes = allNodes.filter { it.node.locationId == location.id }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("关联剧情节点 (${locationNodes.size})", style = MaterialTheme.typography.titleMedium)
                if (locationNodes.isEmpty()) {
                    Text("暂无关联剧情", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        locationNodes.forEach { editorNode ->
                            AssistChip(
                                onClick = { 
                                    // 点击跳转到该节点
                                    onNodeClick(editorNode.node.id)
                                }, 
                                label = { Text(editorNode.node.id) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        }
        
        // NPC 列表
        Card(modifier = Modifier.fillMaxWidth()) {
            val locationNpcs = allCharacters.filter { it.locationId == location.id }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("常驻 NPC (${locationNpcs.size})", style = MaterialTheme.typography.titleMedium)
                if (locationNpcs.isEmpty()) {
                    Text("暂无 NPC", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        locationNpcs.forEach { npc ->
                            SuggestionChip(onClick = {}, label = { Text(npc.name) })
                        }
                    }
                }
            }
        }

        // 怪物列表
        Card(modifier = Modifier.fillMaxWidth()) {
            val locationEnemies = allEnemies.filter { it.locationId == location.id }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("出现怪物 (${locationEnemies.size})", style = MaterialTheme.typography.titleMedium)
                if (locationEnemies.isEmpty()) {
                    Text("暂无怪物", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        locationEnemies.forEach { enemy ->
                            SuggestionChip(onClick = {}, label = { Text(enemy.name) })
                        }
                    }
                }
            }
        }

        // 道具列表
        Card(modifier = Modifier.fillMaxWidth()) {
            val locationItems = allItems.filter { it.locationId == location.id }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("产出道具 (${locationItems.size})", style = MaterialTheme.typography.titleMedium)
                if (locationItems.isEmpty()) {
                    Text("暂无道具", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        locationItems.forEach { item ->
                            SuggestionChip(onClick = {}, label = { Text(item.name) })
                        }
                    }
                }
            }
        }
        
        // 自定义属性
        EntityVariableEditor(
            variables = location.variables,
            onVariablesChange = { newVars ->
                onSave(location.copy(variables = newVars))
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除地点 \"${location.name}\" 吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(location.id)
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
