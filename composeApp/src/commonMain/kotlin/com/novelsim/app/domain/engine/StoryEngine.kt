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
    private var enemies: Map<String, Enemy> = emptyMap()
    
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
            variables = story.variables.toMutableMap(),
            // 初始化自定义物品实例列表
            itemInstances = story.customItems.associateBy { it.uid }.toMutableMap()
        )
        
        // 加载并缓存故事事件和怪物
        events = storyRepository.getEvents(storyId).associateBy { it.id }
        enemies = storyRepository.getEnemies(storyId).associateBy { it.id }
        
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
     * 获取敌人信息
     */
    fun getEnemy(enemyId: String): Enemy? = enemies[enemyId]
    
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
                is NodeContent.Battle -> "遭遇敌人: ${enemies[content.enemyId]?.name ?: "未知敌人"}"
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
            expression.startsWith("@") -> {
                evaluateEntityCondition(expression, state)
            }
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
                try {
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
                } catch (e: Exception) {
                    println("Error parsing reputation condition: ${e.message}")
                    false
                }
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

    private fun evaluateEntityCondition(expression: String, state: GameState): Boolean {
        // format: @type:id:key operator value
        // example: @char:hero_1:loyalty > 50
        
        try {
            // 1. Parse operator and value
            val operator = Regex("(>=|<=|==|!=|>|<)").find(expression)?.value ?: return false
            // Split by the operator to separate variable part and value part
            // Note: split limit 2 ensures we only split at the first operator occurrence if multiple exist (unlikely in valid expr)
            val parts = expression.split(operator, limit = 2)
            if (parts.size != 2) return false
            
            val varPart = parts[0].trim().removePrefix("@") // "char:hero_1:loyalty"
            val targetValueStr = parts[1].trim()
            
            // 2. Parse entity identity
            val varParts = varPart.split(":")
            if (varParts.size < 3) return false
            
            val type = varParts[0] // "char"
            val id = varParts[1]   // "hero_1"
            val key = varParts[2]  // "loyalty"
            
            // 3. Get actual value
            // First check dynamic state in GameState
            val dynamicKey = "$type:$id:$key"
            var actualValueStr = state.entityVariables[dynamicKey]
            
            // If not found in dynamic state, check static definition in Story
            if (actualValueStr == null) {
                val story = currentStory ?: return false
                actualValueStr = when(type) {
                    "char" -> story.characters.find { it.id == id }?.variables?.get(key)
                    "loc" -> story.locations.find { it.id == id }?.variables?.get(key)
                    "item" -> story.items.find { it.id == id }?.variables?.get(key)
                    else -> null
                }
            }
            
            // Default to "0" for numeric comparison if null, or "" for string
            val actualValue = actualValueStr ?: "0"
            
            // 4. Compare
            // Try numeric comparison first
            val numActual = actualValue.toDoubleOrNull()
            val numTarget = targetValueStr.toDoubleOrNull()
            
            if (numActual != null && numTarget != null) {
                return when(operator) {
                    ">" -> numActual > numTarget
                    ">=" -> numActual >= numTarget
                    "<" -> numActual < numTarget
                    "<=" -> numActual <= numTarget
                    "==" -> numActual == numTarget
                    "!=" -> numActual != numTarget
                    else -> false
                }
            }
            
            // String comparison
            return when(operator) {
                "==" -> actualValue == targetValueStr
                "!=" -> actualValue != targetValueStr
                else -> false
            }
        } catch (e: Exception) {
            println("Error evaluating entity condition '$expression': ${e.message}")
            return false
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
                
                // 检查是否是自定义物品实例 (effect.itemId 可能是实例ID)
                val instance = state.itemInstances[effect.itemId]
                
                if (instance != null) {
                    // 自定义物品: 不可堆叠，直接添加新槽位
                    // InventorySlot.itemId 存模板ID, instanceId 存实例ID
                    inventory.add(InventorySlot(
                        itemId = instance.templateId,
                        quantity = 1,
                        instanceId = instance.uid
                    ))
                } else {
                    // 普通物品: 尝试堆叠
                    val existingSlot = inventory.find { it.itemId == effect.itemId && it.instanceId == null }
                    if (existingSlot != null) {
                        val index = inventory.indexOf(existingSlot)
                        inventory[index] = existingSlot.copy(quantity = existingSlot.quantity + effect.quantity)
                    } else {
                        inventory.add(InventorySlot(effect.itemId, effect.quantity))
                    }
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

    /**
     * 使用物品
     */
    fun useItem(slot: InventorySlot): Result<Unit> {
        val state = currentGameState ?: return Result.failure(Exception("State not initialized"))
        val story = currentStory ?: return Result.failure(Exception("Story not loaded"))
        
        // 查找模板 (可能是实例也可能是普通物品，itemId都存的是模板ID)
        val template = story.items.find { it.id == slot.itemId }
            ?: return Result.failure(Exception("Item template not found: ${slot.itemId}"))
            
        if (template.type != ItemType.CONSUMABLE) {
            return Result.failure(Exception("Item is not consumable"))
        }
        
        // 应用效果
        template.effect?.let { effect ->
            // 如果是效果类，这里需要适配 ApplyEffect 逻辑
            // ItemEffect 与 Effect 不同，需要转换或单独处理
            applyItemEffect(effect, null) // Consumables typically don't have instance stats
        }
        
        // 移除物品
        return removeItem(slot, 1)
    }
    
    /**
     * 装备物品
     */
    fun equipItem(slot: InventorySlot): Result<Unit> {
        val state = currentGameState ?: return Result.failure(Exception("State not initialized"))
        val story = currentStory ?: return Result.failure(Exception("Story not loaded"))
        
        val template = story.items.find { it.id == slot.itemId }
            ?: return Result.failure(Exception("Item template not found"))
            
        if (template.type != ItemType.EQUIPMENT) {
            return Result.failure(Exception("Item is not equipment"))
        }
        
        val effect = template.effect as? ItemEffect.EquipmentBonus
            ?: return Result.failure(Exception("Item has no equipment effect"))
            
        // 1. 卸下当前位置的装备
        val currentEquippedId = when (effect.slot) {
            EquipSlot.WEAPON -> state.equipment.weapon
            EquipSlot.ARMOR -> state.equipment.armor
            EquipSlot.HEAD -> state.equipment.head
            EquipSlot.ACCESSORY -> state.equipment.accessory
            EquipSlot.BOOTS -> state.equipment.boots
        }
        
        if (currentEquippedId != null) {
            unequipItem(effect.slot)
        }
        
        // 2. 装备新物品 (保存 InstanceId 或 ItemId)
        // 如果 InventorySlot 只有 ItemId (普通装备)，就用 ItemId
        // 如果有 InstanceId (随机装备)，就用 InstanceId
        val equipId = slot.instanceId ?: slot.itemId
        
        val newEquipment = when (effect.slot) {
            EquipSlot.WEAPON -> state.equipment.copy(weapon = equipId)
            EquipSlot.ARMOR -> state.equipment.copy(armor = equipId)
            EquipSlot.HEAD -> state.equipment.copy(head = equipId)
            EquipSlot.ACCESSORY -> state.equipment.copy(accessory = equipId)
            EquipSlot.BOOTS -> state.equipment.copy(boots = equipId)
        }
        
        currentGameState = state.copy(equipment = newEquipment)
        
        // 3. 移除背包中的物品
        removeItem(slot, 1)
        
        // 4. 应用被动属性 (可选，如果属性是预计算在 CharacterStats 里的。这里暂不处理，
        // 假设 BattleSystem 会根据 GameState 动态计算 TotalStats)
        
        return Result.success(Unit)
    }
    
    /**
     * 卸下物品
     */
    fun unequipItem(slot: EquipSlot): Result<Unit> {
        val state = currentGameState ?: return Result.failure(Exception("State not initialized"))
        
        val equipId = when (slot) {
            EquipSlot.WEAPON -> state.equipment.weapon
            EquipSlot.ARMOR -> state.equipment.armor
            EquipSlot.HEAD -> state.equipment.head
            EquipSlot.ACCESSORY -> state.equipment.accessory
            EquipSlot.BOOTS -> state.equipment.boots
        } ?: return Result.success(Unit) // 已经是空的
        
        // 检查这个ID是实例还是模板
        val instance = state.itemInstances[equipId]
        
        // 添加回背包
        val inventory = state.inventory.toMutableList()
        if (instance != null) {
            // 是实例
            inventory.add(InventorySlot(instance.templateId, 1, instance.uid))
        } else {
            // 是普通物品，尝试堆叠
            val existing = inventory.find { it.itemId == equipId && it.instanceId == null }
            if (existing != null) {
                val index = inventory.indexOf(existing)
                inventory[index] = existing.copy(quantity = existing.quantity + 1)
            } else {
                inventory.add(InventorySlot(equipId, 1))
            }
        }
        
        // 清空装备栏
        val newEquipment = when (slot) {
            EquipSlot.WEAPON -> state.equipment.copy(weapon = null)
            EquipSlot.ARMOR -> state.equipment.copy(armor = null)
            EquipSlot.HEAD -> state.equipment.copy(head = null)
            EquipSlot.ACCESSORY -> state.equipment.copy(accessory = null)
            EquipSlot.BOOTS -> state.equipment.copy(boots = null)
        }
        
        currentGameState = state.copy(
            inventory = inventory,
            equipment = newEquipment
        )
        
        return Result.success(Unit)
    }
    
    private fun removeItem(slot: InventorySlot, amount: Int): Result<Unit> {
        val state = currentGameState ?: return Result.failure(Exception("State error"))
        val inventory = state.inventory.toMutableList()
        
        val index = inventory.indexOf(slot) // 需要精确匹配对象引用或equals
        // 由于 InventorySlot 是 data class，只要内容一样就 equals
        // 这里的 slot 应该来自 UI，是完全匹配的
        
        // 如果 UI 传递的 slot 状态可能过时，最好用 id 查找
        val foundIndex = inventory.indexOfFirst { 
            it.itemId == slot.itemId && it.instanceId == slot.instanceId && it.quantity == slot.quantity 
        }
        
        if (foundIndex != -1) {
            val currentSlot = inventory[foundIndex]
            if (currentSlot.quantity > amount) {
                inventory[foundIndex] = currentSlot.copy(quantity = currentSlot.quantity - amount)
            } else {
                inventory.removeAt(foundIndex)
            }
            currentGameState = state.copy(inventory = inventory)
            return Result.success(Unit)
        }
        return Result.failure(Exception("Item not found"))
    }
    
    // 处理 ItemEffect (Heal, etc)
    private fun applyItemEffect(effect: ItemEffect, instance: ItemInstance?) {
         val state = currentGameState ?: return
         // 简单实现 Heal
         if (effect is ItemEffect.Heal) {
             val stats = state.playerStats
             val newStats = stats.copy(
                 currentHp = (stats.currentHp + effect.hp).coerceIn(0, stats.maxHp),
                 currentMp = (stats.currentMp + effect.mp).coerceIn(0, stats.maxMp)
             )
             currentGameState = state.copy(playerStats = newStats)
         }
         // EquipmentBonus 不在这里处理，而是在 BattleSystem 计算属性时处理
    }
}
