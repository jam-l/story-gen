package com.novelsim.app.presentation.editor.database

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Refresh
import com.novelsim.app.data.model.Skill
import com.novelsim.app.presentation.editor.EditorScreenModel
import com.novelsim.app.util.PlatformUtils
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillEditor(screenModel: EditorScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    var selectedSkillId by remember { mutableStateOf<String?>(null) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    
    // Auto-select first if none selected? No, consistent with others.
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: Skill List
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                    items(uiState.skills) { skill ->
                        ListItem(
                            headlineContent = { Text(skill.name) },
                            supportingContent = { 
                                Text("MP: ${skill.mpCost}", style = MaterialTheme.typography.bodySmall)
                            },
                            modifier = Modifier
                                .clickable { selectedSkillId = skill.id }
                                .then(
                                    if (selectedSkillId == skill.id) 
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
                                    val newSkill = Skill(
                                        id = "skill_${PlatformUtils.getCurrentTimeMillis()}",
                                        name = "新技能",
                                        description = "技能描述",
                                        mpCost = 10,
                                        damage = 0,
                                        heal = 0
                                    )
                                    screenModel.saveSkill(newSkill)
                                    selectedSkillId = newSkill.id
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("新建")
                            }
                            
                            OutlinedButton(
                                onClick = { showGenerateDialog = true },
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
        
        if (showGenerateDialog) {
            val templates by screenModel.nameTemplates.collectAsState()
            // Filter templates that look like skill templates
            val skillTemplates = templates.filter { it.id.startsWith("skill") || it.description.contains("招式") || it.description.contains("武功") }
            
            com.novelsim.app.presentation.editor.components.RandomGenerationDialog(
                title = "随机生成技能",
                templates = if (skillTemplates.isNotEmpty()) skillTemplates else templates,
                onDismissRequest = { showGenerateDialog = false },
                onConfirm = { templateId, count ->
                    repeat(count) {
                        screenModel.generateRandomSkill(templateId) { template ->
                            val newSkill = template.copy(id = "skill_${PlatformUtils.getCurrentTimeMillis()}_$it")
                            screenModel.saveSkill(newSkill)
                            // Select the last generated one
                            selectedSkillId = newSkill.id
                        }
                    }
                    showGenerateDialog = false
                }
            )
        }
        
        // Right: Edit Form
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            val selectedSkill = uiState.skills.find { it.id == selectedSkillId }
            
            if (selectedSkill != null) {
                Card(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("编辑技能", style = MaterialTheme.typography.titleLarge)
                        
                        OutlinedTextField(
                            value = selectedSkill.name,
                            onValueChange = { screenModel.saveSkill(selectedSkill.copy(name = it)) },
                            label = { Text("名称") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = selectedSkill.description,
                            onValueChange = { screenModel.saveSkill(selectedSkill.copy(description = it)) },
                            label = { Text("描述") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = selectedSkill.mpCost.toString(),
                                onValueChange = { 
                                    if (it.all { char -> char.isDigit() }) {
                                        screenModel.saveSkill(selectedSkill.copy(mpCost = it.toIntOrNull() ?: 0))
                                    }
                                },
                                label = { Text("MP消耗") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = selectedSkill.damage.toString(),
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() }) {
                                        screenModel.saveSkill(selectedSkill.copy(damage = it.toIntOrNull() ?: 0))
                                    }
                                },
                                label = { Text("伤害") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = selectedSkill.heal.toString(),
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() }) {
                                        screenModel.saveSkill(selectedSkill.copy(heal = it.toIntOrNull() ?: 0))
                                    }
                                },
                                label = { Text("治疗") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                screenModel.deleteSkill(selectedSkill.id)
                                selectedSkillId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("删除技能")
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("请选择或创建一个技能", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
