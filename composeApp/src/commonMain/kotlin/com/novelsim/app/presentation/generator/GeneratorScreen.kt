package com.novelsim.app.presentation.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.novelsim.app.data.repository.StoryRepository
import com.novelsim.app.domain.generator.RandomStoryGenerator
import com.novelsim.app.presentation.editor.EditorScreen
import com.novelsim.app.presentation.generator.components.CustomStatConfigEditor
import kotlinx.coroutines.launch

class GeneratorScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // Simple ScreenModel for Generator logic
        val screenModel = koinScreenModel<GeneratorScreenModel>()
        var loadedTemplates by remember { mutableStateOf<List<com.novelsim.app.data.source.NameTemplate>>(emptyList()) }
        
        LaunchedEffect(Unit) {
            loadedTemplates = screenModel.loadTemplates()
        }
        
        GeneratorScreenContent(
            allTemplates = loadedTemplates,
            onBack = { navigator.pop() },
            onGenerate = { config, title ->
                screenModel.generateAndSave(config, title) { storyId ->
                    navigator.replace(EditorScreen(storyId))
                }
            }
        )
    }
}

class GeneratorScreenModel(
    private val storyRepository: StoryRepository
) : ScreenModel {
    fun generateAndSave(
        config: RandomStoryGenerator.GeneratorConfig, 
        title: String,
        onSuccess: (String) -> Unit
    ) {
        screenModelScope.launch {
            // 初始化随机名称生成器
            val nameProvider = com.novelsim.app.data.source.RandomNameProvider()
            try {
                nameProvider.initialize { fileName ->
                    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
                    novelsimulator.composeapp.generated.resources.Res.readBytes("files/$fileName").decodeToString()
                }
            } catch (e: Exception) {
                // 忽略加载错误，使用默认值
                println("Failed to load name provider: ${e.message}")
            }

            val generator = RandomStoryGenerator(config, nameProvider)
            val generatedStory = generator.generate(title)
            
            // 强制修复：如果生成器返回空敌人（可能是缓存问题），手动注入默认敌人
            val finalStory = if (generatedStory.enemies.isEmpty()) {
                val fallbackEnemies = listOf(
                    com.novelsim.app.data.model.Enemy(
                        id = "slime_fallback",
                        name = "史莱姆(补)",
                        description = "系统自动补充的敌人",
                        stats = com.novelsim.app.data.model.CharacterStats(maxHp = 50, currentHp = 50, attack = 5, defense = 0, speed = 5, exp = 5),
                        expReward = 5,
                        goldReward = 2
                    )
                )
                generatedStory.copy(enemies = fallbackEnemies)
            } else {
                generatedStory
            }
            
            storyRepository.saveStory(finalStory)
            onSuccess(finalStory.id)
        }
    }
    
    suspend fun loadTemplates(): List<com.novelsim.app.data.source.NameTemplate> {
        val nameProvider = com.novelsim.app.data.source.RandomNameProvider()
        try {
            nameProvider.initialize { fileName ->
                @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
                novelsimulator.composeapp.generated.resources.Res.readBytes("files/$fileName").decodeToString()
            }
        } catch (e: Exception) {
            println("Failed to load name provider for templates: ${e.message}")
        }
        return nameProvider.getTemplates()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratorScreenContent(
    allTemplates: List<com.novelsim.app.data.source.NameTemplate>,
    onBack: () -> Unit,
    onGenerate: (RandomStoryGenerator.GeneratorConfig, String) -> Unit
) {
    var title by remember { mutableStateOf("我的随机故事") }
    
    // Config params
    var minNodes by remember { mutableStateOf(10f) }
    var maxNodes by remember { mutableStateOf(20f) }
    var minChoices by remember { mutableStateOf(2f) }
    var maxChoices by remember { mutableStateOf(5f) }
    var minEndings by remember { mutableStateOf(1f) }
    var maxEndings by remember { mutableStateOf(3f) }
    var generateEnding by remember { mutableStateOf(true) }
    
    var battleProbability by remember { mutableStateOf(0.2f) }
    var conditionProbability by remember { mutableStateOf(0.15f) }
    var itemProbability by remember { mutableStateOf(0.1f) }
    
    // Core Elements Config
    // Elements Generation Rules
    var generationRules by remember { 
        mutableStateOf(listOf(
            RandomStoryGenerator.GenerationRule(
                type = RandomStoryGenerator.EntityType.CHARACTER,
                count = 3
            ),
            RandomStoryGenerator.GenerationRule(
                type = RandomStoryGenerator.EntityType.ITEM,
                count = 2
            ),
            RandomStoryGenerator.GenerationRule(
                type = RandomStoryGenerator.EntityType.ENEMY,
                count = 2
            )
        )) 
    }
    
    // Global Settings
    var chaos by remember { mutableStateOf(0.1f) }
    var difficulty by remember { mutableStateOf(0.5f) }
    
    var selectedTheme by remember { mutableStateOf(RandomStoryGenerator.StoryTheme.FANTASY) }
    var useSeed by remember { mutableStateOf(false) }
    var seed by remember { mutableStateOf("") }
    
    // Init logic if needed in future
    LaunchedEffect(allTemplates) {
         // No default logic needed
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生成新故事") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val config = RandomStoryGenerator.GeneratorConfig(
                        minNodes = minNodes.toInt(),
                        maxNodes = maxNodes.toInt(),
                        minChoices = minChoices.toInt(),
                        maxChoices = maxChoices.toInt(),
                        minEndings = minEndings.toInt(),
                        maxEndings = maxEndings.toInt(),
                        generateEnding = generateEnding,
                        battleProbability = battleProbability,
                        conditionProbability = conditionProbability,
                        itemProbability = itemProbability,
                        chaos = chaos,
                        difficulty = difficulty,
                        theme = selectedTheme,
                        seed = if (useSeed) seed.toLongOrNull() else null,
                        rules = generationRules
                    )
                    onGenerate(config, title)
                },
                icon = { Icon(Icons.Default.Star, contentDescription = null) },
                text = { Text("开始生成") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Basic Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("基础设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("故事标题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("故事主题", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                }
            }
            
            // Structure
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("结构规模", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    SliderControl("节点数量", "${minNodes.toInt()} - ${maxNodes.toInt()}") {
                        RangeSlider(
                            value = minNodes..maxNodes,
                            onValueChange = { 
                                minNodes = it.start
                                maxNodes = it.endInclusive
                            },
                            valueRange = 5f..50f,
                            steps = 44
                        )
                    }
                    
                    SliderControl("分支选择", "${minChoices.toInt()} - ${maxChoices.toInt()}") {
                        RangeSlider(
                            value = minChoices..maxChoices,
                            onValueChange = { 
                                minChoices = it.start
                                maxChoices = it.endInclusive
                            },
                            valueRange = 0f..10f,
                            steps = 9
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("生成结局", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = generateEnding,
                            onCheckedChange = { generateEnding = it }
                        )
                    }

                    if (generateEnding) {
                        SliderControl("结局数量", "${minEndings.toInt()} - ${maxEndings.toInt()}") {
                            RangeSlider(
                                value = minEndings..maxEndings,
                                onValueChange = {
                                    minEndings = it.start
                                    maxEndings = it.endInclusive
                                },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                        }
                    } else {
                        Text(
                            text = "无限模式：故事将循环进行，没有固定结局",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Advanced Params (Chaos & Difficulty)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("高级参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    
                    // Chaos
                    SliderControl("混乱度 (Chaos)", "${(chaos * 100).toInt()}%") {
                        Slider(
                            value = chaos,
                            onValueChange = { chaos = it },
                            valueRange = 0f..1f
                        )
                    }
                    Text(
                        text = if (chaos < 0.3f) "结构较线性" else if (chaos < 0.7f) "包含一些随机跳转" else "结构非常混乱",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Difficulty
                    SliderControl("难度系数", "${(difficulty * 100).toInt()}%") {
                        Slider(
                            value = difficulty,
                            onValueChange = { difficulty = it },
                            valueRange = 0f..1f
                        )
                    }
                    Text(
                        text = if (difficulty < 0.3f) "简单轻松" else if (difficulty < 0.7f) "适中挑战" else "硬核模式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Elements Probability
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("元素密度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    ProbabilityControl("战斗节点", battleProbability) { battleProbability = it }
                    ProbabilityControl("条件判定", conditionProbability) { conditionProbability = it }
                    ProbabilityControl("道具获取", itemProbability) { itemProbability = it }
                }
            }
            

            

            
            // Generation Rules List
            GenerationRuleListEditor(
                rules = generationRules,
                allTemplates = allTemplates,
                onRulesChange = { generationRules = it }
            )
            
            // Seed
             Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                     Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useSeed,
                            onCheckedChange = { useSeed = it }
                        )
                        Text("使用固定种子")
                    }
                    
                    if (useSeed) {
                         OutlinedTextField(
                            value = seed,
                            onValueChange = { seed = it.filter { c -> c.isDigit() } },
                            label = { Text("随机种子 (数字)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
             }
             
             Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    valueText: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun ProbabilityControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    SliderControl(label, "${(value * 100).toInt()}%") {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..0.5f
        )
    }
}

@Composable
fun GenerationRuleListEditor(
    rules: List<RandomStoryGenerator.GenerationRule>,
    allTemplates: List<com.novelsim.app.data.source.NameTemplate>,
    onRulesChange: (List<RandomStoryGenerator.GenerationRule>) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("生成规则", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (rules.isEmpty()) {
                Text(
                    text = "暂无生成规则，点击下方按钮添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rules.forEachIndexed { index, rule ->
                        GenerationRuleEditor(
                            rule = rule,
                            allTemplates = allTemplates,
                            onUpdate = { updatedRule ->
                                val newRules = rules.toMutableList()
                                newRules[index] = updatedRule
                                onRulesChange(newRules)
                            },
                            onDelete = {
                                val newRules = rules.toMutableList()
                                newRules.removeAt(index)
                                onRulesChange(newRules)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full-width Add Rule Button at the bottom
            OutlinedButton(
                onClick = {
                    val newRules = rules + RandomStoryGenerator.GenerationRule(RandomStoryGenerator.EntityType.CHARACTER)
                    onRulesChange(newRules)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("规则", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun GenerationRuleEditor(
    rule: RandomStoryGenerator.GenerationRule,
    allTemplates: List<com.novelsim.app.data.source.NameTemplate>,
    onUpdate: (RandomStoryGenerator.GenerationRule) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Type, Count, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Selector
                var typeExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.5f)) {
                    OutlinedButton(
                        onClick = { typeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(getEntityTypeLabel(rule.type))
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        RandomStoryGenerator.EntityType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(getEntityTypeLabel(type)) },
                                onClick = { 
                                    onUpdate(rule.copy(type = type))
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Count Input
                IntTextField(
                    value = rule.count,
                    onValueChange = { onUpdate(rule.copy(count = it)) },
                    label = "数量",
                    modifier = Modifier.weight(1f)
                )
                
                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "删除", 
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Row 2: Template Selector
            SingleTemplateSelector(
                templates = allTemplates,
                selectedId = rule.templateId,
                onSelectionChange = { onUpdate(rule.copy(templateId = it)) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Row 3: Custom Attributes
            var showAttributes by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAttributes = !showAttributes }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    if (showAttributes) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "自定义属性 (${rule.customStats.size})",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            if (showAttributes) {
                Spacer(modifier = Modifier.height(8.dp))
                CustomStatConfigEditor(
                    configs = rule.customStats,
                    onConfigsChange = { onUpdate(rule.copy(customStats = it)) }
                )
            }

            // Row 4: Basic Stats (Characters & Enemies only)
            if (rule.type == RandomStoryGenerator.EntityType.CHARACTER || rule.type == RandomStoryGenerator.EntityType.ENEMY) {
                Spacer(modifier = Modifier.height(8.dp))
                var showBasicStats by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBasicStats = !showBasicStats }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        if (showBasicStats) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "基础属性 (HP/攻/防/速)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (showBasicStats) {
                     val basicStats = rule.basicStats ?: RandomStoryGenerator.BasicStatsConfig()
                     BasicStatsEditor(
                         config = basicStats,
                         onConfigChange = { onUpdate(rule.copy(basicStats = it)) },
                         enabled = true // Always enabled if expanded, or add a toggle if needed. Currently assuming expanded means enabled.
                         // Actually, if rule.basicStats is null, we use default.
                         // But to "enable" custom basic stats, we might want a checkbox?
                         // For now, let's just show values. If user modifies them, we save them.
                         // If user wants "default", maybe they can clear it?
                         // Simpler: Just showing the editor updates the values.
                     )
                }
            }
        }
    }
}



/**
 * A text field for integer input that handles empty state gracefully.
 * Allow empty string to represent 0 or "no input" visually, but reports 0 to callback if empty.
 */
@Composable
fun IntTextField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(value.toString()) }
    
    // Sync with external value changes (e.g. reset or presets)
    LaunchedEffect(value) {
        // Only potential conflict is if user cleared text (text="") and value is 0.
        // We generally shouldn't overwrite user's empty input with "0" immediately.
        if (text.isEmpty() && value == 0) return@LaunchedEffect
        
        if (text.toIntOrNull() != value) {
            text = value.toString()
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            // Only allow numeric input (and empty)
            if (newText.isEmpty() || newText.all { it.isDigit() }) {
                text = newText
                onValueChange(newText.toIntOrNull() ?: 0)
            }
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    )
}

@Composable
fun BasicStatsEditor(
    config: RandomStoryGenerator.BasicStatsConfig,
    onConfigChange: (RandomStoryGenerator.BasicStatsConfig) -> Unit,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        // Helper for Min-Max Row
        @Composable
        fun StatRow(label: String, min: Int, max: Int, onMinChange: (Int) -> Unit, onMaxChange: (Int) -> Unit) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                IntTextField(
                    value = min,
                    onValueChange = onMinChange,
                    label = "Min",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("-", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
                IntTextField(
                    value = max,
                    onValueChange = onMaxChange,
                    label = "Max",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        StatRow("HP", config.hpMin, config.hpMax, 
            { onConfigChange(config.copy(hpMin = it)) }, 
            { onConfigChange(config.copy(hpMax = it)) }
        )
        StatRow("ATK", config.atkMin, config.atkMax, 
            { onConfigChange(config.copy(atkMin = it)) }, 
            { onConfigChange(config.copy(atkMax = it)) }
        )
        StatRow("DEF", config.defMin, config.defMax, 
            { onConfigChange(config.copy(defMin = it)) }, 
            { onConfigChange(config.copy(defMax = it)) }
        )
        StatRow("SPD", config.spdMin, config.spdMax, 
            { onConfigChange(config.copy(spdMin = it)) }, 
            { onConfigChange(config.copy(spdMax = it)) }
        )
    }
}

private fun getEntityTypeLabel(type: RandomStoryGenerator.EntityType): String {
    return when(type) {
        RandomStoryGenerator.EntityType.CHARACTER -> "角色"
        RandomStoryGenerator.EntityType.ENEMY -> "敌人"
        RandomStoryGenerator.EntityType.ITEM -> "道具"
        RandomStoryGenerator.EntityType.LOCATION -> "地点"
        RandomStoryGenerator.EntityType.FACTION -> "阵营"
        RandomStoryGenerator.EntityType.EVENT -> "事件"
        RandomStoryGenerator.EntityType.CLUE -> "线索"
        RandomStoryGenerator.EntityType.VARIABLE -> "变量"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleTemplateSelector(
    templates: List<com.novelsim.app.data.source.NameTemplate>,
    selectedId: String?,
    onSelectionChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTemplate = templates.find { it.id == selectedId }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            value = selectedTemplate?.description?.ifEmpty { selectedTemplate.id } ?: "请选择",
            onValueChange = {},
            label = { Text("选择模板") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            templates.forEach { template ->
                DropdownMenuItem(
                    text = { Text(template.description.ifEmpty { template.id }) },
                    onClick = {
                        onSelectionChange(template.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private fun getThemeName(theme: RandomStoryGenerator.StoryTheme): String = when (theme) {
    RandomStoryGenerator.StoryTheme.FANTASY -> "奇幻"
    RandomStoryGenerator.StoryTheme.SCI_FI -> "科幻"
    RandomStoryGenerator.StoryTheme.MYSTERY -> "悬疑"
    RandomStoryGenerator.StoryTheme.ROMANCE -> "言情"
    RandomStoryGenerator.StoryTheme.HORROR -> "恐怖"
}


