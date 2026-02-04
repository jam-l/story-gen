package com.novelsim.app.domain.simulation

import com.novelsim.app.data.model.*
import com.novelsim.app.domain.engine.StoryEngine
import com.novelsim.app.util.PlatformUtils

/**
 * 世界仿真器 - 驱动基于交互的故事发展
 */
class WorldSimulator(
    private val storyEngine: StoryEngine
) {

    /**
     * 获取当前场景下可用的所有交互动作
     */
    fun getAvailableInteractions(): List<SimAction> {
        val gameState = storyEngine.getGameState() ?: return emptyList()
        val story = storyEngine.getCurrentStory() ?: return emptyList()

        val actions = mutableListOf<SimAction>()

        // 1. 获取当前位置的所有实体 (NPC, 道具, 甚至位置本身)
        // 从当前节点 ID 获取位置 ID (需要 StoryEngine 支持反查或 GameState 记录)
        // 假设 GameState.variables["current_location"] 记录了位置
        
        // 暂时从 StoryEngine 获取当前 Location
        // 由于 StoryEngine 还没暴露 getCurrentLocation，我们先尝试通过 Node 查找
        val currentNode = storyEngine.getCurrentNode()
        val locationId = currentNode?.locationId
            ?: gameState.variables["current_location"]
        
        val location = story.locations.find { it.id == locationId }
        
        // 收集当前场景中的所有对象 ID
        val objectIds = mutableListOf<String>()
        if (location != null) {
            objectIds.add(location.id) // 地点本身也是可交互对象
            objectIds.addAll(location.npcs)
            objectIds.addAll(location.items)
            objectIds.addAll(location.enemies)
        }
        
        // 2. 遍历所有对象，检查其定义中的交互规则
        objectIds.forEach { objId ->
            // 从 Story 中查找对象定义
            // 这是一个简化，理想情况下 Story 应该统一管理 Definition
            // 这里我们假设 Character, Item, Location 都能映射到 ObjectDefinition
            // 或者我们扩展 StoryModel 来包含 ObjectDefinition
            
            // 为了快速原型，我们尝试动态构建 Definition 或者从 Story 查找
            val definition = findObjectDefinition(story, objId)
            
            if (definition != null) {
                definition.interactions.forEach { rule ->
                    // Fix: 不要在当位置显示"前往当前位置"的选项
                    if (rule.id == "move_to" && objId == locationId) return@forEach
                    
                    if (evaluateCondition(rule.condition, gameState, definition)) {
                        actions.add(SimAction(rule, objId, definition.name))
                    }
                }
            }
        }
        
        // 3. 排序 (优先级 > ID)
        return actions.sortedByDescending { it.rule.priority }
    }

    /**
     * 根据 ID 获取特定的 SimAction (用于验证)
     */
    fun getSimAction(targetId: String, ruleId: String): SimAction? {
        // 1. 尝试在标准交互列表中查找
        val actions = getAvailableInteractions()
        val found = actions.find { it.targetId == targetId && it.rule.id == ruleId }
        if (found != null) return found
        
        // 2. 特殊处理：移动 (Move)
        // 移动动作可能不在 getAvailableInteractions (因为那是基于当前位置的)
        // 而移动是针对"其他位置"的
        if (ruleId == "move_to") {
            // 查找目标地点
            val story = storyEngine.getCurrentStory() ?: return null
            val targetLoc = story.locations.find { it.id == targetId } ?: return null
            
            // 构建临时的移动动作
            val def = targetLoc.toObjectDefinition()
            val rule = def.interactions.find { it.id == "move_to" } ?: return null
            
            return SimAction(rule, targetId, def.name)
        }
        
        return null
    }

    /**
     * 执行交互并返回结果节点
     */
    fun executeInteraction(action: SimAction): StoryNode {
        val gameState = storyEngine.getGameState()!!
        
        // 1. 应用效果
        val messageBuilder = StringBuilder()
        
        // 默认描述
        var description = action.rule.description.replace("{target.name}", action.targetName)
        
        action.rule.effects.forEach { effect ->
            when (effect) {
                is SimEffect.ModifyState -> {
                    // 更新实体变量 @type:id:key
                    if (effect.targetId == "GLOBAL") {
                         storyEngine.executeVariableAction(NodeContent.VariableAction(
                            variableName = effect.key,
                            operation = VariableOperation.SET,
                            value = effect.value,
                            nextNodeId = ""
                        ))
                    } else {
                        // 需要确定 type
                        val targetId = effect.targetId ?: action.targetId
                        // 简单推断 type (这个需要优化)
                        val type = inferType(targetId)
                        val key = "@$type:$targetId:${effect.key}"
                        storyEngine.executeVariableAction(NodeContent.VariableAction(
                            variableName = key,
                            operation = VariableOperation.SET,
                            value = effect.value,
                            nextNodeId = ""
                        ))
                    }
                }
                is SimEffect.ModifyVariable -> {
                    val op = when(effect.operation) {
                        "ADD" -> VariableOperation.ADD
                        "SUB" -> VariableOperation.SUBTRACT
                        else -> VariableOperation.SET
                    }
                    storyEngine.executeVariableAction(NodeContent.VariableAction(
                        variableName = effect.key,
                        operation = op,
                        value = effect.value,
                        nextNodeId = ""
                    ))
                }
                is SimEffect.GiveItem -> {
                     storyEngine.applyEffect(Effect.GiveItem(effect.itemId, effect.count))
                }
                is SimEffect.RemoveItem -> {
                     storyEngine.applyEffect(Effect.RemoveItem(effect.itemId, effect.count))
                }
                is SimEffect.ShowMessage -> {
                    description = effect.message
                }
                is SimEffect.TriggerEvent -> {
                     storyEngine.applyEffect(Effect.TriggerEvent(effect.eventId))
                }
            }
        }
        
        // 2. 生成结果节点
        // 当前交互作为一个"情节片段"，展示结果，并提供"继续"选项再次进入仿真循环
        val nodeId = "sim_${PlatformUtils.getCurrentTimeMillis()}"
        
        // 我们创建一个 CHOICE 节点，但只有一个 "继续" 选项，点击后会让 Engine 再次调用 Simulator
        // 或者，我们可以直接生成下一个场景的各种选择？
        // 更好的方式：生成一个 Dialog 节点描述结果，连接到一个新的 Choice 节点列出下一轮所有可用动作
        
        // 获取当前位置 ID 用于显示
        val currentLocId = gameState.variables["current_location"] 
            ?: storyEngine.getCurrentNode()?.locationId
            ?: storyEngine.getCurrentStory()?.startNodeId?.let { storyEngine.getCurrentStory()?.nodes?.get(it)?.locationId }

        return StoryNode(
            id = nodeId,
            type = NodeType.DIALOGUE,
            content = NodeContent.Dialogue(
                text = description,
                speaker = "系统",
                nextNodeId = "${nodeId}_next" 
            ),
            locationId = currentLocId
        )
    }
    
    /**
     * 生成基于当前状态的 Choice 节点
     * (当此节点显示时，列出所有可用交互)
     */
    fun generateChoiceNode(): StoryNode {
        val actions = getAvailableInteractions()
        val nodeId = "sim_choice_${PlatformUtils.getCurrentTimeMillis()}"
        
            // 获取 locationId 
        val gameState = storyEngine.getGameState()
        val story = storyEngine.getCurrentStory()
        val currentLocId = gameState?.variables?.get("current_location") 
             ?: story?.nodes?.get(storyEngine.getCurrentNode()?.id ?: "")?.locationId
             ?: story?.nodes?.get(story?.startNodeId ?: "")?.locationId

        if (actions.isEmpty()) {
            return StoryNode(
                id = nodeId,
                type = NodeType.DIALOGUE,
                content = NodeContent.Dialogue(
                    text = "周围一片死寂，似乎没有什么可以做的了。",
                    nextNodeId = "END"
                ),
                locationId = currentLocId
            )
        }
        
        val options = actions.mapIndexed { index, action ->
            val text = if (action.rule.type == InteractionType.OBSERVE && action.targetId == currentLocId) {
                "观察四周"
            } else {
                "${action.rule.name} ${action.targetName}"
            }
            ChoiceOption(
                id = "opt_$index",
                text = text,
                nextNodeId = "SIM_ACTION:${action.targetId}:${action.rule.id}", // 特殊协议 ID
                effects = emptyList()
            )
        }.toMutableList()
        
        // 增加"移动"选项
        // 增加"移动"选项
        val storyForMove = story ?: return StoryNode(id = nodeId, type = NodeType.END, content = NodeContent.Ending("Error", "No Story Loaded"))
        
        // 尝试解析当位置 ID
        // 1. 优先使用变量
        var resolvedLocId = currentLocId
        
        // 2. 如果之前解析失败 (e.g. initial state check logic above was simple), try deeper check
        if (resolvedLocId == null && gameState != null) {
            val currentNode = storyForMove.nodes[gameState.currentNodeId]
            resolvedLocId = currentNode?.locationId
        }
        
        // 3. Fallback to start node
        if (resolvedLocId == null) {
            val startNode = storyForMove.nodes[storyForMove.startNodeId]
            resolvedLocId = startNode?.locationId
        }
        
        val currentLocation = storyForMove.locations.find { it.id == resolvedLocId }
        
        // 找出所有非当前的位置
        // 注意：filter 条件是 IDs 不相等
        val otherLocations = storyForMove.locations.filter { it.id != resolvedLocId }
        
        // 如果有其他位置，提供移动选项
        // 优先显示"Connected" locations，如果没有连接信息，默认全部可选（自由模式）
        val connectedIds = currentLocation?.connectedLocationIds ?: emptyList()
        val targets = if (connectedIds.isNotEmpty()) {
             otherLocations.filter { it.id in connectedIds }
        } else {
             // 如果没有明确连接，随机选2个，防止选项太多
             otherLocations.shuffled().take(2)
        }
        
        targets.forEachIndexed { index, loc ->
             options.add(ChoiceOption(
                 id = "move_$index",
                 text = "前往 ${loc.name}",
                 nextNodeId = "SIM_ACTION:${loc.id}:move_to",
                 effects = emptyList()
             ))
        }
        
        return StoryNode(
            id = nodeId,
            type = NodeType.CHOICE,
            content = NodeContent.Choice(
                prompt = "你打算做什么？",
                options = options
            ),
            locationId = currentLocId
        )
    }

    // --- Helpers ---

    private fun evaluateCondition(expression: String?, state: GameState, target: ObjectDefinition): Boolean {
        if (expression.isNullOrEmpty()) return true
        
        // 简单替换 @target 为具体 ID
        // e.g. "@target:isOpen" -> "@item:door_1:isOpen"
        // 这需要我们知道 target 的完整 type:id。
        // 为了简化，我们在 ObjectDefinition 里存完整 ID，但 evaluateEntityCondition 需要 type。
        
        // 临时 hack: 替换 @target: 为 @type:id:
        // 但这里 evaluate 需要知道 type。
        // 我们假设 expression 已经写好了完整的 @char:xxx 或者我们在这里做一次预处理。
        
        // 更健壮的做法：Simulator 负责解析特定 DSL
        // "isOpen == true" -> 检查 target.defaultState 或 entityVariables
        
        // 1. 尝试直接在 Entity 变量中查找 (Local Scope)
        // 我们可以把 "isOpen" 映射到 "@type:id:isOpen"
        val type = inferType(target.id)
        val fullKey = "@$type:${target.id}:"
        
        // 将 "self:" 或 "this:" 替换为具体前缀
        val expandedExpr = expression.replace("this:", fullKey)
                                     .replace("@target:", fullKey)
        
        return storyEngine.evaluateCondition(expandedExpr)
    }

    private fun findObjectDefinition(story: Story, id: String): ObjectDefinition? {
        // 0. 特殊通用动作 (Move)
        if (id == "move_to") { 
             // 这是一个虚拟动作，我们在 executeInteraction 里处理，或者造一个 dummy definition
             // 但我们的架构是 find definition -> find rule -> execute
             // 所以我们在 Location definition 里加 move? 或者 create a global "System" object?
             // 实际上 processSimulationAction 解析的是 targetId:ruleId
             // 如果 targetId 是 locationId, ruleId 是 "move_to"
             // 那我们需要 Location 有 "move_to" rule 吗？
             // 让我们在 Location definition 里动态加入 move_to 规则
        }
        
        // 1. 检查是否存在 Character
        story.characters.find { it.id == id }?.let { return it.toObjectDefinition() }
        // 2. Item
        story.items.find { it.id == id }?.let { return it.toObjectDefinition() }
        // 3. Location
        story.locations.find { it.id == id }?.let { return it.toObjectDefinition() }
        
        return null
    }
    
    // ... Character.toObjectDefinition ...

    private fun Character.toObjectDefinition(): ObjectDefinition {
        // 从 Character 转换，如果有特定 tags 或 variables
        // 暂时硬编码一些通用人际交互
        val rules = mutableListOf<InteractionRule>()
        
        rules.add(InteractionRule(
            id = "talk",
            name = "交谈",
            description = "你向{target.name}搭话。对方似乎在思考什么。",
            type = InteractionType.TALK,
            priority = 10
        ))
        
        rules.add(InteractionRule(
            id = "observe_char",
            name = "观察",
            description = "{target.name}看起来$description",
            type = InteractionType.OBSERVE,
            priority = 5
        ))
        
        return ObjectDefinition(id, name, description, variables, rules, tags)
    }
    
    private fun Item.toObjectDefinition(): ObjectDefinition {
        val rules = mutableListOf<InteractionRule>()
        
        // 1. 拾取 (如果不在背包里且没有所属人)
        // 这是一个简化判断，理想情况下应该检查 inventory
        rules.add(InteractionRule(
            id = "take",
            name = "捡起",
            description = "你捡起了{target.name}。",
            // 条件：不仅要是未拥有，还要在当前 Location (隐式条件)
            // 这里 check "isOwned" 变量，通常 item 在地上时 isOwned=false
            condition = "this:isOwned != true", 
            effects = listOf(
                SimEffect.GiveItem(id),
                SimEffect.ModifyState("isOwned", "true", id), // 标记为已拥有
                // SimEffect.RemoveItem(id) // 如果在 Item 列表里，RemoveItem 会把它干掉
                // 对于场景互动物品，我们可能只是 SetState hidden
                SimEffect.ShowMessage("你把{target.name}放入了背包。")
            ),
            type = InteractionType.TAKE,
            priority = 10
        ))

        // 2. 消耗品 - 吃/喝
        if (type == ItemType.CONSUMABLE) {
             rules.add(InteractionRule(
                id = "consume",
                name = "使用",
                description = "你使用了{target.name}。",
                condition = "this:isOwned == true", // 必须在背包里
                effects = listOf(
                    SimEffect.RemoveItem(id),
                    // Item effect application is handled by StoryEngine via Give/Remove but 
                    // applying stat changes explicitly here is better if we want custom description
                    SimEffect.ShowMessage("你使用了{target.name}。感觉不错。"),
                    // TODO: Apply actual item stats effect
                ),
                type = InteractionType.INTERACT,
                priority = 20
            ))
        }
        
        // 3. 装备 - 装备/卸下
        if (type == ItemType.EQUIPMENT) {
             rules.add(InteractionRule(
                id = "equip",
                name = "装备",
                description = "你装备了{target.name}。",
                condition = "this:isOwned == true && this:isEquipped != true",
                effects = listOf(
                    SimEffect.ModifyState("isEquipped", "true", id),
                    SimEffect.ShowMessage("你装备上了{target.name}。")
                    // Note: Actual stats update needs plumbing
                ),
                type = InteractionType.INTERACT,
                priority = 20
            ))
        }

        return ObjectDefinition(id, name, description, variables, rules, emptyList())
    }
    
    private fun Location.toObjectDefinition(): ObjectDefinition {
        val rules = mutableListOf<InteractionRule>()
        rules.add(InteractionRule(
             id = "observe",
             name = "观察四周",
             description = "你仔细观察了{target.name}。$description",
             priority = -10,
             type = InteractionType.OBSERVE
        ))
         // 动态添加 "move_to" 规则
        rules.add(InteractionRule(
             id = "move_to",
             name = "前往",
             description = "你花费了一些时间，抵达了{target.name}。", 
             priority = 0,
             type = InteractionType.MOVEMENT,
             effects = listOf(
                  SimEffect.ShowMessage("你花费了一些时间，抵达了{target.name}。"),
                  SimEffect.ModifyState("current_location", id, "GLOBAL") // 使用特殊标记或扩展 SimEffect
             )
        ))
        return ObjectDefinition(id, name, description, variables, rules, emptyList())
    }

    private fun inferType(id: String): String {
        return when {
            id.startsWith("char") -> "char"
            id.startsWith("item") -> "item"
            id.startsWith("loc") -> "loc"
            id.startsWith("enemy") -> "enemy"
            else -> "obj"
        }
    }
}
