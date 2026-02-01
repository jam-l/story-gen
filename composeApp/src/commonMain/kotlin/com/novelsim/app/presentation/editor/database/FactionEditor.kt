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
import com.novelsim.app.data.model.Faction
import com.novelsim.app.presentation.editor.EditorScreenModel
import com.novelsim.app.util.PlatformUtils

@Composable
fun FactionEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    var selectedFactionId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(uiState.factions) {
        if (selectedFactionId != null && uiState.factions.none { it.id == selectedFactionId }) {
            selectedFactionId = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：阵营列表
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.factions) { faction ->
                        ListItem(
                            headlineContent = { Text(faction.name) },
                            supportingContent = { 
                                Text(
                                    text = "声望: ${faction.reputation}", 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            modifier = Modifier
                                .clickable { selectedFactionId = faction.id }
                                .let {
                                    if (selectedFactionId == faction.id) {
                                        it.background(MaterialTheme.colorScheme.primaryContainer)
                                    } else it
                                }
                        )
                        HorizontalDivider()
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val newFaction = Faction(
                                    id = "fac_${PlatformUtils.getCurrentTimeMillis()}",
                                    name = "新阵营",
                                    description = "",
                                    reputation = 0
                                )
                                screenModel.saveFaction(newFaction)
                                selectedFactionId = newFaction.id
                            },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新建")
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
            if (selectedFactionId != null) {
                val faction = uiState.factions.find { it.id == selectedFactionId }
                if (faction != null) {
                    FactionDetailEditor(
                        faction = faction,
                        onSave = { screenModel.saveFaction(it) },
                        onDelete = { 
                            screenModel.deleteFaction(it) 
                            selectedFactionId = null
                        }
                    )
                }
            } else {
                Text(
                    text = "请选择或新建一个阵营",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun FactionDetailEditor(
    faction: Faction,
    onSave: (Faction) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(faction) { mutableStateOf(faction.name) }
    var description by remember(faction) { mutableStateOf(faction.description) }
    var reputation by remember(faction) { mutableStateOf(faction.reputation) }
    
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
                text = "编辑阵营",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("删除阵营")
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
                        onSave(faction.copy(name = it))
                    },
                    label = { Text("阵营名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        onSave(faction.copy(description = it))
                    },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                 OutlinedTextField(
                    value = reputation.toString(),
                    onValueChange = { 
                         if (it.all { c -> c.isDigit() || c == '-' }) {
                            val r = it.toIntOrNull() ?: 0
                            reputation = r
                            onSave(faction.copy(reputation = r))
                        }
                    },
                    label = { Text("初始声望") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除阵营 \"${faction.name}\" 吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(faction.id)
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
