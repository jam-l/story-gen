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
import com.novelsim.app.data.model.CharacterStats
import com.novelsim.app.data.model.Enemy
import com.novelsim.app.presentation.editor.EditorScreenModel
import com.novelsim.app.presentation.editor.components.StatInput
import com.novelsim.app.presentation.editor.components.CustomStatsEditor
import com.novelsim.app.presentation.editor.components.EntityVariableEditor
import androidx.compose.material3.HorizontalDivider
import com.novelsim.app.util.PlatformUtils
import com.novelsim.app.presentation.editor.components.SkillSelectionDialog
import com.novelsim.app.data.model.Skill
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@Composable
fun EnemyEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    val nameTemplates by screenModel.nameTemplates.collectAsState()
    var showRandomDialog by remember { mutableStateOf(false) }
    var selectedEnemyId by remember { mutableStateOf<String?>(null) }

    if (showRandomDialog) {
        com.novelsim.app.presentation.editor.components.RandomGenerationDialog(
            title = "生成随机怪物",
            templates = nameTemplates,
            onDismissRequest = { showRandomDialog = false },
            onConfirm = { templateId, count ->
                screenModel.generateRandomEnemy(templateId, count)
                showRandomDialog = false
            }
        )
    }
    
    LaunchedEffect(uiState.enemies) {
        if (selectedEnemyId != null && uiState.enemies.none { it.id == selectedEnemyId }) {
            selectedEnemyId = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：怪物列表
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.enemies) { enemy ->
                        ListItem(
                            headlineContent = { Text(enemy.name) },
                            supportingContent = { 
                                Text(
                                    text = "HP: ${enemy.stats.maxHp} ATK: ${enemy.stats.attack}", 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            modifier = Modifier
                                .clickable { selectedEnemyId = enemy.id }
                                .let {
                                    if (selectedEnemyId == enemy.id) {
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
                                    val newEnemy = Enemy(
                                        id = "enemy_${PlatformUtils.getCurrentTimeMillis()}",
                                        name = "新怪物",
                                        description = "",
                                        stats = CharacterStats(maxHp = 50, currentHp = 50, attack = 5)
                                    )
                                    screenModel.saveEnemy(newEnemy)
                                    selectedEnemyId = newEnemy.id
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
            if (selectedEnemyId != null) {
                val enemy = uiState.enemies.find { it.id == selectedEnemyId }
                if (enemy != null) {
                    EnemyDetailEditor(
                        enemy = enemy,
                        availableSkills = uiState.skills,
                        availableLocations = uiState.locations, // Pass locations
                        onSave = { screenModel.saveEnemy(it) },
                        onDelete = { 
                            screenModel.deleteEnemy(it) 
                            selectedEnemyId = null
                        }
                    )
                }
            } else {
                Text(
                    text = "请选择或新建一个怪物",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EnemyDetailEditor(
    enemy: Enemy,
    availableSkills: List<Skill>,
    availableLocations: List<com.novelsim.app.data.model.Location>,
    onSave: (Enemy) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(enemy) { mutableStateOf(enemy.name) }
    var description by remember(enemy) { mutableStateOf(enemy.description) }
    var stats by remember(enemy) { mutableStateOf(enemy.stats) }
    var expReward by remember(enemy) { mutableStateOf(enemy.expReward) }
    var goldReward by remember(enemy) { mutableStateOf(enemy.goldReward) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSkillDialog by remember { mutableStateOf(false) }

    if (showSkillDialog) {
        SkillSelectionDialog(
            availableSkills = availableSkills,
            onDismissRequest = { showSkillDialog = false },
            onSkillSelected = { skill ->
                if (skill.id !in enemy.skills) {
                    onSave(enemy.copy(skills = enemy.skills + skill.id))
                }
                showSkillDialog = false
            }
        )
    }

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
                text = "编辑怪物",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("删除怪物")
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
                        onSave(enemy.copy(name = it))
                    },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        onSave(enemy.copy(description = it))
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
                    val currentLocationName = availableLocations.find { it.id == enemy.locationId }?.name ?: "无 (未分配)"
                    OutlinedTextField(
                        value = currentLocationName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("出现地点") },
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
                                onSave(enemy.copy(locationId = null))
                                showLocationMenu = false
                            }
                        )
                        availableLocations.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc.name) },
                                onClick = {
                                    onSave(enemy.copy(locationId = loc.id))
                                    showLocationMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // 战斗属性
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("战斗属性", style = MaterialTheme.typography.titleMedium)
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput(
                        label = "最大生命 (HP)", 
                        value = stats.maxHp,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(maxHp = it, currentHp = it) // 默认满血
                        onSave(enemy.copy(stats = stats))
                    }
                    StatInput(
                        label = "最大法力 (MP)", 
                        value = stats.maxMp,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(maxMp = it, currentMp = it)
                        onSave(enemy.copy(stats = stats))
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput(
                        label = "攻击力", 
                        value = stats.attack,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(attack = it)
                        onSave(enemy.copy(stats = stats))
                    }
                    StatInput(
                        label = "防御力", 
                        value = stats.defense,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(defense = it)
                        onSave(enemy.copy(stats = stats))
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput(
                        label = "速度", 
                        value = stats.speed,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(speed = it)
                        onSave(enemy.copy(stats = stats))
                    }
                    StatInput(
                        label = "幸运", 
                        value = stats.luck,
                        modifier = Modifier.weight(1f)
                    ) {
                        stats = stats.copy(luck = it)
                        onSave(enemy.copy(stats = stats))
                    }
                }
                }
            }

        
        // 技能列表
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("技能列表", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showSkillDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加技能")
                    }
                }
                
                if (enemy.skills.isEmpty()) {
                    Text("暂无技能", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        enemy.skills.forEach { skillId ->
                            val skillName = availableSkills.find { it.id == skillId }?.name ?: skillId
                            InputChip(
                                selected = true,
                                onClick = { },
                                label = { Text(skillName) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.clickable {
                                            onSave(enemy.copy(skills = enemy.skills - skillId))
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // 自定义数值属性
        CustomStatsEditor(
            stats = stats,
            onStatsChange = { newStats ->
                stats = newStats
                onSave(enemy.copy(stats = newStats))
            }
        )
        
        // 自定义变量 (字符串)
        EntityVariableEditor(
            variables = enemy.variables,
            onVariablesChange = { newVars ->
                onSave(enemy.copy(variables = newVars))
            }
        )
        
        // 奖励
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("战利品", style = MaterialTheme.typography.titleMedium)
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput(
                        label = "经验值奖励", 
                        value = expReward,
                        modifier = Modifier.weight(1f)
                    ) {
                        expReward = it
                        onSave(enemy.copy(expReward = it))
                    }
                    StatInput(
                        label = "金币奖励", 
                        value = goldReward,
                        modifier = Modifier.weight(1f)
                    ) {
                        goldReward = it
                        onSave(enemy.copy(goldReward = it))
                    }
                }
            }
        }
        
        // TODO: 掉落物
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除怪物 \"${enemy.name}\" 吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(enemy.id)
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
