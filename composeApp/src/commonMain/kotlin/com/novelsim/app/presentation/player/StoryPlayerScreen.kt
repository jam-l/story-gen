package com.novelsim.app.presentation.player

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.novelsim.app.data.model.*
import com.novelsim.app.domain.rpg.BattleSystem
import org.koin.core.parameter.parametersOf

import com.novelsim.app.presentation.player.components.EntityVariableViewer

/**
 * 故事播放器屏幕
 */
data class StoryPlayerScreen(
    val storyId: String,
    val saveId: String? = null
) : Screen {
    
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<StoryPlayerScreenModel> { parametersOf(storyId) }
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 如果有存档ID，从存档加载
        LaunchedEffect(saveId) {
            saveId?.let { screenModel.loadFromSave(it) }
        }

        // 地点变换提示
        var lastLocationId by remember { mutableStateOf<String?>(uiState.currentNode?.locationId) }
        var showLocationToast by remember { mutableStateOf(false) }
        var locationTitle by remember { mutableStateOf("") }
        
        val story = uiState.story
        LaunchedEffect(uiState.currentNode) {
            val currentLocId = uiState.currentNode?.locationId
            if (currentLocId != lastLocationId && currentLocId != null && story != null) {
                val loc = story.locations.find { it.id == currentLocId }
                if (loc != null) {
                    locationTitle = loc.name
                    showLocationToast = true
                    kotlinx.coroutines.delay(3000)
                    showLocationToast = false
                }
            }
            lastLocationId = currentLocId
        }
        
        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    ErrorContent(
                        message = uiState.error!!,
                        onBack = { navigator.pop() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    when (uiState.displayMode) {
                        StoryPlayerScreenModel.DisplayMode.STORY -> {
                            StoryContent(
                                uiState = uiState,
                                onContinue = { screenModel.continueDialogue() },
                                onChoiceSelect = { screenModel.selectChoice(it) },
                                onBack = { navigator.pop() },
                                onSave = { screenModel.toggleSaveDialog() },
                                onInventory = { screenModel.toggleInventory() },
                                onStatus = { screenModel.toggleStatus() },
                                onHistory = { screenModel.toggleHistory() },
                                onVariable = { screenModel.toggleVariableViewer() },
                                onMap = { screenModel.toggleMap() }
                            )
                        }
                        StoryPlayerScreenModel.DisplayMode.BATTLE -> {
                            BattleContent(
                                battleState = uiState.battleState!!,
                                onAction = { screenModel.executeBattleAction(it) }
                            )
                        }
                        StoryPlayerScreenModel.DisplayMode.ENDING -> {
                            EndingContent(
                                ending = uiState.currentNode?.content as? NodeContent.Ending,
                                onBack = { navigator.pop() }
                            )
                        }
                        StoryPlayerScreenModel.DisplayMode.MAP -> {
                            MapContent(
                                story = uiState.story!!,
                                currentNode = uiState.currentNode,
                                onNodeSelect = { screenModel.selectNodeFromMap(it) },
                                onBack = { screenModel.toggleMap() }
                            )
                        }
                    }
                }
            }
            
            // 存档对话框
            if (uiState.showSaveDialog) {
                SaveDialog(
                    onSave = { screenModel.saveGame(it) },
                    onDismiss = { screenModel.toggleSaveDialog() }
                )
            }
            
            // 背包界面
            if (uiState.showInventory) {
                InventoryScreen(
                    inventory = uiState.gameState?.inventory ?: emptyList(),
                    itemInstances = uiState.gameState?.itemInstances ?: emptyMap(),
                    equipment = uiState.gameState?.equipment ?: Equipment(),
                    items = uiState.story?.items ?: emptyList(),
                    gold = uiState.gameState?.gold ?: 0,
                    onUseItem = { screenModel.useItem(it) },
                    onEquipItem = { screenModel.equipItem(it) },
                    onUnequipItem = { screenModel.unequipItem(it) },
                    onDismiss = { screenModel.toggleInventory() }
                )
            }
            
            // 状态面板
            if (uiState.showStatus) {
                StatusPanel(
                    gameState = uiState.gameState,
                    onDismiss = { screenModel.toggleStatus() }
                )
            }
            
            // 历史记录
            if (uiState.showHistory) {
                uiState.gameState?.history?.let { history ->
                    HistoryDialog(
                        history = history,
                        onDismiss = { screenModel.toggleHistory() }
                    )
                }
            }

            // 变量查看器
            if (uiState.showVariableViewer) {
                EntityVariableViewer(
                    entityVariables = uiState.gameState?.entityVariables ?: emptyMap(),
                    onDismiss = { screenModel.toggleVariableViewer() }
                )
            }

            // 地点切换提示 Toast
            AnimatedVisibility(
                visible = showLocationToast,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "到达新地点",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Text(
                                locationTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryContent(
    uiState: StoryPlayerScreenModel.UiState,
    onContinue: () -> Unit,
    onChoiceSelect: (ChoiceOption) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onInventory: () -> Unit,
    onStatus: () -> Unit,
    onHistory: () -> Unit,
    onVariable: () -> Unit,
    onMap: () -> Unit
) {
    val node = uiState.currentNode ?: return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // 顶部工具栏
        StoryTopBar(
            currentNode = node,
            locations = uiState.story?.locations ?: emptyList(),
            onBack = onBack,
            onSave = onSave,
            onInventory = onInventory,
            onStatus = onStatus,
            onHistory = onHistory,
            onVariable = onVariable,
            onMap = onMap
        )
        
        // 内容区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (node.type == NodeType.DIALOGUE) {
                            onContinue()
                        }
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // 角色名称（如果有）
                if (node.type == NodeType.DIALOGUE) {
                    val dialogue = node.content as? NodeContent.Dialogue
                    dialogue?.speaker?.let { speaker ->
                        Text(
                            text = speaker,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // 文字内容框
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = uiState.displayedText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp
                        )
                        
                        // 继续提示
                        if (!uiState.isTyping && node.type == NodeType.DIALOGUE) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "▼ 点击继续",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
                
                // 选项按钮
                if (node.type == NodeType.CHOICE) {
                    val choice = node.content as? NodeContent.Choice
                    choice?.options?.let { options ->
                        Spacer(modifier = Modifier.height(16.dp))
                        ChoiceButtons(
                            options = options,
                            onSelect = onChoiceSelect
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryTopBar(
    currentNode: StoryNode?,
    locations: List<Location>,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onInventory: () -> Unit,
    onStatus: () -> Unit,
    onHistory: () -> Unit,
    onVariable: () -> Unit,
    onMap: () -> Unit
) {
    TopAppBar(
        title = { 
            currentNode?.locationId?.let { locId ->
                val location = locations.find { it.id == locId }
                if (location != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp),
                        onClick = onMap
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = location.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = onVariable) {
                Icon(Icons.Default.Info, contentDescription = "变量")
            }
            IconButton(onClick = onStatus) {
                Icon(Icons.Default.Person, contentDescription = "状态")
            }
            IconButton(onClick = onInventory) {
                Icon(Icons.Default.Favorite, contentDescription = "背包")
            }
            IconButton(onClick = onHistory) {
                Icon(Icons.Default.DateRange, contentDescription = "历史")
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Done, contentDescription = "存档")
            }
            IconButton(onClick = onMap) {
                Icon(Icons.Default.Place, contentDescription = "地图", tint = MaterialTheme.colorScheme.primary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        windowInsets = WindowInsets(0.dp)
    )
}

@Composable
private fun ChoiceButtons(
    options: List<ChoiceOption>,
    onSelect: (ChoiceOption) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEachIndexed { index, option ->
            Button(
                onClick = { onSelect(option) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    text = "${index + 1}. ${option.text}",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun BattleContent(
    battleState: BattleSystem.BattleState,
    onAction: (BattleSystem.BattleAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        // 敌人信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3460))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = battleState.enemy.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 敌人HP条
                LinearProgressIndicator(
                    progress = { 
                        battleState.enemyCurrentHp.toFloat() / battleState.enemy.stats.maxHp 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFFE94560),
                    trackColor = Color(0xFF533483),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "HP: ${battleState.enemyCurrentHp} / ${battleState.enemy.stats.maxHp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 战斗日志
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E).copy(alpha = 0.8f))
        ) {
            LazyColumn(
                modifier = Modifier.padding(12.dp),
                reverseLayout = true
            ) {
                items(battleState.logs.reversed()) { log ->
                    val color = when (log.type) {
                        BattleSystem.BattleLog.LogType.DAMAGE -> Color(0xFFE94560)
                        BattleSystem.BattleLog.LogType.HEAL -> Color(0xFF00D9A5)
                        BattleSystem.BattleLog.LogType.BUFF -> Color(0xFFFFBD39)
                        BattleSystem.BattleLog.LogType.CRITICAL -> Color(0xFFFF6B6B)
                        else -> Color.White.copy(alpha = 0.8f)
                    }
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 玩家状态
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3460))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "你",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                // HP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HP", color = Color.White, modifier = Modifier.width(32.dp))
                    LinearProgressIndicator(
                        progress = { battleState.playerStats.hpPercent },
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF00D9A5),
                        trackColor = Color(0xFF533483),
                    )
                    Text(
                        text = "${battleState.playerStats.currentHp}/${battleState.playerStats.maxHp}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // MP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MP", color = Color.White, modifier = Modifier.width(32.dp))
                    LinearProgressIndicator(
                        progress = { battleState.playerStats.mpPercent },
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF4D96FF),
                        trackColor = Color(0xFF533483),
                    )
                    Text(
                        text = "${battleState.playerStats.currentMp}/${battleState.playerStats.maxMp}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 战斗按钮
        if (battleState.phase == BattleSystem.BattlePhase.PLAYER_TURN) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BattleButton(
                    text = "攻击",
                    color = Color(0xFFE94560),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(BattleSystem.BattleAction.Attack) }
                )
                BattleButton(
                    text = "防御",
                    color = Color(0xFF4D96FF),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(BattleSystem.BattleAction.Defend) }
                )
                BattleButton(
                    text = "逃跑",
                    color = Color(0xFFFFBD39),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(BattleSystem.BattleAction.Flee) }
                )
            }
        } else if (!battleState.isFinished) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun BattleButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EndingContent(
    ending: NodeContent.Ending?,
    onBack: () -> Unit
) {
    val backgroundColor = when (ending?.endingType) {
        EndingType.GOOD -> listOf(Color(0xFF2D6A4F), Color(0xFF1B4332))
        EndingType.BAD -> listOf(Color(0xFF6B2737), Color(0xFF4A1A24))
        EndingType.SECRET -> listOf(Color(0xFF4A1A6B), Color(0xFF2D1040))
        else -> listOf(Color(0xFF495057), Color(0xFF343A40))
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(backgroundColor))
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "~ ${ending?.title ?: "故事结束"} ~",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = ending?.description ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = "返回主页",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SaveDialog(
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择存档槽位") },
        text = {
            Column {
                (1..5).forEach { slot ->
                    TextButton(
                        onClick = { onSave(slot) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("槽位 $slot")
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun StatusPanel(
    gameState: GameState?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("角色状态") },
        text = {
            gameState?.playerStats?.let { stats ->
                Column {
                    StatusRow("等级", "${stats.level}")
                    StatusRow("HP", "${stats.currentHp} / ${stats.maxHp}")
                    StatusRow("MP", "${stats.currentMp} / ${stats.maxMp}")
                    StatusRow("攻击", "${stats.attack}")
                    StatusRow("防御", "${stats.defense}")
                    StatusRow("速度", "${stats.speed}")
                    StatusRow("经验", "${stats.exp} / ${stats.expToNextLevel}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("返回")
        }
    }
}

@Composable
private fun HistoryDialog(
    history: List<HistoryItem>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("剧情回顾") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                reverseLayout = true
            ) {
                items(history.reversed()) { item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = item.text,
                            style = if (item.type == HistoryType.CHOICE) 
                                MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary)
                                else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (item.type == HistoryType.CHOICE) FontWeight.Bold else FontWeight.Normal
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapContent(
    story: Story,
    currentNode: StoryNode?,
    onNodeSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    // 1. 计算地图边界
    val allLocations = story.locations
    // if (allLocations.isEmpty()) return // REMOVED EARLY RETURN

    // 计算边界 (基于地点坐标，如果为空则使用默认值)
    val minX = if (allLocations.isNotEmpty()) allLocations.minOf { it.position.x } else 0f
    val maxX = if (allLocations.isNotEmpty()) allLocations.maxOf { it.position.x } else 1000f
    val minY = if (allLocations.isNotEmpty()) allLocations.minOf { it.position.y } else 0f
    val maxY = if (allLocations.isNotEmpty()) allLocations.maxOf { it.position.y } else 2000f
    
    // 增加一些边距 (DP)
    val paddingDp = 300.dp 
    val paddingVal = 300f 
    
    // 计算画布这一层的相对偏移，保证所有内容都在正坐标区域
    val offsetX = -minX + paddingVal
    val offsetY = -minY + paddingVal
    
    val mapWidthDp = (maxX - minX + paddingVal * 2).dp
    val mapHeightDp = (maxY - minY + paddingVal * 2).dp
    
    val vScrollState = rememberScrollState()
    val hScrollState = rememberScrollState()
    val density = LocalDensity.current
    
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    
    // 自动滚动到当前节点所在区域
    LaunchedEffect(currentNode) {
        currentNode?.locationId?.let { locId ->
            story.locations.find { it.id == locId }?.let { loc ->
                // targetY 是逻辑坐标 (DP)
                val targetYSwapped = loc.position.y + offsetY
                val screenHeightPx = 2000f // 估算值
                
                // ScrollState 需要 Pixels
                val targetYPx = with(density) { targetYSwapped.dp.toPx() }
                vScrollState.animateScrollTo((targetYPx - screenHeightPx / 2).toInt().coerceAtLeast(0))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("世界地图 (共 ${story.locations.size} 个区域)", style = MaterialTheme.typography.titleMedium)
                        currentNode?.locationId?.let { locId ->
                            story.locations.find { it.id == locId }?.let { loc ->
                                Text("当前区域: ${loc.name}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF2C3E50)) // 深色海洋/背景
        ) {
            if (allLocations.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "当前故事没有生成地图数据",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray
                    )
                    Text(
                        "请生成一个新的故事以体验地图功能",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                // 可滚动的地图容器
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(vScrollState)
                        .horizontalScroll(hScrollState)
                ) {
                    Box(
                        modifier = Modifier.size(width = mapWidthDp, height = mapHeightDp) 
                    ) {
                        // 绘制背景网格和连接线
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // 绘制经纬度网格线
                            val gridSizeDp = 200.dp
                            val gridSizePx = gridSizeDp.toPx()
                            val width = size.width
                            val height = size.height
                            
                            // 纵向线
                            for (x in 0 until (width / gridSizePx).toInt()) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = androidx.compose.ui.geometry.Offset(x * gridSizePx, 0f),
                                    end = androidx.compose.ui.geometry.Offset(x * gridSizePx, height),
                                    strokeWidth = 1f
                                )
                            }
                            // 横向线
                            for (y in 0 until (height / gridSizePx).toInt()) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = androidx.compose.ui.geometry.Offset(0f, y * gridSizePx),
                                    end = androidx.compose.ui.geometry.Offset(width, y * gridSizePx),
                                    strokeWidth = 1f
                                )
                            }

                            // 绘制区域间的连接路径 (更明显的道路)
                            story.locations.forEach { location ->
                                val startX = (location.position.x + offsetX).dp.toPx()
                                val startY = (location.position.y + offsetY).dp.toPx()
                                
                                location.connectedLocationIds.forEach { targetId ->
                                    story.locations.find { it.id == targetId }?.let { target ->
                                        if (location.id < target.id) {
                                            val endX = (target.position.x + offsetX).dp.toPx()
                                            val endY = (target.position.y + offsetY).dp.toPx()
                                            
                                            // 边框
                                            drawLine(
                                                color = Color.Black.copy(alpha = 0.5f),
                                                start = androidx.compose.ui.geometry.Offset(startX, startY),
                                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                                strokeWidth = 12f
                                            )
                                            // 金色道路
                                            drawLine(
                                                color = Color(0xFFD4A056), // 土黄色
                                                start = androidx.compose.ui.geometry.Offset(startX, startY),
                                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                                strokeWidth = 6f,
                                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                                            )
                                        }
                                    }
                                }
                            }

                            // 绘制连接线 (节点间，细虚线)
                            story.nodes.values.forEach { node ->
                                 node.connections.forEach { conn ->
                                    story.nodes[conn.targetNodeId]?.let { target ->
                                        // 坐标转换: Model(DP) -> Add Offset -> To Px
                                        val startX = (node.position.x + offsetX).dp.toPx()
                                        val startY = (node.position.y + offsetY).dp.toPx()
                                        val endX = (target.position.x + offsetX).dp.toPx()
                                        val endY = (target.position.y + offsetY).dp.toPx()
                                        
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.1f),
                                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                                            strokeWidth = 1f,
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    }
                                }
                            }
                        }
                        
                            // 4. 绘制区域 (可视化优化 - 不规则多边形)
                            story.locations.forEach { location ->
                                val locXDp = (location.position.x + offsetX).dp
                                val locYDp = (location.position.y + offsetY).dp
                                val isCurrentLoc = currentNode?.locationId == location.id
                                val color = getColorForLocation(location.name)
                                
                                // 根据半径计算显示大小
                                val displaySizeDp = (location.radius / 400f * 120f).coerceIn(100f, 260f).dp
                                val halfSizeDp = displaySizeDp / 2
                                
                                // 区域容器
                                Box(
                                    modifier = Modifier
                                        .absoluteOffset(x = locXDp - halfSizeDp, y = locYDp - halfSizeDp) 
                                        .size(displaySizeDp)
                                        .clickable { selectedLocation = location },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 绘制不规则区域背景
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        val radius = size.minDimension / 2
                                        val centerX = w / 2
                                        val centerY = h / 2
                                        
                                        // 伪随机生成每块地的不规则形状
                                        val seed = location.name.hashCode()
                                        val random = kotlin.random.Random(seed)
                                        val points = 8 + random.nextInt(4) 
                                        
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                             for (i in 0 until points) {
                                                 val angle = (i.toFloat() / points) * 2 * 3.14159f
                                                 // 半径随机抖动
                                                 val r = radius * (0.7f + random.nextFloat() * 0.3f)
                                                 val px = centerX + r * kotlin.math.cos(angle)
                                                 val py = centerY + r * kotlin.math.sin(angle)
                                                 
                                                 if (i == 0) moveTo(px, py) else lineTo(px, py)
                                             }
                                             close()
                                        }
                                        
                                        // 阴影
                                        drawPath(
                                            path = path, 
                                            color = Color.Black.copy(alpha = 0.3f),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                                        )
                                        
                                        // 填充
                                        drawPath(
                                            path = path,
                                            color = color.copy(alpha = 0.6f)
                                        )
                                        
                                        // 内部纹理
                                        drawPath(
                                            path = path,
                                            color = color.copy(alpha = 0.9f),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 3f,
                                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                            )
                                        )
                                    }
                                    
                                    // 中央图标和文字
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        val icon = when {
                                            location.name.contains("城") || location.name.contains("村") -> Icons.Default.Home
                                            else -> Icons.Default.Place
                                        }
                                        
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = location.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            // maxLines = 1, // Remove limit to show 2 lines
                                            textAlign = TextAlign.Center,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    
                                    // 当前位置标记
                                    if (isCurrentLoc) {
                                        Icon(
                                            Icons.Default.LocationOn, 
                                            contentDescription = "当前位置",
                                            tint = Color(0xFFFFD700), // 金色
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 0.dp, y = 0.dp)
                                                .size(28.dp)
                                        )
                                    }
                                }
                            }
                    }
                }
                
                // 底部图例
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(Color.Green, CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Text("当前位置", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(Color(0xFFE74C3C), CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Text("区域威胁", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        
        // 区域详情 Bottom Sheet
        if (selectedLocation != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedLocation = null }
            ) {
                RegionDetailContent(
                    story = story,
                    location = selectedLocation!!,
                    currentNodeId = currentNode?.id,
                    onNodeSelect = { 
                        onNodeSelect(it)
                        selectedLocation = null // 选完关闭
                    }
                )
            }
        }
    }
}

@Composable
private fun RegionDetailContent(
    story: Story,
    location: Location,
    currentNodeId: String?,
    onNodeSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp) // 底部安全区
    ) {
        // 标题栏
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(location.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            if (currentNodeId != null && story.nodes[currentNodeId]?.locationId == location.id) {
                Text("当前所在", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(location.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 区域内的实体列表
        val locationNodes = story.nodes.values.filter { it.locationId == location.id }
        val enemies = locationNodes.filter { it.type == NodeType.BATTLE }.mapNotNull { node ->
             if (node.content is NodeContent.Battle) {
                 story.enemies.find { it.id == node.content.enemyId }?.let { enemy ->
                     node to enemy
                 }
             } else null
        }.distinctBy { it.second.id } // 去重显示种类
        
        val items = locationNodes.filter { it.type == NodeType.ITEM }.mapNotNull { node ->
            if (node.content is NodeContent.ItemAction) {
                // 这里简单展示节点上的物品名
                 node to (node.content.itemName ?: "未知物品")
            } else null
        }
        
        // 怪物列表
        if (enemies.isNotEmpty()) {
            Text("区域威胁", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(enemies) { (node, enemy) ->
                     Card(
                        modifier = Modifier.width(160.dp).clickable { onNodeSelect(node.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(enemy.name, fontWeight = FontWeight.Bold)
                            Text("HP: ${enemy.stats.maxHp}  ATK: ${enemy.stats.attack}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                             Button(
                                onClick = { onNodeSelect(node.id) },
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("挑战")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 探索点/NPC
        val dialogueNodes = locationNodes.filter { it.type == NodeType.DIALOGUE || it.type == NodeType.CHOICE }
        if (dialogueNodes.isNotEmpty()) {
             Text("探索点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
             Spacer(modifier = Modifier.height(8.dp))
             
             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                 dialogueNodes.take(5).forEach { node ->
                     Row(
                         modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNodeSelect(node.id) }
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp))
                         Spacer(modifier = Modifier.width(12.dp))
                         Text(
                             text = if (node.type == NodeType.CHOICE) "一个抉择点" else "对话/事件",
                             style = MaterialTheme.typography.bodyMedium
                         )
                         Spacer(modifier = Modifier.weight(1f))
                         Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                     }
                 }
                 if (dialogueNodes.size > 5) {
                     Text("...以及其他 ${dialogueNodes.size - 5} 个地点", style = MaterialTheme.typography.bodySmall)
                 }
             }
        }
    }
}

// 辅助函数：根据名称生成固定颜色
private fun getColorForLocation(name: String): Color {
    val hash = name.hashCode()
    return Color(
        red = (hash and 0xFF) / 255f,
        green = ((hash shr 8) and 0xFF) / 255f,
        blue = ((hash shr 16) and 0xFF) / 255f,
        alpha = 1f
    ).copy(alpha = 1f) // 确保不透明
}
