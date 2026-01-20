package com.novelsim.app.presentation.player

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.novelsim.app.data.model.*
import com.novelsim.app.data.repository.SaveRepository
import com.novelsim.app.domain.engine.StoryEngine
import com.novelsim.app.domain.rpg.BattleSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 故事播放器 ScreenModel
 */
class StoryPlayerScreenModel(
    private val storyId: String,
    private val storyEngine: StoryEngine,
    private val saveRepository: SaveRepository
) : ScreenModel {
    
    /**
     * UI 状态
     */
    data class UiState(
        val isLoading: Boolean = true,
        val error: String? = null,
        val story: Story? = null,
        val currentNode: StoryNode? = null,
        val gameState: GameState? = null,
        val displayMode: DisplayMode = DisplayMode.STORY,
        val battleState: BattleSystem.BattleState? = null,
        val showSaveDialog: Boolean = false,
        val showInventory: Boolean = false,
        val showStatus: Boolean = false,
        val isTyping: Boolean = false,
        val displayedText: String = ""
    )
    
    /**
     * 显示模式
     */
    enum class DisplayMode {
        STORY,      // 故事模式
        BATTLE,     // 战斗模式
        ENDING      // 结局模式
    }
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val battleSystem = BattleSystem()
    
    init {
        loadStory()
    }
    
    /**
     * 加载故事
     */
    private fun loadStory() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            storyEngine.loadStory(storyId).fold(
                onSuccess = { node ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        story = storyEngine.getCurrentStory(),
                        currentNode = node,
                        gameState = storyEngine.getGameState(),
                        error = null
                    )
                    processNode(node)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    /**
     * 从存档加载
     */
    fun loadFromSave(saveId: String) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val save = saveRepository.getSaveById(saveId)
            val story = storyEngine.getCurrentStory()
            
            if (save != null && story != null) {
                storyEngine.loadFromSave(story, save).fold(
                    onSuccess = { node ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            currentNode = node,
                            gameState = storyEngine.getGameState()
                        )
                        processNode(node)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            }
        }
    }
    
    /**
     * 处理节点显示
     */
    private fun processNode(node: StoryNode) {
        when (node.type) {
            NodeType.DIALOGUE -> {
                val content = node.content as? NodeContent.Dialogue
                startTypingEffect(content?.text ?: "")
            }
            NodeType.CHOICE -> {
                _uiState.value = _uiState.value.copy(
                    isTyping = false,
                    displayedText = (node.content as? NodeContent.Choice)?.prompt ?: ""
                )
            }
            NodeType.BATTLE -> {
                startBattle(node)
            }
            NodeType.END -> {
                _uiState.value = _uiState.value.copy(
                    displayMode = DisplayMode.ENDING,
                    isTyping = false
                )
            }
            else -> {
                // 自动跳转到下一个节点
                continueToNextNode()
            }
        }
    }
    
    /**
     * 打字机效果
     */
    private fun startTypingEffect(text: String) {
        _uiState.value = _uiState.value.copy(
            isTyping = true,
            displayedText = ""
        )
        
        screenModelScope.launch {
            text.forEachIndexed { index, _ ->
                kotlinx.coroutines.delay(30) // 每个字符的延迟
                _uiState.value = _uiState.value.copy(
                    displayedText = text.substring(0, index + 1)
                )
            }
            _uiState.value = _uiState.value.copy(isTyping = false)
        }
    }
    
    /**
     * 跳过打字效果
     */
    fun skipTyping() {
        val node = _uiState.value.currentNode ?: return
        val fullText = when (val content = node.content) {
            is NodeContent.Dialogue -> content.text
            is NodeContent.Choice -> content.prompt
            else -> ""
        }
        _uiState.value = _uiState.value.copy(
            isTyping = false,
            displayedText = fullText
        )
    }
    
    /**
     * 继续对话
     */
    fun continueDialogue() {
        if (_uiState.value.isTyping) {
            skipTyping()
            return
        }
        
        continueToNextNode()
    }
    
    /**
     * 跳转到下一个节点
     */
    private fun continueToNextNode() {
        storyEngine.continueDialogue().fold(
            onSuccess = { node ->
                _uiState.value = _uiState.value.copy(
                    currentNode = node,
                    gameState = storyEngine.getGameState()
                )
                processNode(node)
            },
            onFailure = { /* 没有下一个节点，可能需要用户选择 */ }
        )
    }
    
    /**
     * 选择选项
     */
    fun selectChoice(option: ChoiceOption) {
        storyEngine.processChoice(option).fold(
            onSuccess = { node ->
                _uiState.value = _uiState.value.copy(
                    currentNode = node,
                    gameState = storyEngine.getGameState()
                )
                processNode(node)
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        )
    }
    
    /**
     * 开始战斗
     */
    private fun startBattle(node: StoryNode) {
        val content = node.content as? NodeContent.Battle ?: return
        val story = _uiState.value.story ?: return
        val gameState = _uiState.value.gameState ?: return
        
        val enemy = story.enemies.find { it.id == content.enemyId } ?: return
        
        val battleState = battleSystem.startBattle(gameState.playerStats, enemy)
        
        _uiState.value = _uiState.value.copy(
            displayMode = DisplayMode.BATTLE,
            battleState = battleState
        )
    }
    
    /**
     * 执行战斗动作
     */
    fun executeBattleAction(action: BattleSystem.BattleAction) {
        val currentBattleState = _uiState.value.battleState ?: return
        
        // 执行玩家回合
        var newState = battleSystem.executePlayerTurn(currentBattleState, action)
        _uiState.value = _uiState.value.copy(battleState = newState)
        
        // 如果战斗未结束且是敌人回合，执行敌人回合
        if (!newState.isFinished && newState.phase == BattleSystem.BattlePhase.ENEMY_TURN) {
            screenModelScope.launch {
                kotlinx.coroutines.delay(1000) // 等待动画
                newState = battleSystem.executeEnemyTurn(newState)
                _uiState.value = _uiState.value.copy(battleState = newState)
                
                // 检查战斗是否结束
                if (newState.isFinished) {
                    handleBattleEnd(newState)
                }
            }
        } else if (newState.isFinished) {
            handleBattleEnd(newState)
        }
    }
    
    /**
     * 处理战斗结束
     */
    private fun handleBattleEnd(battleState: BattleSystem.BattleState) {
        val node = _uiState.value.currentNode ?: return
        val content = node.content as? NodeContent.Battle ?: return
        
        screenModelScope.launch {
            kotlinx.coroutines.delay(1500) // 显示结果
            
            val nextNodeId = if (battleState.phase == BattleSystem.BattlePhase.VICTORY) {
                // 处理战斗奖励
                battleSystem.calculateReward(battleState)?.let { reward ->
                    // TODO: 应用奖励
                }
                content.winNextNodeId
            } else {
                content.loseNextNodeId
            }
            
            // 更新玩家状态
            _uiState.value = _uiState.value.copy(
                gameState = _uiState.value.gameState?.copy(
                    playerStats = battleState.playerStats
                )
            )
            
            // 跳转到下一个节点
            storyEngine.navigateToNode(nextNodeId).fold(
                onSuccess = { nextNode ->
                    _uiState.value = _uiState.value.copy(
                        displayMode = DisplayMode.STORY,
                        currentNode = nextNode,
                        battleState = null
                    )
                    processNode(nextNode)
                },
                onFailure = { }
            )
        }
    }
    
    /**
     * 保存游戏
     */
    fun saveGame(slotIndex: Int) {
        screenModelScope.launch {
            val story = _uiState.value.story ?: return@launch
            val gameState = storyEngine.getGameState() ?: return@launch
            val currentNode = _uiState.value.currentNode
            
            val preview = when (val content = currentNode?.content) {
                is NodeContent.Dialogue -> content.text.take(50)
                is NodeContent.Choice -> content.prompt.take(50)
                else -> null
            }
            
            saveRepository.saveGame(
                slotIndex = slotIndex,
                storyId = story.id,
                storyTitle = story.title,
                gameState = gameState,
                currentNodePreview = preview
            )
            
            _uiState.value = _uiState.value.copy(showSaveDialog = false)
        }
    }
    
    /**
     * 显示/隐藏存档对话框
     */
    fun toggleSaveDialog() {
        _uiState.value = _uiState.value.copy(
            showSaveDialog = !_uiState.value.showSaveDialog
        )
    }
    
    /**
     * 显示/隐藏背包
     */
    fun toggleInventory() {
        _uiState.value = _uiState.value.copy(
            showInventory = !_uiState.value.showInventory
        )
    }
    
    /**
     * 显示/隐藏状态面板
     */
    fun toggleStatus() {
        _uiState.value = _uiState.value.copy(
            showStatus = !_uiState.value.showStatus
        )
    }
}
