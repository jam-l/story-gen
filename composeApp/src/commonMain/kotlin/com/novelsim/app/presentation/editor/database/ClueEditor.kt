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
import com.novelsim.app.data.model.Clue
import com.novelsim.app.presentation.editor.EditorScreenModel
import com.novelsim.app.util.PlatformUtils

@Composable
fun ClueEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    var selectedClueId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(uiState.clues) {
        if (selectedClueId != null && uiState.clues.none { it.id == selectedClueId }) {
            selectedClueId = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：线索列表
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.clues) { clue ->
                        ListItem(
                            headlineContent = { Text(clue.name) },
                            supportingContent = { 
                                Text(
                                    text = clue.description.take(20), 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            modifier = Modifier
                                .clickable { selectedClueId = clue.id }
                                .let {
                                    if (selectedClueId == clue.id) {
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
                                val newClue = Clue(
                                    id = "clue_${PlatformUtils.getCurrentTimeMillis()}",
                                    name = "新线索",
                                    description = ""
                                )
                                screenModel.saveClue(newClue)
                                selectedClueId = newClue.id
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
            if (selectedClueId != null) {
                val clue = uiState.clues.find { it.id == selectedClueId }
                if (clue != null) {
                    ClueDetailEditor(
                        clue = clue,
                        onSave = { screenModel.saveClue(it) },
                        onDelete = { 
                            screenModel.deleteClue(it) 
                            selectedClueId = null
                        }
                    )
                }
            } else {
                Text(
                    text = "请选择或新建一个线索",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun ClueDetailEditor(
    clue: Clue,
    onSave: (Clue) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(clue) { mutableStateOf(clue.name) }
    var description by remember(clue) { mutableStateOf(clue.description) }
    var isKnown by remember(clue) { mutableStateOf(clue.isKnown) }
    
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
                text = "编辑线索",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("删除线索")
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
                        onSave(clue.copy(name = it))
                    },
                    label = { Text("线索名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        onSave(clue.copy(description = it))
                    },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isKnown,
                        onCheckedChange = { 
                            isKnown = it
                            onSave(clue.copy(isKnown = it))
                        }
                    )
                    Text("初始已知 (故事开始时玩家即获得该线索)")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除线索 \"${clue.name}\" 吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(clue.id)
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
