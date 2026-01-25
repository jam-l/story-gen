package com.novelsim.app.presentation.editor.database

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.dp
import com.novelsim.app.presentation.editor.EditorScreenModel
import com.novelsim.app.data.model.Character
import com.novelsim.app.presentation.editor.components.StatInput

/**
 * 数据库编辑器主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseEditor(
    onDismissRequest: () -> Unit,
    screenModel: EditorScreenModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("角色", "地点", "事件", "线索", "阵营", "怪物", "道具")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            TopAppBar(
                title = { Text("故事数据库") },
                navigationIcon = {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // 标签栏
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> CharacterEditor(screenModel)
                    1 -> LocationEditor(screenModel)
                    2 -> EventEditor(screenModel)
                    3 -> ClueEditor(screenModel)
                    4 -> FactionEditor(screenModel)
                    5 -> EnemyEditor(screenModel)
                    6 -> ItemEditor(screenModel)
                }
            }
        }
    }
}

@Composable
fun CharacterEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    var selectedCharacterId by remember { mutableStateOf<String?>(null) }
    
    // 监听角色列表变化，如果选中角色被删除，取消选中
    LaunchedEffect(uiState.characters) {
        if (selectedCharacterId != null && uiState.characters.none { it.id == selectedCharacterId }) {
            selectedCharacterId = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：角色列表
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.characters) { character ->
                        ListItem(
                            headlineContent = { Text(character.name) },
                            supportingContent = { 
                                Text(
                                    text = character.description.take(20), 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            modifier = Modifier
                                .clickable { selectedCharacterId = character.id }
                                .let {
                                    if (selectedCharacterId == character.id) {
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
                                val newChar = Character(
                                    id = "char_${com.novelsim.app.util.PlatformUtils.getCurrentTimeMillis()}",
                                    name = "新角色",
                                    description = ""
                                )
                                screenModel.saveCharacter(newChar)
                                selectedCharacterId = newChar.id
                            },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("添加角色")
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
            if (selectedCharacterId != null) {
                val character = uiState.characters.find { it.id == selectedCharacterId }
                if (character != null) {
                    CharacterDetailEditor(
                        character = character,
                        onSave = { screenModel.saveCharacter(it) },
                        onDelete = { 
                            screenModel.deleteCharacter(it) 
                            selectedCharacterId = null
                        }
                    )
                }
            } else {
                Text(
                    text = "请选择或新建一个角色",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CharacterDetailEditor(
    character: Character,
    onSave: (Character) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(character) { mutableStateOf(character.name) }
    var description by remember(character) { mutableStateOf(character.description) }
    var stats by remember(character) { mutableStateOf(character.baseStats) }
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
                text = "编辑角色",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("删除角色")
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
                        onSave(character.copy(name = it))
                    },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        onSave(character.copy(description = it))
                    },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }

        // 基础属性
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("基础属性", style = MaterialTheme.typography.titleMedium)
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput(
                        label = "最大生命 (HP)", 
                        value = stats.maxHp,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(maxHp = it, currentHp = it)
                        onSave(character.copy(baseStats = stats))
                    }
                    StatInput(
                        label = "最大法力 (MP)", 
                        value = stats.maxMp,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(maxMp = it, currentMp = it)
                        onSave(character.copy(baseStats = stats))
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput(
                        label = "攻击力", 
                        value = stats.attack,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(attack = it)
                        onSave(character.copy(baseStats = stats))
                    }
                    StatInput(
                        label = "防御力", 
                        value = stats.defense,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(defense = it)
                        onSave(character.copy(baseStats = stats))
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput(
                        label = "速度", 
                        value = stats.speed,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(speed = it)
                        onSave(character.copy(baseStats = stats))
                    }
                    StatInput(
                        label = "幸运", 
                        value = stats.luck,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(luck = it)
                        onSave(character.copy(baseStats = stats))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除角色 \"${character.name}\" 吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(character.id)
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




