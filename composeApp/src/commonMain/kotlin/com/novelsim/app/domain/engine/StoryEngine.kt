package com.novelsim.app.domain.engine

import com.novelsim.app.data.model.*
import com.novelsim.app.data.repository.StoryRepository
import com.novelsim.app.util.PlatformUtils

/**
 * 故事引擎 - 核心叙事逻辑处理
 * 
 * 负责：
 * - 加载和解析故事
 * - 管理当前故事状态
 * - 处理节点跳转逻辑
 * - 执行条件判断和效果
 */
class StoryEngine(
    private val storyRepository: StoryRepository
) {
    private var currentStory: Story? = null
    private var currentGameState: GameState? = null
    private var events: Map<String, GameEvent> = emptyMap()
    
    /**
     * 加载故事并初始化游戏状态
     */
    suspend fun loadStory(storyId: String): Result<StoryNode> {
        val story = storyRepository.getStoryById(storyId)
            ?: return Result.failure(Exception("故事不存在: $storyId"))
        
        currentStory = story
        currentGameState = GameState(
            storyId = storyId,
            currentNodeId = story.startNodeId,
            variables = story.variables.toMutableMap()
        )
        
        // 加载并缓存故事事件
        events = storyRepository.getEvents(storyId).associateBy { it.id }
        
        val startNode = story.nodes[story.startNodeId]
            ?: return Result.failure(Exception("起始节点不存在: ${story.startNodeId}"))
        
        return Result.success(startNode)
    }
    
    /**
     * 从存档加载游戏
     */
    fun loadFromSave(story: Story, saveData: SaveData): Result<StoryNode> {
        currentStory = story
        currentGameState = saveData.gameState
        
        val node = story.nodes[saveData.gameState.currentNodeId]
            ?: return Result.failure(Exception("节点不存在: ${saveData.gameState.currentNodeId}"))
        
        return Result.success(node)
    }
    
    /**
     * 获取当前节点
     */
    fun getCurrentNode(): StoryNode? {
        val story = currentStory ?: return null
        val state = currentGameState ?: return null
        return story.nodes[state.currentNodeId]
    }
    
    /**
     * 获取当前游戏状态
     */
    fun getGameState(): GameState? = currentGameState
    
    /**
     * 获取当前故事
     */
    fun getCurrentStory(): Story? = currentStory
    
    /**
     * 处理选择，执行效果并跳转到下一个节点
     */
    fun processChoice(option: ChoiceOption): Result<StoryNode> {
        val story = currentStory
            ?: return Result.failure(Exception("未加载故事"))
        val state = currentGameState
            ?: return Result.failure(Exception("游戏状态未初始化"))
        
        // 检查选项条件
        if (option.condition != null && !evaluateCondition(option.condition)) {
            return Result.failure(Exception("不满足选项条件"))
        }
        
        // 执行选项效果
        option.effects.forEach { effect ->
            applyEffect(effect)
        }
        
        // 记录选项历史
        val historyItem = HistoryItem(
            nodeId = state.currentNodeId,
            text = option.text,
            type = HistoryType.CHOICE,
            timestamp = PlatformUtils.getCurrentTimeMillis()
        )
        currentGameState = state.copy(history = state.history + historyItem)
        
        // 跳转到下一个节点
        return navigateToNode(option.nextNodeId)
    }
    
    /**
     * 根据对话节点的连接跳转到下一个节点
     */
    fun continueDialogue(): Result<StoryNode> {
        val currentNode = getCurrentNode()
            ?: return Result.failure(Exception("当前节点不存在"))
        
        val nextNodeId = currentNode.connections.firstOrNull()?.targetNodeId
            ?: return Result.failure(Exception("没有下一个节点"))
        
        return navigateToNode(nextNodeId)
    }
    
    /**
     * 跳转到指定节点
     */
    fun navigateToNode(nodeId: String): Result<StoryNode> {
        val story = currentStory
            ?: return Result.failure(Exception("未加载故事"))
        
        val nextNode = story.nodes[nodeId]
            ?: return Result.failure(Exception("节点不存在: $nodeId"))
        
        val now = PlatformUtils.getCurrentTimeMillis()
        
        // 记录节点历史 (仅记录对话节点和重要节点)
        var newHistory = currentGameState?.history ?: emptyList()
        if (nextNode.type == NodeType.DIALOGUE || nextNode.type == NodeType.END || nextNode.type == NodeType.BATTLE) {
            val contentText = when(val content = nextNode.content) {
                is NodeContent.Dialogue -> content.text
                is NodeContent.Ending -> content.description
                is NodeContent.Battle -> "遭遇敌人: ${content.enemy.name}"
                else -> ""
            }
            
            if (contentText.isNotEmpty()) {
                val historyItem = HistoryItem(
                    nodeId = nodeId,
                    text = contentText,
                    type = HistoryType.NODE,
                    timestamp = now
                )
                newHistory = newHistory + historyItem
            }
        }

        currentGameState = currentGameState?.copy(
            currentNodeId = nodeId,
            history = newHistory
        )
        
        return Result.success(nextNode)
    }
    
    /**
     * 评估条件表达式
     * 
     * 支持的格式：
     * - "variable_name > 5"
     * - "has_item:item_id"
     * - "flag:flag_name"
     */
    fun evaluateCondition(expression: String): Boolean {
        val state = currentGameState ?: return false
        
        return when {
            expression.startsWith("has_item:") -> {
                val itemId = expression.removePrefix("has_item:")
                state.inventory.any { it.itemId == itemId && it.quantity > 0 }
            }
            expression.startsWith("has_clue:") -> {
                val clueId = expression.removePrefix("has_clue:")
                state.collectedClues.contains(clueId)
            }
            expression.startsWith("flag:") -> {
                val flagName = expression.removePrefix("flag:")
                flagName in state.flags
            }
            expression.startsWith("reputation:") -> {
                // expecting "reputation:factionId > 50"
                // split by space first to get "reputation:factionId", ">", "50"
                // or just regex/parsing
                val parts = expression.trim().split(Regex("\\s+"))
                if (parts.size >= 3) {
                    val reputationKey = parts[0] // reputation:factionId
                    val operator = parts[1]
                    val value = parts[2].toIntOrNull() ?: 0
                    
                    val factionId = reputationKey.removePrefix("reputation:")
                    val currentRep = state.factionReputations[factionId] ?: 0
                    
                    when (operator) {
                        ">" -> currentRep > value
                        ">=" -> currentRep >= value
                        "<" -> currentRep < value
                        "<=" -> currentRep <= value
                        "==" -> currentRep == value
                        "!=" -> currentRep != value
                        else -> false
                    }
                } else false
            }
            expression.contains(">") -> {
                val parts = expression.split(">").map { it.trim() }
                if (parts.size == 2) {
                    val varValue = state.variables[parts[0]]?.toIntOrNull() ?: 0
                    val compareValue = parts[1].toIntOrNull() ?: 0
                    varValue > compareValue
                } else false
            }
            expression.contains("<") -> {
                val parts = expression.split("<").map { it.trim() }
                if (parts.size == 2) {
                    val varValue = state.variables[parts[0]]?.toIntOrNull() ?: 0
                    val compareValue = parts[1].toIntOrNull() ?: 0
                    varValue < compareValue
                } else false
            }
            expression.contains("==") -> {
                val parts = expression.split("==").map { it.trim() }
                if (parts.size == 2) {
                    val varValue = state.variables[parts[0]] ?: ""
                    varValue == parts[1]
                } else false
            }
            else -> false
        }
    }
    
    /**
     * 应用效果
     */
    fun applyEffect(effect: Effect) {
        val state = currentGameState ?: return
        
        when (effect) {
            is Effect.ModifyVariable -> {
                val currentValue = state.variables[effect.variableName]?.toIntOrNull() ?: 0
                val operandValue = effect.value.toIntOrNull() ?: 0
                val newValue = when (effect.operation) {
                    VariableOperation.SET -> operandValue
                    VariableOperation.ADD -> currentValue + operandValue
                    VariableOperation.SUBTRACT -> currentValue - operandValue
                    VariableOperation.MULTIPLY -> currentValue * operandValue
                    VariableOperation.DIVIDE -> if (operandValue != 0) currentValue / operandValue else currentValue
                }
                state.variables[effect.variableName] = newValue.toString()
            }
            is Effect.GiveItem -> {
                val inventory = state.inventory.toMutableList()
                val existingSlot = inventory.find { it.itemId == effect.itemId }
                if (existingSlot != null) {
                    val index = inventory.indexOf(existingSlot)
                    inventory[index] = existingSlot.copy(quantity = existingSlot.quantity + effect.quantity)
                } else {
                    inventory.add(InventorySlot(effect.itemId, effect.quantity))
                }
                currentGameState = state.copy(inventory = inventory)
            }
            is Effect.RemoveItem -> {
                val inventory = state.inventory.toMutableList()
                val existingSlot = inventory.find { it.itemId == effect.itemId }
                if (existingSlot != null) {
                    val newQuantity = existingSlot.quantity - effect.quantity
                    if (newQuantity <= 0) {
                        inventory.remove(existingSlot)
                    } else {
                        val index = inventory.indexOf(existingSlot)
                        inventory[index] = existingSlot.copy(quantity = newQuantity)
                    }
                    currentGameState = state.copy(inventory = inventory)
                }
            }
            is Effect.ModifyAttribute -> {
                val stats = state.playerStats
                val newStats = when (effect.attribute.lowercase()) {
                    "hp" -> stats.copy(currentHp = (stats.currentHp + effect.value).coerceIn(0, stats.maxHp))
                    "mp" -> stats.copy(currentMp = (stats.currentMp + effect.value).coerceIn(0, stats.maxMp))
                    "attack" -> stats.copy(attack = (stats.attack + effect.value).coerceAtLeast(0))
                    "defense" -> stats.copy(defense = (stats.defense + effect.value).coerceAtLeast(0))
                    "speed" -> stats.copy(speed = (stats.speed + effect.value).coerceAtLeast(0))
                    "exp" -> stats.copy(exp = (stats.exp + effect.value).coerceAtLeast(0))
                    else -> stats
                }
                currentGameState = state.copy(playerStats = newStats)
            }
            is Effect.PlaySound -> {
                // TODO: 实现音效播放 (需接入平台音频API)
            }
            is Effect.AddClue -> {
               state.collectedClues.add(effect.clueId)
            }
            is Effect.ModifyReputation -> {
                val currentRep = state.factionReputations[effect.factionId] ?: 0
                state.factionReputations[effect.factionId] = currentRep + effect.amount
            }
            is Effect.ModifyRelationship -> {
                val currentRel = state.characterRelationships[effect.characterId] ?: 0
                state.characterRelationships[effect.characterId] = currentRel + effect.amount
            }
            is Effect.MoveToLocation -> {
                state.variables["current_location"] = effect.locationId
            }
            is Effect.TriggerEvent -> {
                val event = events[effect.eventId] ?: return
                
                // 检查是否可重复触发
                if (!event.isRepeatable && state.triggeredEvents.contains(effect.eventId)) {
                    return
                }
                
                // 检查触发条件
                if (event.triggerCondition != null && !evaluateCondition(event.triggerCondition)) {
                    return
                }
                
                // 触发事件
                state.triggeredEvents.add(effect.eventId)
                
                // 如果事件有关联的起始节点，且当前处于自动流转状态（非Choice跳转），
                // 这里可以考虑直接跳转。但在 processChoice 中，通常由 options 控制跳转。
                // 这里的 TriggerEvent 更多作为"标记"或"解锁"使用。
                // 如果需要强制跳转，建议在 Option 中直接指定 nextNodeId 为事件起始节点。
            }
        }
    }
    
    /**
     * 设置标记
     */
    fun setFlag(flagName: String) {
        currentGameState?.flags?.add(flagName)
    }
    
    /**
     * 移除标记
     */
    fun removeFlag(flagName: String) {
        currentGameState?.flags?.remove(flagName)
    }
    
    /**
     * 更新游玩时间
     */
    fun updatePlayTime(deltaMs: Long) {
        currentGameState = currentGameState?.copy(
            playTime = (currentGameState?.playTime ?: 0L) + deltaMs
        )
    }
}
