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
import com.novelsim.app.data.model.Location
import com.novelsim.app.presentation.editor.EditorScreenModel
import com.novelsim.app.util.PlatformUtils

@Composable
fun LocationEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    
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
                Button(
                    onClick = {
                        val newLocation = Location(
                            id = "loc_${PlatformUtils.getCurrentTimeMillis()}",
                            name = "新地点",
                            description = ""
                        )
                        screenModel.saveLocation(newLocation)
                        selectedLocationId = newLocation.id
                    },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("添加地点")
                }

                HorizontalDivider()

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


@Composable
fun LocationDetailEditor(
    location: Location,
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
        
        // TODO: 连接的地点编辑器 (Connected Locations)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("连接的地点 (TODO)", style = MaterialTheme.typography.titleMedium)
                Text("将在后续版本实现地点连接可视化编辑", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        // TODO: NPC 列表编辑器
         Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("包含的 NPC (TODO)", style = MaterialTheme.typography.titleMedium)
                 Text("将在后续版本实现 NPC 分配", style = MaterialTheme.typography.bodySmall)
            }
        }
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
