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
import com.novelsim.app.presentation.generator.components.CustomStatConfigEditor
import com.novelsim.app.domain.generator.RandomStoryGenerator.CustomStatConfig

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
    
    // Enemy Stats Params
    var enemyMinHp by remember { mutableStateOf(50f) }
    var enemyMaxHp by remember { mutableStateOf(200f) }
    var enemyMinAtk by remember { mutableStateOf(5f) }
    var enemyMaxAtk by remember { mutableStateOf(20f) }
    var enemyMinDef by remember { mutableStateOf(0f) }
    var enemyMaxDef by remember { mutableStateOf(10f) }
    var enemyMinSpeed by remember { mutableStateOf(5f) }
    var enemyMaxSpeed by remember { mutableStateOf(15f) }

    // Character Stats Params
    var characterMinHp by remember { mutableStateOf(80f) }
    var characterMaxHp by remember { mutableStateOf(150f) }
    var characterMinAtk by remember { mutableStateOf(10f) }
    var characterMaxAtk by remember { mutableStateOf(30f) }
    var characterMinDef by remember { mutableStateOf(5f) }
    var characterMaxDef by remember { mutableStateOf(15f) }
    var characterMinSpeed by remember { mutableStateOf(8f) }
    var characterMaxSpeed by remember { mutableStateOf(18f) }
    
    var selectedTheme by remember { mutableStateOf(RandomStoryGenerator.StoryTheme.FANTASY) }
    var selectedNamingStyle by remember { mutableStateOf(RandomStoryGenerator.NamingStyle.AUTO) }
    var useSeed by remember { mutableStateOf(false) }
    var seed by remember { mutableStateOf("") }
    
    // Loaded Templates
    // allTemplates passed from parent
    var selectedItemTemplate by remember { mutableStateOf<String?>(null) }
    var selectedEnemyTemplate by remember { mutableStateOf<String?>(null) }
    var selectedCharacterTemplate by remember { mutableStateOf<String?>(null) }
    var selectedLocationTemplate by remember { mutableStateOf<String?>(null) }
    var selectedEventTemplate by remember { mutableStateOf<String?>(null) }
    var selectedClueTemplate by remember { mutableStateOf<String?>(null) }
    var selectedFactionTemplate by remember { mutableStateOf<String?>(null) }
    
    var customStatConfigs by remember { mutableStateOf<List<CustomStatConfig>>(emptyList()) }
    
    LaunchedEffect(allTemplates) {
        // Init logic if needed in future
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
                        itemTemplateIds = listOfNotNull(selectedItemTemplate),
                        enemyTemplateIds = listOfNotNull(selectedEnemyTemplate),
                        characterTemplateIds = listOfNotNull(selectedCharacterTemplate),
                        locationTemplateIds = listOfNotNull(selectedLocationTemplate),
                        eventTemplateIds = listOfNotNull(selectedEventTemplate),
                        clueTemplateIds = listOfNotNull(selectedClueTemplate),
                        factionTemplateIds = listOfNotNull(selectedFactionTemplate),
                        seed = if (useSeed) seed.toLongOrNull() else null,
                        customStats = customStatConfigs,
                        enemyMinHp = enemyMinHp.toInt(),
                        enemyMaxHp = enemyMaxHp.toInt(),
                        enemyMinAtk = enemyMinAtk.toInt(),
                        enemyMaxAtk = enemyMaxAtk.toInt(),
                        enemyMinDef = enemyMinDef.toInt(),
                        enemyMaxDef = enemyMaxDef.toInt(),
                        enemyMinSpeed = enemyMinSpeed.toInt(),
                        enemyMaxSpeed = enemyMaxSpeed.toInt(),
                        characterMinHp = characterMinHp.toInt(),
                        characterMaxHp = characterMaxHp.toInt(),
                        characterMinAtk = characterMinAtk.toInt(),
                        characterMaxAtk = characterMaxAtk.toInt(),
                        characterMinDef = characterMinDef.toInt(),
                        characterMaxDef = characterMaxDef.toInt(),
                        characterMinSpeed = characterMinSpeed.toInt(),
                        characterMaxSpeed = characterMaxSpeed.toInt()
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
            
            // Custom Stats Config
            CustomStatConfigEditor(
                configs = customStatConfigs,
                onConfigsChange = { customStatConfigs = it }
            )
            
            // Enemy Base Stats Config
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("敌人基础属性 (Base Stats)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    SliderControl("生命值 (HP)", "${enemyMinHp.toInt()} - ${enemyMaxHp.toInt()}") {
                        RangeSlider(
                            value = enemyMinHp..enemyMaxHp,
                            onValueChange = { 
                                enemyMinHp = it.start
                                enemyMaxHp = it.endInclusive
                            },
                            valueRange = 1f..1000f,
                            steps = 0
                        )
                    }
                    
                    SliderControl("攻击力 (ATK)", "${enemyMinAtk.toInt()} - ${enemyMaxAtk.toInt()}") {
                        RangeSlider(
                            value = enemyMinAtk..enemyMaxAtk,
                            onValueChange = { 
                                enemyMinAtk = it.start
                                enemyMaxAtk = it.endInclusive
                            },
                            valueRange = 1f..200f,
                            steps = 0
                        )
                    }
                    
                    SliderControl("防御力 (DEF)", "${enemyMinDef.toInt()} - ${enemyMaxDef.toInt()}") {
                        RangeSlider(
                            value = enemyMinDef..enemyMaxDef,
                            onValueChange = { 
                                enemyMinDef = it.start
                                enemyMaxDef = it.endInclusive
                            },
                            valueRange = 0f..100f,
                            steps = 0
                        )
                    }
                     
                    SliderControl("速度 (SPD)", "${enemyMinSpeed.toInt()} - ${enemyMaxSpeed.toInt()}") {
                        RangeSlider(
                            value = enemyMinSpeed..enemyMaxSpeed,
                            onValueChange = { 
                                enemyMinSpeed = it.start
                                enemyMaxSpeed = it.endInclusive
                            },
                            valueRange = 1f..100f,
                            steps = 0
                        )
                    }
                }
            }

            // Character Base Stats Config
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("角色基础属性 (Base Stats)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    SliderControl("生命值 (HP)", "${characterMinHp.toInt()} - ${characterMaxHp.toInt()}") {
                        RangeSlider(
                            value = characterMinHp..characterMaxHp,
                            onValueChange = { 
                                characterMinHp = it.start
                                characterMaxHp = it.endInclusive
                            },
                            valueRange = 1f..1000f,
                            steps = 0
                        )
                    }
                    
                    SliderControl("攻击力 (ATK)", "${characterMinAtk.toInt()} - ${characterMaxAtk.toInt()}") {
                        RangeSlider(
                            value = characterMinAtk..characterMaxAtk,
                            onValueChange = { 
                                characterMinAtk = it.start
                                characterMaxAtk = it.endInclusive
                            },
                            valueRange = 1f..200f,
                            steps = 0
                        )
                    }
                    
                    SliderControl("防御力 (DEF)", "${characterMinDef.toInt()} - ${characterMaxDef.toInt()}") {
                        RangeSlider(
                            value = characterMinDef..characterMaxDef,
                            onValueChange = { 
                                characterMinDef = it.start
                                characterMaxDef = it.endInclusive
                            },
                            valueRange = 0f..100f,
                            steps = 0
                        )
                    }
                     
                    SliderControl("速度 (SPD)", "${characterMinSpeed.toInt()} - ${characterMaxSpeed.toInt()}") {
                        RangeSlider(
                            value = characterMinSpeed..characterMaxSpeed,
                            onValueChange = { 
                                characterMinSpeed = it.start
                                characterMaxSpeed = it.endInclusive
                            },
                            valueRange = 1f..100f,
                            steps = 0
                        )
                    }
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
                        SingleTemplateSelector(
                            templates = allTemplates,
                            selectedId = selectedItemTemplate,
                            onSelectionChange = { selectedItemTemplate = it }
                        )

                        Text("敌人生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        SingleTemplateSelector(
                            templates = allTemplates,
                            selectedId = selectedEnemyTemplate,
                            onSelectionChange = { selectedEnemyTemplate = it }
                        )
                        
                        Text("角色生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        SingleTemplateSelector(
                            templates = allTemplates,
                            selectedId = selectedCharacterTemplate,
                            onSelectionChange = { selectedCharacterTemplate = it }
                        )
                        
                        Text("地点生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        SingleTemplateSelector(
                            templates = allTemplates,
                            selectedId = selectedLocationTemplate,
                            onSelectionChange = { selectedLocationTemplate = it }
                        )
                        
                        Text("事件生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        SingleTemplateSelector(
                            templates = allTemplates,
                            selectedId = selectedEventTemplate,
                            onSelectionChange = { selectedEventTemplate = it }
                        )
                        
                        Text("线索生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        SingleTemplateSelector(
                            templates = allTemplates,
                            selectedId = selectedClueTemplate,
                            onSelectionChange = { selectedClueTemplate = it }
                        )
                        
                        Text("阵营生成模板", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        SingleTemplateSelector(
                            templates = allTemplates,
                            selectedId = selectedFactionTemplate,
                            onSelectionChange = { selectedFactionTemplate = it }
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
