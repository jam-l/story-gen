package com.novelsim.app.domain.generator

import com.novelsim.app.data.model.*
import com.novelsim.app.util.PlatformUtils
import kotlin.random.Random

/**
 * 随机故事生成器
 * 根据配置参数随机生成故事节点和分支
 */
class RandomStoryGenerator(
    private val config: GeneratorConfig = GeneratorConfig(),
    private val nameProvider: com.novelsim.app.data.source.RandomNameProvider? = null
) {
    
    /**
     * 生成器配置参数
     */
    data class GeneratorConfig(
        /** 节点数量范围 */
        val minNodes: Int = 5,
        val maxNodes: Int = 15,
        
        /** 分支数量（选择节点数） */
        val minChoices: Int = 1,
        val maxChoices: Int = 3,
        
        /** 每个选择的选项数量 */
        val minOptions: Int = 2,
        val maxOptions: Int = 4,
        
        /** 包含战斗节点的概率 (0.0-1.0) */
        val battleProbability: Float = 0.2f,
        
        /** 包含条件节点的概率 */
        val conditionProbability: Float = 0.15f,
        
        /** 包含道具节点的概率 */
        val itemProbability: Float = 0.1f,
        
        /** 包含变量节点的概率 */
        val variableProbability: Float = 0.1f,
        
        /** 包含随机分支节点的概率 */
        val randomNodeProbability: Float = 0.1f,
        
        /** 结局数量 */
        val minEndings: Int = 1,
        val maxEndings: Int = 3,
        
        /** 故事主题 */
        val theme: StoryTheme = StoryTheme.FANTASY,
        
        /** 混乱度 (0.0-1.0): 控制结构的非线性程度 */
        val chaos: Float = 0.0f,
        
        /** 难度 (0.0-1.0): 影响数值和挑战性 */
        val difficulty: Float = 0.5f,

        /** 是否生成结局 (false = 无限模式/循环) */
        val generateEnding: Boolean = true,
        
        /** 随机种子（null 表示随机） */
        val seed: Long? = null
    )
    
    /**
     * 故事主题
     */
    enum class StoryTheme {
        FANTASY,    // 奇幻
        SCI_FI,     // 科幻
        MYSTERY,    // 悬疑
        ROMANCE,    // 言情
        HORROR      // 恐怖
    }
    
    private val random = config.seed?.let { Random(it) } ?: Random.Default
    
    // 各主题的对话模板
    private val dialogueTemplates = mapOf(
        StoryTheme.FANTASY to listOf(
            Triple("旁白", "你站在古老的城堡前，巨大的石门上刻满了神秘的符文。", null),
            Triple("神秘老者", "年轻人，你是来寻找传说中的龙之珠吗？", null),
            Triple("旁白", "远处的山峰上，一条巨龙正在盘旋。", null),
            Triple("精灵公主", "这片森林是我们的家园，外来者请离开。", null),
            Triple("旁白", "你在废墟中发现了一本发光的魔法书。", null),
            Triple("黑暗法师", "愚蠢的冒险者，你不该来到这里！", null),
            Triple("旁白", "洞穴深处传来奇怪的声响。", null),
            Triple("铁匠", "这把剑是我毕生的杰作，请好好使用它。", null)
        ),
        StoryTheme.SCI_FI to listOf(
            Triple("旁白", "飞船在虚空中飘荡，警报声响彻走廊。", null),
            Triple("AI助手", "检测到未知生命体信号，建议保持警惕。", null),
            Triple("旁白", "星际之门开始缓缓启动，蓝色光芒照亮了控制室。", null),
            Triple("舰长", "所有人员进入战斗状态！", null),
            Triple("旁白", "你看向窗外，一颗巨大的行星正在逼近。", null),
            Triple("外星人", "地球人，我们终于见面了。", null),
            Triple("旁白", "实验室里的机器发出嗡嗡声。", null),
            Triple("科学家", "这项发现将改变人类的未来。", null)
        ),
        StoryTheme.MYSTERY to listOf(
            Triple("旁白", "午夜的钟声响起，老宅的灯光忽明忽暗。", null),
            Triple("侦探", "所有的线索都指向这间密室。", null),
            Triple("旁白", "你在抽屉里发现了一封神秘的信件。", null),
            Triple("管家", "主人今晚不见任何客人。", null),
            Triple("旁白", "墙上的画像似乎在注视着你。", null),
            Triple("神秘人", "有些真相，还是不要知道的好。", null),
            Triple("旁白", "地下室传来脚步声。", null),
            Triple("警察", "请您配合调查。", null)
        ),
        StoryTheme.ROMANCE to listOf(
            Triple("旁白", "樱花飘落的季节，你在咖啡馆遇见了她。", null),
            Triple("她", "这个座位有人吗？", null),
            Triple("旁白", "雨中的相遇，总是那么浪漫。", null),
            Triple("他", "我一直想对你说...", null),
            Triple("旁白", "夕阳下的海边，海风轻柔地吹过。", null),
            Triple("她", "谢谢你一直陪在我身边。", null),
            Triple("旁白", "约定的地点，只有你一个人。", null),
            Triple("他", "对不起，让你久等了。", null)
        ),
        StoryTheme.HORROR to listOf(
            Triple("旁白", "走廊尽头的房间传来诡异的声音。", null),
            Triple("???", "救...救命...", null),
            Triple("旁白", "你感觉背后有人在盯着你。", null),
            Triple("旁白", "镜子里映出了一个不属于你的身影。", null),
            Triple("孩子", "叔叔，你想和我们一起玩吗？", null),
            Triple("旁白", "手机突然收到一条未知号码的短信。", null),
            Triple("旁白", "血迹一直延伸到地下室。", null),
            Triple("诡异声音", "不要回头...", null)
        )
    )
    
    // 选择提示模板
    private val choicePrompts = mapOf(
        StoryTheme.FANTASY to listOf(
            "你决定怎么做？",
            "面对眼前的情况，你选择：",
            "命运的岔路口，你将走向何方？"
        ),
        StoryTheme.SCI_FI to listOf(
            "下一步行动是什么？",
            "系统请求指令：",
            "你需要做出决定："
        ),
        StoryTheme.MYSTERY to listOf(
            "线索指向哪里？",
            "你选择调查：",
            "下一步该怎么做？"
        ),
        StoryTheme.ROMANCE to listOf(
            "你的心意是？",
            "该如何回应？",
            "你决定："
        ),
        StoryTheme.HORROR to listOf(
            "逃跑还是探索？",
            "你该怎么办？",
            "恐惧中，你选择："
        )
    )
    
    // 选项模板
    private val optionTemplates = mapOf(
        StoryTheme.FANTASY to listOf(
            "勇敢前进",
            "小心探索",
            "寻求帮助",
            "使用魔法",
            "战斗到底",
            "暂时撤退",
            "与之交谈",
            "悄悄绕过"
        ),
        StoryTheme.SCI_FI to listOf(
            "发送信号",
            "启动防御",
            "进行扫描",
            "开启通讯",
            "准备跃迁",
            "释放探测器",
            "请求支援",
            "独自行动"
        ),
        StoryTheme.MYSTERY to listOf(
            "继续调查",
            "询问嫌疑人",
            "收集证据",
            "联系警方",
            "独自前往",
            "等待时机",
            "伪装潜入",
            "直接对质"
        ),
        StoryTheme.ROMANCE to listOf(
            "主动表白",
            "默默守护",
            "约对方出去",
            "送一份礼物",
            "写一封信",
            "假装偶遇",
            "寻求朋友帮助",
            "等待对方"
        ),
        StoryTheme.HORROR to listOf(
            "逃离这里",
            "躲起来",
            "继续探索",
            "呼救",
            "寻找武器",
            "打开灯",
            "不要出声",
            "面对恐惧"
        )
    )
    
    // 结局模板
    private val endingTemplates = mapOf(
        StoryTheme.FANTASY to listOf(
            Triple("英雄凯旋", "你成功完成了使命，成为了传说中的英雄。", EndingType.GOOD),
            Triple("黑暗降临", "邪恶势力获胜，世界陷入黑暗。", EndingType.BAD),
            Triple("隐秘真相", "你发现了世界的终极秘密。", EndingType.SECRET),
            Triple("平凡生活", "你选择了平静的生活，远离冒险。", EndingType.NORMAL)
        ),
        StoryTheme.SCI_FI to listOf(
            Triple("新纪元", "人类迈入了星际时代。", EndingType.GOOD),
            Triple("末日降临", "文明毁于一旦。", EndingType.BAD),
            Triple("平行宇宙", "你发现这不是唯一的现实。", EndingType.SECRET),
            Triple("归途", "你终于回到了地球。", EndingType.NORMAL)
        ),
        StoryTheme.MYSTERY to listOf(
            Triple("真相大白", "谜团终于被解开。", EndingType.GOOD),
            Triple("永恒之谜", "真相永远被埋葬。", EndingType.BAD),
            Triple("意外真相", "凶手竟然是...", EndingType.SECRET),
            Triple("不了了之", "案件暂时搁置。", EndingType.NORMAL)
        ),
        StoryTheme.ROMANCE to listOf(
            Triple("幸福结局", "你们幸福地生活在一起。", EndingType.GOOD),
            Triple("错过", "缘分就这样消逝了。", EndingType.BAD),
            Triple("意外重逢", "多年后，你们再次相遇。", EndingType.SECRET),
            Triple("好朋友", "虽然没有在一起，但你们成了好朋友。", EndingType.NORMAL)
        ),
        StoryTheme.HORROR to listOf(
            Triple("逃出生天", "你成功逃离了这个噩梦。", EndingType.GOOD),
            Triple("永恒囚禁", "你再也无法离开这里。", EndingType.BAD),
            Triple("真正恐惧", "原来恐怖的源头是...", EndingType.SECRET),
            Triple("一场梦", "醒来发现只是一场噩梦。", EndingType.NORMAL)
        )
    )
    
    /**
     * 生成随机故事
     */
    /**
     * 生成随机故事
     */
    suspend fun generate(title: String = "随机生成的故事"): Story {
        val nodeCount = random.nextInt(config.minNodes, config.maxNodes + 1)
        val choiceCount = random.nextInt(config.minChoices, config.maxChoices + 1)
        val endingCount = if (config.generateEnding) {
            random.nextInt(config.minEndings, config.maxEndings + 1)
        } else {
            0
        }
        
        val nodes = mutableMapOf<String, StoryNode>()
        val nodeIds = mutableListOf<String>()
        val customItems = mutableListOf<ItemInstance>()
        val enemies = mutableListOf<Enemy>()
        
        // 1. 创建开始节点
        val startNode = createDialogueNode("start", 100f, 100f)
        nodes[startNode.id] = startNode
        nodeIds.add(startNode.id)
        
        var currentX = 100f
        var currentY = 200f
        
        // 2. 创建主要对话节点
        val mainDialogueCount = nodeCount - choiceCount - endingCount
        for (i in 0 until mainDialogueCount) {
            val nodeId = "node_${i + 1}"
            currentY += 120f
            
            // 根据概率决定节点类型
            val nodeType = when {
                random.nextFloat() < config.battleProbability -> NodeType.BATTLE
                random.nextFloat() < config.conditionProbability -> NodeType.CONDITION
                random.nextFloat() < config.itemProbability -> NodeType.ITEM
                random.nextFloat() < config.variableProbability -> NodeType.VARIABLE
                random.nextFloat() < config.randomNodeProbability -> NodeType.RANDOM
                else -> NodeType.DIALOGUE
            }
            
            val node = when (nodeType) {
                NodeType.BATTLE -> createBattleNode(nodeId, currentX, currentY, enemies)
                NodeType.CONDITION -> createConditionNode(nodeId, currentX, currentY)
                NodeType.ITEM -> createItemNode(nodeId, currentX, currentY, customItems)
                NodeType.VARIABLE -> createVariableNode(nodeId, currentX, currentY)
                NodeType.RANDOM -> createRandomNode(nodeId, currentX, currentY)
                else -> createDialogueNode(nodeId, currentX, currentY)
            }
            
            nodes[node.id] = node
            nodeIds.add(node.id)
        }
        
        // 3. 创建选择节点
        for (i in 0 until choiceCount) {
            val nodeId = "choice_${i + 1}"
            currentX += 200f * (if (i % 2 == 0) 1 else -1)
            currentY += 120f
            
            val optionCount = random.nextInt(config.minOptions, config.maxOptions + 1)
            val node = createChoiceNode(nodeId, currentX, currentY, optionCount)
            nodes[node.id] = node
            nodeIds.add(node.id)
        }
        
        // 4. 创建结局节点
        for (i in 0 until endingCount) {
            val nodeId = "ending_${i + 1}"
            currentX = 100f + (i * 180f)
            currentY += 120f
            
            val node = createEndingNode(nodeId, currentX, currentY)
            nodes[node.id] = node
            nodeIds.add(node.id)
        }
        
        // 5. 连接节点
        connectNodes(nodes, nodeIds)
        
        return Story(
            id = "story_${PlatformUtils.getCurrentTimeMillis()}",
            title = title,
            author = "AI生成",
            description = "基于「${getThemeName(config.theme)}」主题随机生成的故事",
            startNodeId = "start",
            nodes = nodes,
            createdAt = PlatformUtils.getCurrentTimeMillis(),
            updatedAt = PlatformUtils.getCurrentTimeMillis(),
            customItems = customItems,
            enemies = enemies
        )
    }



    private suspend fun createDialogueNode(id: String, x: Float, y: Float): StoryNode {
        val templates = dialogueTemplates[config.theme] ?: dialogueTemplates[StoryTheme.FANTASY]!!
        val template = templates[random.nextInt(templates.size)]
        
        var speaker = template.first
        if (speaker != "旁白" && nameProvider != null) {
            // 50%概率替换为随机名字
            if (random.nextBoolean()) {
                speaker = when(config.theme) {
                    StoryTheme.FANTASY, StoryTheme.MYSTERY, StoryTheme.ROMANCE -> nameProvider.getChineseName()
                    else -> nameProvider.getEnglishName()
                }
                // 偶尔加上称号
                if (random.nextBoolean()) {
                    speaker += " (${template.first})"
                }
            }
        }
        
        return StoryNode(
            id = id,
            type = NodeType.DIALOGUE,
            content = NodeContent.Dialogue(
                speaker = speaker,
                text = template.second,
                portrait = template.third
            ),
            position = NodePosition(x, y)
        )
    }
    
    private fun createChoiceNode(id: String, x: Float, y: Float, optionCount: Int): StoryNode {
        val prompts = choicePrompts[config.theme] ?: choicePrompts[StoryTheme.FANTASY]!!
        val options = optionTemplates[config.theme] ?: optionTemplates[StoryTheme.FANTASY]!!
        
        val selectedOptions = options.shuffled(random).take(optionCount).mapIndexed { index, text ->
            ChoiceOption(
                id = "opt_${index + 1}",
                text = text,
                nextNodeId = "" // 稍后连接
            )
        }
        
        return StoryNode(
            id = id,
            type = NodeType.CHOICE,
            content = NodeContent.Choice(
                prompt = prompts[random.nextInt(prompts.size)],
                options = selectedOptions
            ),
            position = NodePosition(x, y)
        )
    }
    
    private fun createEndingNode(id: String, x: Float, y: Float): StoryNode {
        val endings = endingTemplates[config.theme] ?: endingTemplates[StoryTheme.FANTASY]!!
        val ending = endings[random.nextInt(endings.size)]
        
        return StoryNode(
            id = id,
            type = NodeType.END,
            content = NodeContent.Ending(
                title = ending.first,
                description = ending.second,
                endingType = ending.third
            ),
            position = NodePosition(x, y)
        )
    }
    
    private suspend fun createBattleNode(id: String, x: Float, y: Float, generatedEnemies: MutableList<Enemy>): StoryNode {
        // 根据难度计算基础属性
        val baseHp = 50 + (50 * config.difficulty).toInt()
        val baseAtk = 5 + (5 * config.difficulty).toInt()
        
        // 敌人模板列表
        val enemyTemplates = listOf(
            Triple("slime", "史莱姆", Triple(0.5, 0.5, 0)),
            Triple("goblin", "哥布林", Triple(0.8, 1.0, 2)),
            Triple("wolf", "野狼", Triple(1.0, 1.2, 3)),
            Triple("bandit", "强盗", Triple(1.5, 1.1, 5)),
            Triple("skeleton", "骷髅兵", Triple(1.2, 1.3, 8)),
            Triple("dark_knight", "黑暗骑士", Triple(2.5, 1.8, 15)),
            Triple("dragon", "幼龙", Triple(3.0, 2.5, 20))
        )
        
        // 根据难度选择可用敌人范围
        val maxEnemyIndex = (enemyTemplates.size * config.difficulty).toInt().coerceIn(0, enemyTemplates.size - 1)
        val availableEnemies = enemyTemplates.take(maxOf(1, maxEnemyIndex + 1))
        val template = availableEnemies[random.nextInt(availableEnemies.size)]
        
        val (templateId, baseName, statsMods) = template
        val (hpMod, atkMod, defVal) = statsMods
        
        
        val enemyName = nameProvider?.generate("monster_beast") ?: baseName
        
        val enemy = Enemy(
            id = "${templateId}_${id}",
            name = enemyName,
            description = "一个危险的敌人",
            stats = CharacterStats(
                maxHp = (baseHp * hpMod).toInt(),
                currentHp = (baseHp * hpMod).toInt(),
                attack = (baseAtk * atkMod).toInt(),
                defense = defVal,
                speed = 5 + random.nextInt(10)
            ),
            expReward = (10 * (1 + hpMod)).toInt(),
            goldReward = (5 * (1 + hpMod)).toInt()
        )
        
        generatedEnemies.add(enemy)
        
        return StoryNode(
            id = id,
            type = NodeType.BATTLE,
            content = NodeContent.Battle(
                enemyId = enemy.id,
                winNextNodeId = "",
                loseNextNodeId = ""
            ),
            position = NodePosition(x, y)
        )
    }
    
    private fun createConditionNode(id: String, x: Float, y: Float): StoryNode {
        // 根据难度调整数值
        val goldBase = (100 * config.difficulty).toInt() + 10
        val levelBase = (10 * config.difficulty).toInt() + 1
        
        val conditions = listOf(
            "player_level > $levelBase",
            "has_item:key_gold",
            "flag:boss_defeated",
            "gold >= $goldBase",
            "reputation:kingdom > ${if (config.difficulty > 0.5) 10 else 0}"
        )
        return StoryNode(
            id = id,
            type = NodeType.CONDITION,
            content = NodeContent.Condition(
                expression = conditions[random.nextInt(conditions.size)],
                trueNextNodeId = "",
                falseNextNodeId = ""
            ),
            position = NodePosition(x, y)
        )
    }
    
    private suspend fun createItemNode(id: String, x: Float, y: Float, customItems: MutableList<ItemInstance>): StoryNode {
        // 30% 概率获得随机生成的装备 (且必须有 nameProvider)
        if (nameProvider != null && random.nextFloat() < 0.3f) {
            val equipmentInstance = createRandomEquipment(id)
            customItems.add(equipmentInstance)
            
            return StoryNode(
                id = id,
                type = NodeType.ITEM,
                content = NodeContent.ItemAction(
                    itemId = equipmentInstance.uid, // 使用实例ID
                    itemName = equipmentInstance.name,
                    quantity = 1,
                    action = ItemActionType.GIVE,
                    nextNodeId = ""
                ),
                position = NodePosition(x, y)
            )
        }
    
        val items = listOf("potion_hp", "key_gold", "sword_iron", "scroll_magic")
        val actions = ItemActionType.entries
        // 难度越高，获得的数量可能越少
        val maxQty = maxOf(1, 5 - (config.difficulty * 4).toInt())
        
        return StoryNode(
            id = id,
            type = NodeType.ITEM,
            content = NodeContent.ItemAction(
                itemId = items[random.nextInt(items.size)],
                quantity = random.nextInt(1, maxQty + 1),
                action = actions[random.nextInt(actions.size)],
                nextNodeId = ""
            ),
            position = NodePosition(x, y)
        )
    }

    private suspend fun createRandomEquipment(nodeId: String): ItemInstance {
        // 随机选择类型
        val types = listOf("sword", "blade") // 目前 rules.json 只有这两种的生成逻辑
        val type = types.random(random)
        val templateId = "weapon_$type" // 假设基础模板ID
        
        // 生成名字
        val customName = nameProvider!!.getEquipmentName(type)
        
        // 生成属性
        val level = random.nextInt(1, (10 * config.difficulty).toInt() + 2)
        // 稀有度
        val rarityRoll = random.nextFloat()
        val rarity = when {
            rarityRoll < 0.6 -> ItemRarity.COMMON
            rarityRoll < 0.85 -> ItemRarity.UNCOMMON
            rarityRoll < 0.95 -> ItemRarity.RARE
            rarityRoll < 0.99 -> ItemRarity.EPIC
            else -> ItemRarity.LEGENDARY
        }
        
        // 属性加成计算 (简单的线性成长 + 稀有度倍率)
        val rarityMultiplier = when(rarity) {
            ItemRarity.COMMON -> 1.0f
            ItemRarity.UNCOMMON -> 1.5f
            ItemRarity.RARE -> 2.5f
            ItemRarity.EPIC -> 4.0f
            ItemRarity.LEGENDARY -> 6.0f
            ItemRarity.MYTHIC -> 10.0f
        }
        
        val baseStat = (level * 2 * rarityMultiplier).toInt()
        val atk = baseStat + random.nextInt(level)
        val def = if (random.nextBoolean()) (baseStat * 0.5).toInt() else 0
        
        return ItemInstance(
            uid = "inst_${PlatformUtils.getCurrentTimeMillis()}_${random.nextInt(10000)}",
            templateId = templateId,
            name = customName,
            level = level,
            rarity = rarity,
            bonusAttack = atk,
            bonusDefense = def,
            creationTime = PlatformUtils.getCurrentTimeMillis()
        )
    }
    
    private fun createVariableNode(id: String, x: Float, y: Float): StoryNode {
        val variables = listOf("score", "reputation", "gold", "exp")
        val operations = listOf(VariableOperation.ADD, VariableOperation.SET)
        // 难度越高，获得的奖励数值越低
        val baseValue = 100 - (config.difficulty * 80).toInt()
        val value = random.nextInt(1, maxOf(10, baseValue)).toString()
        
        return StoryNode(
            id = id,
            type = NodeType.VARIABLE,
            content = NodeContent.VariableAction(
                variableName = variables[random.nextInt(variables.size)],
                operation = operations[random.nextInt(operations.size)],
                value = value,
                nextNodeId = ""
            ),
            position = NodePosition(x, y)
        )
    }
    
    private fun createRandomNode(id: String, x: Float, y: Float): StoryNode {
        // 创建 2-3 个随机分支
        val branchCount = random.nextInt(2, 4)
        val branches = mutableListOf<RandomBranch>()
        
        // 分配权重 (总和不需要严格为100，这里简单随机分配)
        for (i in 0 until branchCount) {
            branches.add(
                RandomBranch(
                    nextNodeId = "", // 连接阶段填充
                    weight = random.nextInt(20, 80)
                )
            )
        }
        
        return StoryNode(
            id = id,
            type = NodeType.RANDOM,
            content = NodeContent.Random(branches),
            position = NodePosition(x, y)
        )
    }

    private fun connectNodes(nodes: MutableMap<String, StoryNode>, nodeIds: List<String>) {
        // 找出不同类型的节点
        val dialogueNodes = nodeIds.filter { 
            val node = nodes[it]
            node?.type == NodeType.DIALOGUE || node?.type == NodeType.BATTLE ||
            node?.type == NodeType.ITEM || node?.type == NodeType.VARIABLE ||
            node?.type == NodeType.CONDITION || node?.type == NodeType.RANDOM
        }
        val choiceNodes = nodeIds.filter { nodes[it]?.type == NodeType.CHOICE }
        val endingNodes = nodeIds.filter { nodes[it]?.type == NodeType.END }
        
        // 连接对话节点链
        for (i in 0 until dialogueNodes.size - 1) {
            val currentId = dialogueNodes[i]
            
            // Chaos 逻辑：有一定概率连接到随机节点（跳跃或回环），而不是下一个节点
            // 避免连接到自己，且目标必须是 dialogueNodes 中的。
            val nextId = if (random.nextFloat() < config.chaos && dialogueNodes.size > 2) {
                val potentialTargets = dialogueNodes.filter { it != currentId }
                potentialTargets[random.nextInt(potentialTargets.size)]
            } else {
                if (choiceNodes.isNotEmpty() && i == dialogueNodes.size / 2) {
                    choiceNodes.first()
                } else {
                    dialogueNodes[i + 1]
                }
            }
            
            val currentNode = nodes[currentId]!!
            val connection = Connection(nextId)
            nodes[currentId] = currentNode.copy(connections = listOf(connection))
            
            // 更新特殊节点的 nextNodeId
            updateNodeNextId(nodes, currentId, nextId)
        }
        
        // 连接选择节点的选项到不同目标
        choiceNodes.forEach { choiceId ->
            val choiceNode = nodes[choiceId]!!
            val choice = choiceNode.content as NodeContent.Choice
            
            val updatedOptions = choice.options.mapIndexed { index, option ->
                // Chaos 逻辑也会影响选项的目标
                val targetId = if (random.nextFloat() < config.chaos && dialogueNodes.isNotEmpty()) {
                     dialogueNodes[random.nextInt(dialogueNodes.size)]
                } else {
                    when {
                        // 如果没有结局 (无限模式)，并且这是最后一个选项，或者正常逻辑下应该接结局
                        endingNodes.isEmpty() -> {
                            if (dialogueNodes.isNotEmpty()) {
                                // 循环回前面的某个节点，或者连接到未被充分利用的节点
                                dialogueNodes[random.nextInt(dialogueNodes.size)]
                            } else {
                                "start"
                            }
                        }
                        index == 0 && endingNodes.isNotEmpty() -> endingNodes.first()
                        (dialogueNodes.size / 2 + index) < dialogueNodes.size -> dialogueNodes[dialogueNodes.size / 2 + index]
                        endingNodes.isNotEmpty() -> endingNodes[index % endingNodes.size]
                        else -> dialogueNodes.lastOrNull() ?: "start"
                    }
                }
                option.copy(nextNodeId = targetId)
            }
            
            nodes[choiceId] = choiceNode.copy(
                content = choice.copy(options = updatedOptions)
            )
        }
        
        // 确保最后的对话节点连接到结局
        if (dialogueNodes.isNotEmpty() && endingNodes.isNotEmpty()) {
            val lastDialogueId = dialogueNodes.last()
            
            // 检查如果不包含连接才添加，避免覆盖之前的逻辑（如果有）
            // 简单起见，这里强制连接最后一个，除非它已经有连接（在 chaos 逻辑中可能已经有）
            val lastNode = nodes[lastDialogueId]!!
            if (lastNode.connections.isEmpty() || lastNode.connections[0].targetNodeId.isEmpty()) { 
                val endingId = endingNodes.first()
                nodes[lastDialogueId] = lastNode.copy(connections = listOf(Connection(endingId)))
                updateNodeNextId(nodes, lastDialogueId, endingId)
            }
        }
    }
    
    private fun updateNodeNextId(nodes: MutableMap<String, StoryNode>, nodeId: String, nextId: String) {
        val node = nodes[nodeId] ?: return
        val updatedContent = when (val content = node.content) {
            is NodeContent.Battle -> content.copy(winNextNodeId = nextId)
            is NodeContent.Condition -> content.copy(trueNextNodeId = nextId, falseNextNodeId = nextId)
            is NodeContent.ItemAction -> content.copy(nextNodeId = nextId)
            is NodeContent.VariableAction -> content.copy(nextNodeId = nextId)
            is NodeContent.Random -> content.copy(
                branches = content.branches.map { it.copy(nextNodeId = nextId) }
            )
            else -> return
        }
        nodes[nodeId] = node.copy(content = updatedContent)
    }
    
    private fun getThemeName(theme: StoryTheme): String = when (theme) {
        StoryTheme.FANTASY -> "奇幻冒险"
        StoryTheme.SCI_FI -> "科幻太空"
        StoryTheme.MYSTERY -> "悬疑推理"
        StoryTheme.ROMANCE -> "浪漫爱情"
        StoryTheme.HORROR -> "恐怖惊悚"
    }
}
