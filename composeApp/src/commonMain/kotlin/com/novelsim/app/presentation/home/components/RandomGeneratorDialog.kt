package com.novelsim.app.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novelsim.app.domain.generator.RandomStoryGenerator

/**
 * 随机故事生成器配置对话框
 */
@Composable
fun RandomGeneratorDialog(
    onGenerate: (RandomStoryGenerator.GeneratorConfig, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("我的故事") }
    
    // 配置参数
    var minNodes by remember { mutableStateOf(5f) }
    var maxNodes by remember { mutableStateOf(15f) }
    var minChoices by remember { mutableStateOf(1f) }
    var maxChoices by remember { mutableStateOf(3f) }
    var battleProbability by remember { mutableStateOf(0.2f) }
    var conditionProbability by remember { mutableStateOf(0.15f) }
    var itemProbability by remember { mutableStateOf(0.1f) }
    var minEndings by remember { mutableStateOf(1f) }
    var maxEndings by remember { mutableStateOf(3f) }
    var selectedTheme by remember { mutableStateOf(RandomStoryGenerator.StoryTheme.FANTASY) }
    var useSeed by remember { mutableStateOf(false) }
    var seed by remember { mutableStateOf("12345") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("随机生成故事")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 故事标题
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("故事标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                HorizontalDivider()
                
                // 故事主题
                Text(
                    text = "故事主题",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RandomStoryGenerator.StoryTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme },
                            label = { Text(getThemeName(theme)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                HorizontalDivider()
                
                // 节点数量
                Text(
                    text = "节点数量: ${minNodes.toInt()} - ${maxNodes.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                RangeSlider(
                    value = minNodes..maxNodes,
                    onValueChange = { range ->
                        minNodes = range.start
                        maxNodes = range.endInclusive
                    },
                    valueRange = 3f..30f,
                    steps = 26
                )
                
                // 选择节点数量
                Text(
                    text = "分支选择数: ${minChoices.toInt()} - ${maxChoices.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                RangeSlider(
                    value = minChoices..maxChoices,
                    onValueChange = { range ->
                        minChoices = range.start
                        maxChoices = range.endInclusive
                    },
                    valueRange = 0f..5f,
                    steps = 4
                )
                
                // 结局数量
                Text(
                    text = "结局数量: ${minEndings.toInt()} - ${maxEndings.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                RangeSlider(
                    value = minEndings..maxEndings,
                    onValueChange = { range ->
                        minEndings = range.start
                        maxEndings = range.endInclusive
                    },
                    valueRange = 1f..5f,
                    steps = 3
                )
                
                HorizontalDivider()
                
                // 特殊节点概率
                Text(
                    text = "特殊节点概率",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                
                ProbabilitySlider(
                    label = "战斗节点",
                    value = battleProbability,
                    onValueChange = { battleProbability = it }
                )
                
                ProbabilitySlider(
                    label = "条件节点",
                    value = conditionProbability,
                    onValueChange = { conditionProbability = it }
                )
                
                ProbabilitySlider(
                    label = "道具节点",
                    value = itemProbability,
                    onValueChange = { itemProbability = it }
                )
                
                HorizontalDivider()
                
                // 随机种子
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useSeed,
                        onCheckedChange = { useSeed = it }
                    )
                    Text("使用固定种子 (可重现结果)")
                }
                
                if (useSeed) {
                    OutlinedTextField(
                        value = seed,
                        onValueChange = { seed = it.filter { c -> c.isDigit() } },
                        label = { Text("随机种子") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = RandomStoryGenerator.GeneratorConfig(
                        minNodes = minNodes.toInt(),
                        maxNodes = maxNodes.toInt(),
                        minChoices = minChoices.toInt(),
                        maxChoices = maxChoices.toInt(),
                        battleProbability = battleProbability,
                        conditionProbability = conditionProbability,
                        itemProbability = itemProbability,
                        minEndings = minEndings.toInt(),
                        maxEndings = maxEndings.toInt(),
                        theme = selectedTheme,
                        seed = if (useSeed) seed.toLongOrNull() else null
                    )
                    onGenerate(config, title)
                }
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("生成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ProbabilitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..0.5f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${(value * 100).toInt()}%",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun getThemeName(theme: RandomStoryGenerator.StoryTheme): String = when (theme) {
    RandomStoryGenerator.StoryTheme.FANTASY -> "奇幻"
    RandomStoryGenerator.StoryTheme.SCI_FI -> "科幻"
    RandomStoryGenerator.StoryTheme.MYSTERY -> "悬疑"
    RandomStoryGenerator.StoryTheme.ROMANCE -> "言情"
    RandomStoryGenerator.StoryTheme.HORROR -> "恐怖"
}
