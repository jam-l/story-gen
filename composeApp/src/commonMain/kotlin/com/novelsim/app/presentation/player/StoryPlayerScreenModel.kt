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
        val showHistory: Boolean = false,
        val showVariableViewer: Boolean = false,
        val isTyping: Boolean = false,
        val displayedText: String = ""
    )
    
    /**
     * 显示模式
     */
    enum class DisplayMode {
        STORY,      // 故事模式
        BATTLE,     // 战斗模式
        ENDING,     // 结局模式
        MAP         // 地图模式
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
    /**
     * 处理节点显示
     */
    private suspend fun processNode(node: StoryNode) {
        var currentNode = node
        var iterations = 0
        val maxIterations = 100 // 安全阈值，防止无限逻辑循环
        
        while (iterations < maxIterations) {
            iterations++
            
            // 只有在节点变化时才更新状态
            if (iterations == 1 || currentNode != _uiState.value.currentNode) {
                _uiState.value = _uiState.value.copy(
                    currentNode = currentNode,
                    gameState = storyEngine.getGameState()
                )
            }

            // 让出 CPU，确保 UI 响应
            kotlinx.coroutines.yield()

            when (currentNode.type) {
                NodeType.DIALOGUE -> {
                    val content = currentNode.content as? NodeContent.Dialogue
                    startTypingEffect(content?.text ?: "")
                    return
                }
                NodeType.CHOICE -> {
                    _uiState.value = _uiState.value.copy(
                        isTyping = false,
                        displayedText = (currentNode.content as? NodeContent.Choice)?.prompt ?: ""
                    )
                    return
                }
                NodeType.BATTLE -> {
                    startBattle(currentNode)
                    return
                }
                NodeType.END -> {
                    _uiState.value = _uiState.value.copy(
                        displayMode = DisplayMode.ENDING,
                        isTyping = false
                    )
                    return
                }
                NodeType.VARIABLE -> {
                    val content = currentNode.content as? NodeContent.VariableAction
                    if (content != null) {
                        storyEngine.executeVariableAction(content)
                        val nextId = if (content.nextNodeId.isNotEmpty()) content.nextNodeId else currentNode.connections.firstOrNull()?.targetNodeId
                        if (nextId != null) {
                            val nextNodeResult = storyEngine.navigateToNode(nextId)
                            if (nextNodeResult.isSuccess) {
                                currentNode = nextNodeResult.getOrThrow()
                                continue
                            }
                        }
                    }
                    return
                }
                NodeType.CONDITION -> {
                    val content = currentNode.content as? NodeContent.Condition
                    if (content != null) {
                        val result = storyEngine.evaluateCondition(content.expression)
                        val nextId = if (result) content.trueNextNodeId else content.falseNextNodeId
                        val nextNodeResult = storyEngine.navigateToNode(nextId)
                        if (nextNodeResult.isSuccess) {
                            currentNode = nextNodeResult.getOrThrow()
                            continue
                        }
                    }
                    return
                }
                NodeType.RANDOM -> {
                    val content = currentNode.content as? NodeContent.Random
                    if (content != null && content.branches.isNotEmpty()) {
                        val totalWeight = content.branches.sumOf { it.weight }
                        var randomVal = (0 until totalWeight).random()
                        var selectedNodeId = content.branches.last().nextNodeId
                        for (branch in content.branches) {
                            if (randomVal < branch.weight) {
                                selectedNodeId = branch.nextNodeId
                                break
                            }
                            randomVal -= branch.weight
                        }
                        val nextNodeResult = storyEngine.navigateToNode(selectedNodeId)
                        if (nextNodeResult.isSuccess) {
                            currentNode = nextNodeResult.getOrThrow()
                            continue
                        }
                    }
                    return
                }
                NodeType.ITEM -> {
                    val content = currentNode.content as? NodeContent.ItemAction
                    if (content != null) {
                        val nextId = if (content.nextNodeId.isNotEmpty()) content.nextNodeId else currentNode.connections.firstOrNull()?.targetNodeId
                        if (nextId != null) {
                            val nextNodeResult = storyEngine.navigateToNode(nextId)
                            if (nextNodeResult.isSuccess) {
                                currentNode = nextNodeResult.getOrThrow()
                                continue
                            }
                        }
                    }
                    return
                }
                else -> {
                    val nextId = currentNode.connections.firstOrNull()?.targetNodeId
                    if (nextId != null) {
                        val nextNodeResult = storyEngine.navigateToNode(nextId)
                        if (nextNodeResult.isSuccess) {
                            currentNode = nextNodeResult.getOrThrow()
                            continue
                        }
                    }
                    return
                }
            }
        }
        
        // 如果达到最大迭代次数仍未找到 UI 节点，则停止并报错，防止 ANR
        if (iterations >= maxIterations) {
            _uiState.value = _uiState.value.copy(error = "检测到逻辑死循环，请检查故事结构")
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
        screenModelScope.launch {
            storyEngine.continueDialogue().fold(
                onSuccess = { node ->
                    processNode(node)
                },
                onFailure = { /* 没有下一个节点，可能需要用户选择 */ }
            )
        }
    }
    
    /**
     * 选择选项
     */
    fun selectChoice(option: ChoiceOption) {
        screenModelScope.launch {
            storyEngine.processChoice(option).fold(
                onSuccess = { node ->
                    processNode(node)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }
    
    /**
     * 开始战斗
     */
    private fun startBattle(node: StoryNode) {
        val content = node.content as? NodeContent.Battle ?: return
        val gameState = _uiState.value.gameState ?: return
        
        // 从引擎获取敌人数据
        val baseEnemy = storyEngine.getEnemy(content.enemyId) ?: return 
        
        // 计算动态难度系数
        // 1. 玩家等级系数：确保怪物强度随玩家等级提升
        // 2. 地图深度系数：根据节点 Y 坐标判断"深度"，模拟地图分布难度
        val playerLevel = gameState.playerStats.level
        val depthFactor = (node.position.y / 1000f).coerceAtLeast(0f) // 假设每 1000px 增加一次难度阶梯
        
        // 综合难度倍率 = (1 + (玩家等级 - 1) * 0.1) + 深度系数 * 0.2
        // 这个公式让怪物稍微比玩家强一点点，同时越深处越强
        val levelMultiplier = 1f + (playerLevel - 1) * 0.15f
        val depthMultiplier = 1f + depthFactor * 0.2f
        val totalMultiplier = levelMultiplier * depthMultiplier

        // 动态生成的敌人
        val scaledEnemy = baseEnemy.copy(
            name = if (totalMultiplier > 1.5) "强化的 ${baseEnemy.name}" else baseEnemy.name,
            stats = baseEnemy.stats.copy(
                maxHp = (baseEnemy.stats.maxHp * totalMultiplier).toInt(),
                currentHp = (baseEnemy.stats.maxHp * totalMultiplier).toInt(), // 确保满血
                attack = (baseEnemy.stats.attack * totalMultiplier).toInt(),
                defense = (baseEnemy.stats.defense * totalMultiplier).toInt(),
                speed = (baseEnemy.stats.speed * (1 + (playerLevel * 0.05f))).toInt(), // 速度成长慢一点
                exp = (baseEnemy.stats.exp * totalMultiplier).toInt()
            ),
            expReward = (baseEnemy.expReward * totalMultiplier).toInt(),
            goldReward = (baseEnemy.goldReward * totalMultiplier).toInt()
        )
        
        val battleState = battleSystem.startBattle(gameState.playerStats, scaledEnemy)
        
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
        val skillsMap = _uiState.value.story?.skills?.associateBy { it.id } ?: emptyMap()
        var newState = battleSystem.executePlayerTurn(currentBattleState, action, skillsMap)
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
                val reward = battleSystem.calculateReward(battleState)
                if (reward != null) {
                    val currentStats = battleState.playerStats.copy() // 使用副本
                    val leveledUp = currentStats.addExp(reward.exp)
                    
                    // 应用金币和道具奖励
                    val currentGameState = _uiState.value.gameState
                    if (currentGameState != null) {
                        val updatedGameState = currentGameState.copy(
                            gold = currentGameState.gold + reward.gold,
                            playerStats = currentStats
                        )
                        
                        // 持久化到引擎
                        storyEngine.updateGameState(updatedGameState)
                        
                        // 应用掉落物品
                        reward.drops.forEach { (itemId, quantity) ->
                            storyEngine.applyEffect(Effect.GiveItem(itemId, quantity))
                        }
                        
                        // 同步 UI 状态
                        _uiState.value = _uiState.value.copy(
                            gameState = storyEngine.getGameState()
                        )
                        
                        if (leveledUp) {
                            println("恭喜！等级提升到 ${currentStats.level}")
                            // TODO: 可以在此处触发 UI 提示，例如弹窗或飘字
                        }
                    }
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
     * 切换地图显示
     */
    fun toggleMap() {
        val currentMode = _uiState.value.displayMode
        _uiState.value = _uiState.value.copy(
            displayMode = if (currentMode == DisplayMode.MAP) DisplayMode.STORY else DisplayMode.MAP
        )
    }

    /**
     * 在地图上选择节点
     */
    fun selectNodeFromMap(nodeId: String) {
        val story = storyEngine.getCurrentStory() ?: return
        val node = story.nodes[nodeId] ?: return
        
        // 只有当前可到达的节点或已访问节点才能点击（简单逻辑：暂允许预览所有节点）
        _uiState.value = _uiState.value.copy(
            currentNode = node,
            displayMode = DisplayMode.STORY
        )
        screenModelScope.launch {
            processNode(node)
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

    /**
     * 显示/隐藏历史记录
     */
    fun toggleHistory() {
        _uiState.value = _uiState.value.copy(
            showHistory = !_uiState.value.showHistory
        )
    }

    /**
     * 切换变量查看器显示
     */
    fun toggleVariableViewer() {
        _uiState.value = _uiState.value.copy(
            showVariableViewer = !_uiState.value.showVariableViewer
        )
    }

    fun useItem(slot: InventorySlot) {
        storyEngine.useItem(slot).onSuccess {
            _uiState.value = _uiState.value.copy(
                gameState = storyEngine.getGameState()
            )
        }
    }

    fun equipItem(slot: InventorySlot) {
        storyEngine.equipItem(slot).onSuccess {
            _uiState.value = _uiState.value.copy(
                gameState = storyEngine.getGameState()
            )
        }
    }

    fun unequipItem(slot: EquipSlot) {
        storyEngine.unequipItem(slot).onSuccess {
            _uiState.value = _uiState.value.copy(
                gameState = storyEngine.getGameState()
            )
        }
    }
}
