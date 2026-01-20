package com.novelsim.app.presentation.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
        
        Box(modifier = Modifier.fillMaxSize()) {
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
                                onStatus = { screenModel.toggleStatus() }
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
            
            // 状态面板
            if (uiState.showStatus) {
                StatusPanel(
                    gameState = uiState.gameState,
                    onDismiss = { screenModel.toggleStatus() }
                )
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
    onStatus: () -> Unit
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
            onBack = onBack,
            onSave = onSave,
            onInventory = onInventory,
            onStatus = onStatus
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
    onBack: () -> Unit,
    onSave: () -> Unit,
    onInventory: () -> Unit,
    onStatus: () -> Unit
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = onStatus) {
                Icon(Icons.Default.Person, contentDescription = "状态")
            }
            IconButton(onClick = onInventory) {
                Icon(Icons.Default.Favorite, contentDescription = "背包")
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Done, contentDescription = "存档")
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
