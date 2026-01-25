package com.novelsim.app.presentation.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.novelsim.app.domain.generator.RandomStoryGenerator
import com.novelsim.app.presentation.editor.EditorScreen
import cafe.adriel.voyager.koin.koinScreenModel
import com.novelsim.app.data.repository.StoryRepository
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
    var minItems by remember { mutableStateOf(2f) }
    var maxItems by remember { mutableStateOf(5f) }
    var minVariables by remember { mutableStateOf(2f) }
    var maxVariables by remember { mutableStateOf(5f) }
    var minEnemies by remember { mutableStateOf(2f) }
    var maxEnemies by remember { mutableStateOf(5f) }
    
    var minCharacters by remember { mutableStateOf(3f) }
    var maxCharacters by remember { mutableStateOf(6f) }
    
    var minLocations by remember { mutableStateOf(3f) }
    var maxLocations by remember { mutableStateOf(6f) }
    
    var minEvents by remember { mutableStateOf(2f) }
    var maxEvents by remember { mutableStateOf(4f) }
    
    var minClues by remember { mutableStateOf(2f) }
    var maxClues by remember { mutableStateOf(5f) }
    
    var minFactions by remember { mutableStateOf(2f) }
    var maxFactions by remember { mutableStateOf(4f) }
    
    // New Params
    var chaos by remember { mutableStateOf(0.1f) }
    var difficulty by remember { mutableStateOf(0.5f) }
    
    var selectedTheme by remember { mutableStateOf(RandomStoryGenerator.StoryTheme.FANTASY) }
    var selectedNamingStyle by remember { mutableStateOf(RandomStoryGenerator.NamingStyle.AUTO) }
    var useSeed by remember { mutableStateOf(false) }
    var seed by remember { mutableStateOf("") }
    
    // Loaded Templates
    // allTemplates passed from parent
    var selectedItemTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedEnemyTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCharacterTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedLocationTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedEventTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedClueTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFactionTemplates by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    LaunchedEffect(allTemplates) {
        // Default selections (intelligent defaults) if not already selected
        if (allTemplates.isNotEmpty()) {
            if (selectedItemTemplates.isEmpty()) {
                selectedItemTemplates = allTemplates.filter { it.id.contains("item") || it.id.contains("equipment") || it.id.contains("treasure") }.map { it.id }.toSet()
            }
            if (selectedEnemyTemplates.isEmpty()) {
                selectedEnemyTemplates = allTemplates.filter { it.id.contains("monster") || it.id.contains("enemy") }.map { it.id }.toSet()
            }
            if (selectedCharacterTemplates.isEmpty()) {
                 selectedCharacterTemplates = allTemplates.filter { it.id.contains("character") || it.id.contains("name") }.map { it.id }.toSet()
            }
            if (selectedLocationTemplates.isEmpty()) {
                 selectedLocationTemplates = allTemplates.filter { it.id.contains("place") || it.id.contains("location") }.map { it.id }.toSet()
            }
            if (selectedEventTemplates.isEmpty()) {
                 selectedEventTemplates = allTemplates.filter { it.id.contains("event") }.map { it.id }.toSet()
            }
             if (selectedClueTemplates.isEmpty()) {
                 selectedClueTemplates = allTemplates.filter { it.id.contains("clue") || it.id.contains("item") }.map { it.id }.toSet()
            }
            if (selectedFactionTemplates.isEmpty()) {
                 selectedFactionTemplates = allTemplates.filter { it.id.contains("faction") || it.id.contains("org") || it.id.contains("sect") || it.id.contains("skill") }.map { it.id }.toSet()
            }
        }
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
                        minRandomItems = minItems.toInt(),
                        maxRandomItems = maxItems.toInt(),
                        minRandomVariables = minVariables.toInt(),
                        maxRandomVariables = maxVariables.toInt(),
                        minRandomEnemies = minEnemies.toInt(),
                        maxRandomEnemies = maxEnemies.toInt(),
                        minRandomCharacters = minCharacters.toInt(),
                        maxRandomCharacters = maxCharacters.toInt(),
                        minRandomLocations = minLocations.toInt(),
                        maxRandomLocations = maxLocations.toInt(),
                        minRandomEvents = minEvents.toInt(),
                        maxRandomEvents = maxEvents.toInt(),
                        minRandomClues = minClues.toInt(),
                        maxRandomClues = maxClues.toInt(),
                        minRandomFactions = minFactions.toInt(),
                        maxRandomFactions = maxFactions.toInt(),
                        chaos = chaos,
                        difficulty = difficulty,
                        theme = selectedTheme,
                        namingStyle = selectedNamingStyle,
                        itemTemplateIds = selectedItemTemplates.toList(),
                        enemyTemplateIds = selectedEnemyTemplates.toList(),
                        characterTemplateIds = selectedCharacterTemplates.toList(),
                        locationTemplateIds = selectedLocationTemplates.toList(),
                        eventTemplateIds = selectedEventTemplates.toList(),
                        clueTemplateIds = selectedClueTemplates.toList(),
                        factionTemplateIds = selectedFactionTemplates.toList(),
                        seed = if (useSeed) seed.toLongOrNull() else null
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("命名风格", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RandomStoryGenerator.NamingStyle.entries.forEach { style ->
                            FilterChip(
                                selected = selectedNamingStyle == style,
                                onClick = { selectedNamingStyle = style },
                                label = { 
                                    Text(when(style) {
                                        RandomStoryGenerator.NamingStyle.AUTO -> "自动"
                                        RandomStoryGenerator.NamingStyle.CHINESE -> "中文"
                                        RandomStoryGenerator.NamingStyle.WESTERN -> "西方"
                                    })
                                },
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
            
            // Core Elements Generation
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("核心元素生成", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    SliderControl("随机道具模板", "${minItems.toInt()} - ${maxItems.toInt()}") {
                        RangeSlider(
                            value = minItems..maxItems,
                            onValueChange = { 
                                minItems = it.start
                                maxItems = it.endInclusive
                            },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }
                    
                    SliderControl("随机变量", "${minVariables.toInt()} - ${maxVariables.toInt()}") {
                        RangeSlider(
                            value = minVariables..maxVariables,
                            onValueChange = { 
                                minVariables = it.start
                                maxVariables = it.endInclusive
                            },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }
                    
                    SliderControl("随机敌人模板", "${minEnemies.toInt()} - ${maxEnemies.toInt()}") {
                        RangeSlider(
                            value = minEnemies..maxEnemies,
                            onValueChange = { 
                                minEnemies = it.start
                                maxEnemies = it.endInclusive
                            },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }
                    
                    SliderControl("随机角色数量", "${minCharacters.toInt()} - ${maxCharacters.toInt()}") {
                        RangeSlider(
                            value = minCharacters..maxCharacters,
                            onValueChange = { 
                                minCharacters = it.start
                                maxCharacters = it.endInclusive
                            },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }

                    SliderControl("随机地点数量", "${minLocations.toInt()} - ${maxLocations.toInt()}") {
                        RangeSlider(
                            value = minLocations..maxLocations,
                            onValueChange = { 
                                minLocations = it.start
                                maxLocations = it.endInclusive
                            },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }

                    SliderControl("随机事件数量", "${minEvents.toInt()} - ${maxEvents.toInt()}") {
                        RangeSlider(
                            value = minEvents..maxEvents,
                            onValueChange = { 
                                minEvents = it.start
                                maxEvents = it.endInclusive
                            },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }

                    SliderControl("随机线索数量", "${minClues.toInt()} - ${maxClues.toInt()}") {
                        RangeSlider(
                            value = minClues..maxClues,
                            onValueChange = { 
                                minClues = it.start
                                maxClues = it.endInclusive
                            },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }

                    SliderControl("随机阵营数量", "${minFactions.toInt()} - ${maxFactions.toInt()}") {
                        RangeSlider(
                            value = minFactions..maxFactions,
                            onValueChange = { 
                                minFactions = it.start
                                maxFactions = it.endInclusive
                            },
                            valueRange = 0f..10f,
                            steps = 9
                        )
                    }
                    
                    if (allTemplates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("名称模板选择", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Text("道具生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        TemplateSelector(
                            templates = allTemplates.filter { it.id.contains("item") || it.id.contains("equipment") },
                            selectedIds = selectedItemTemplates,
                            onSelectionChange = { selectedItemTemplates = it }
                        )

                        Text("敌人生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        TemplateSelector(
                            templates = allTemplates.filter { it.id.contains("monster") || it.id.contains("enemy") },
                            selectedIds = selectedEnemyTemplates,
                            onSelectionChange = { selectedEnemyTemplates = it }
                        )
                        
                        Text("角色生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        TemplateSelector(
                            templates = allTemplates.filter { it.id.contains("character") || it.id.contains("name") },
                            selectedIds = selectedCharacterTemplates,
                            onSelectionChange = { selectedCharacterTemplates = it }
                        )
                        
                        Text("地点生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        TemplateSelector(
                            templates = allTemplates.filter { it.id.contains("place") || it.id.contains("location") },
                            selectedIds = selectedLocationTemplates,
                            onSelectionChange = { selectedLocationTemplates = it }
                        )
                        
                        Text("事件生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        TemplateSelector(
                            templates = allTemplates.filter { it.id.contains("event") },
                            selectedIds = selectedEventTemplates,
                            onSelectionChange = { selectedEventTemplates = it }
                        )
                        
                        Text("线索生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        TemplateSelector(
                            templates = allTemplates.filter { it.id.contains("clue") },
                            selectedIds = selectedClueTemplates,
                            onSelectionChange = { selectedClueTemplates = it }
                        )
                        
                        Text("阵营生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        TemplateSelector(
                            templates = allTemplates.filter { it.id.contains("faction") || it.id.contains("skill") },
                            selectedIds = selectedFactionTemplates,
                            onSelectionChange = { selectedFactionTemplates = it }
                        )
                    }
                }
            }
            
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateSelector(
    templates: List<com.novelsim.app.data.source.NameTemplate>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        templates.forEach { template ->
            FilterChip(
                selected = selectedIds.contains(template.id),
                onClick = {
                    val newSelection = selectedIds.toMutableSet()
                    if (newSelection.contains(template.id)) {
                        newSelection.remove(template.id)
                    } else {
                        newSelection.add(template.id)
                    }
                    onSelectionChange(newSelection)
                },
                label = { Text(template.description.ifEmpty { template.id }) }
            )
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
