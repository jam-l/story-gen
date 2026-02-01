package com.novelsim.app.presentation.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.Skill

@Composable
fun SkillSelectionDialog(
    availableSkills: List<Skill>,
    onDismissRequest: () -> Unit,
    onSkillSelected: (Skill) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("选择技能") },
        text = {
            if (availableSkills.isEmpty()) {
                Text("暂无可用技能。请先在技能标签页创建技能。")
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableSkills) { skill ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSkillSelected(skill) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(skill.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "MP: ${skill.mpCost} | 效果: " +
                                            if (skill.damage > 0) "伤害${skill.damage} " else "" +
                                            if (skill.heal > 0) "治疗${skill.heal}" else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text("关闭") }
        }
    )
}
