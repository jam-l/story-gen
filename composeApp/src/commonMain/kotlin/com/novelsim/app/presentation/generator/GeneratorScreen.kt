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
        
        GeneratorScreenContent(
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
            val generator = RandomStoryGenerator(config)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratorScreenContent(
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
    
    // New Params
    var chaos by remember { mutableStateOf(0.1f) }
    var difficulty by remember { mutableStateOf(0.5f) }
    
    var selectedTheme by remember { mutableStateOf(RandomStoryGenerator.StoryTheme.FANTASY) }
    var useSeed by remember { mutableStateOf(false) }
    var seed by remember { mutableStateOf("") }
    
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
                        // Chips row might overflow, let's just show a few or use FlowRow if available
                        // Or just a simple Row with weight
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

private fun getThemeName(theme: RandomStoryGenerator.StoryTheme): String = when (theme) {
    RandomStoryGenerator.StoryTheme.FANTASY -> "奇幻"
    RandomStoryGenerator.StoryTheme.SCI_FI -> "科幻"
    RandomStoryGenerator.StoryTheme.MYSTERY -> "悬疑"
    RandomStoryGenerator.StoryTheme.ROMANCE -> "言情"
    RandomStoryGenerator.StoryTheme.HORROR -> "恐怖"
}
