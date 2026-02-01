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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.*
import com.novelsim.app.presentation.editor.EditorScreenModel
import androidx.compose.material3.HorizontalDivider
import com.novelsim.app.presentation.editor.components.StatInput
import com.novelsim.app.util.PlatformUtils
import com.novelsim.app.presentation.editor.components.EntityVariableEditor

/**
 * 道具编辑器
 */
@Composable
fun ItemEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    val nameTemplates by screenModel.nameTemplates.collectAsState()
    var showRandomDialog by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }

    if (showRandomDialog) {
        com.novelsim.app.presentation.editor.components.RandomGenerationDialog(
            title = "生成随机道具",
            templates = nameTemplates,
            onDismissRequest = { showRandomDialog = false },
            onConfirm = { templateId, count ->
                screenModel.generateRandomItem(templateId, count)
                showRandomDialog = false
            }
        )
    }
    
    // 监听列表变化
    LaunchedEffect(uiState.items) {
        if (selectedItemId != null && uiState.items.none { it.id == selectedItemId }) {
            selectedItemId = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧列表
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                    items(uiState.items) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { 
                                Text(
                                    text = item.type.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .clickable { selectedItemId = item.id }
                                .then(
                                    if (selectedItemId == item.id) 
                                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                    else Modifier
                                )
                        )
                        HorizontalDivider()
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val newItem = Item(
                                        id = "item_${PlatformUtils.getCurrentTimeMillis()}",
                                        name = "新道具",
                                        description = "这是一个新道具",
                                        type = ItemType.CONSUMABLE,
                                        price = 10
                                    )
                                    screenModel.saveItem(newItem)
                                    selectedItemId = newItem.id
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

        // 右侧编辑区
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            if (selectedItemId != null) {
                val item = uiState.items.find { it.id == selectedItemId }
                if (item != null) {
                    ItemDetailEditor(
                        item = item,
                        availableLocations = uiState.locations, // Pass locations
                        onSave = { screenModel.saveItem(it) },
                        onDelete = { 
                            screenModel.deleteItem(it)
                            selectedItemId = null
                        }
                    )
                }
            } else {
                 Text(
                    text = "请选择或新建一个道具",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailEditor(
    item: Item,
    availableLocations: List<com.novelsim.app.data.model.Location>,
    onSave: (Item) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(item) { mutableStateOf(item.name) }
    var description by remember(item) { mutableStateOf(item.description) }
    var price by remember(item) { mutableStateOf(item.price) }
    var type by remember(item) { mutableStateOf(item.type) }
    var stackable by remember(item) { mutableStateOf(item.stackable) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("编辑道具", style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("删除道具")
            }
        }

        // 基本属性
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("基本信息", style = MaterialTheme.typography.titleMedium)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        onSave(item.copy(name = it))
                    },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 类型选择
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ItemType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                onClick = {
                                    type = t
                                    onSave(item.copy(type = t))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        onSave(item.copy(description = it))
                    },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // 所属地点选择
                var showLocationMenu by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = showLocationMenu,
                    onExpandedChange = { showLocationMenu = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentLocationName = availableLocations.find { it.id == item.locationId }?.name ?: "无 (未分配)"
                    OutlinedTextField(
                        value = currentLocationName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("获得地点") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLocationMenu) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showLocationMenu,
                        onDismissRequest = { showLocationMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("无 (未分配)") },
                            onClick = {
                                onSave(item.copy(locationId = null))
                                showLocationMenu = false
                            }
                        )
                        availableLocations.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc.name) },
                                onClick = {
                                    onSave(item.copy(locationId = loc.id))
                                    showLocationMenu = false
                                }
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput(
                        label = "价格",
                        value = price,
                        modifier = Modifier.weight(1f)
                    ) {
                        price = it
                        onSave(item.copy(price = it))
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Checkbox(
                            checked = stackable,
                            onCheckedChange = { 
                                stackable = it
                                onSave(item.copy(stackable = it))
                            }
                        )
                        Text("可堆叠")
                    }
                }
            }
        }
        
        // 自定义属性
        EntityVariableEditor(
            variables = item.variables,
            onVariablesChange = { newVars ->
                onSave(item.copy(variables = newVars))
            }
        )
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除道具 \"${item.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(item.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}
