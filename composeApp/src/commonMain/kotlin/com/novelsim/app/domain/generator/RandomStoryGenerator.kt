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
        val seed: Long? = null,
        
        /** 命名风格 */
        val namingStyle: NamingStyle = NamingStyle.AUTO,
        
        /** 生成规则列表 */
        /** 生成规则列表 */
        val rules: List<GenerationRule> = listOf(
            GenerationRule(type = EntityType.LOCATION, count = 6)
        ),

        /** 敌人属性配置范围 (默认值，可被规则覆盖或作为兜底) */
        val enemyMinHp: Int = 30,
        val enemyMaxHp: Int = 100,
        val enemyMinAtk: Int = 3,
        val enemyMaxAtk: Int = 12,
        val enemyMinDef: Int = 0,
        val enemyMaxDef: Int = 5,
        val enemyMinSpeed: Int = 5,
        val enemyMaxSpeed: Int = 12,

        /** 角色属性配置范围 (默认值，可被规则覆盖或作为兜底) */
        val characterMinHp: Int = 80,
        val characterMaxHp: Int = 150,
        val characterMinAtk: Int = 10,
        val characterMaxAtk: Int = 30,
        val characterMinDef: Int = 5,
        val characterMaxDef: Int = 15,
        val characterMinSpeed: Int = 8,
        val characterMaxSpeed: Int = 18
    )

    /**
     * 实体生成规则
     */
    data class GenerationRule(
        val type: EntityType,
        val templateId: String? = null,
        val count: Int = 1,
        val customStats: List<CustomStatConfig> = emptyList(),
        val basicStats: BasicStatsConfig? = null
    )

    /**
     * 实体类型枚举
     */
    enum class EntityType {
        CHARACTER,
        ENEMY,
        ITEM,
        LOCATION,
        FACTION,
        EVENT,
        CLUE,
        VARIABLE
    }

    /**
     * 自定义属性配置
     */
    data class CustomStatConfig(
        val name: String,
        val min: Int,
        val max: Int,
        val applyTo: Set<EntityType> = setOf(EntityType.CHARACTER, EntityType.ENEMY)
    )

    /**
     * 基础属性配置
     */
    data class BasicStatsConfig(
        val hpMin: Int = 50,
        val hpMax: Int = 100,
        val atkMin: Int = 5,
        val atkMax: Int = 20,
        val defMin: Int = 0,
        val defMax: Int = 10,
        val spdMin: Int = 5,
        val spdMax: Int = 15
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
    
    /**
     * 命名风格
     */
    enum class NamingStyle {
        AUTO,       // 自动 (根据主题)
        CHINESE,    // 中文风格
        WESTERN     // 西方风格
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
        
        // 0. 生成核心元素 (道具、变量、敌人、角色、地点、阵营)
        val generatedItems = generateRandomItems()
        val generatedVariables = generateRandomVariables()
        // 敌人通常随后续节点生成，并按强度排序以支持难度曲线
        val preGeneratedEnemies = generateRandomEnemies().sortedBy { 
            it.stats.maxHp + it.stats.attack * 4 + it.stats.defense * 2 
        }
        enemies.addAll(preGeneratedEnemies)
        
        val generatedCharacters = generateRandomCharacters()
        val generatedLocations = generateRandomLocations()
        val generatedFactions = generateRandomFactions()
        val generatedEvents = generateRandomEvents()
        val generatedClues = generateRandomClues()
        
        // 1. 创建开始节点
        val startNode = createDialogueNode("start", 100f, 100f, generatedCharacters)
        nodes[startNode.id] = startNode
        nodeIds.add(startNode.id)
        
        var currentX = 100f
        var currentY = 200f
        
        // 2. 创建主要对话节点
        val mainDialogueCount = nodeCount - choiceCount - endingCount
        for (i in 0 until mainDialogueCount) {
            val nodeId = "node_${i + 1}"
            currentY += 200f
            
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
                // Pass generated lists to node creators to avoid hardcoding
                NodeType.BATTLE -> {
                    val progress = i.toFloat() / (mainDialogueCount.coerceAtLeast(1))
                    createBattleNode(nodeId, currentX, currentY, enemies, preGeneratedEnemies, progress)
                }
                NodeType.CONDITION -> createConditionNode(nodeId, currentX, currentY, generatedVariables)
                NodeType.ITEM -> createItemNode(nodeId, currentX, currentY, customItems, generatedItems)
                NodeType.VARIABLE -> createVariableNode(nodeId, currentX, currentY, generatedVariables)
                NodeType.RANDOM -> createRandomNode(nodeId, currentX, currentY)
                else -> createDialogueNode(nodeId, currentX, currentY, generatedCharacters)
            }
            
            nodes[node.id] = node
            nodeIds.add(node.id)
        }
        
        // 3. 创建选择节点
        for (i in 0 until choiceCount) {
            val nodeId = "choice_${i + 1}"
            currentX += 200f * (if (i % 2 == 0) 1 else -1)
            currentY += 200f
            
            val optionCount = random.nextInt(config.minOptions, config.maxOptions + 1)
            val node = createChoiceNode(nodeId, currentX, currentY, optionCount)
            nodes[node.id] = node
            nodeIds.add(node.id)
        }
        
        // 4. 创建结局节点
        for (i in 0 until endingCount) {
            val nodeId = "ending_${i + 1}"
            currentX = 100f + (i * 180f)
            currentY += 200f
            
            val node = createEndingNode(nodeId, currentX, currentY)
            nodes[node.id] = node
            nodeIds.add(node.id)
        }

        // 4.5 关联地点 (根据位置距离最近判定)
        val nodesWithLocation = nodes.mapValues { (_, node) ->
            val nearestLocation = generatedLocations.minByOrNull { loc ->
                val dx = node.position.x - loc.position.x
                val dy = node.position.y - loc.position.y
                dx * dx + dy * dy
            }
            node.copy(locationId = nearestLocation?.id)
        }
        nodes.putAll(nodesWithLocation)
        
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
            items = generatedItems,
            variables = generatedVariables,
            customItems = customItems,
            enemies = enemies,
            characters = generatedCharacters,
            locations = generatedLocations,
            factions = generatedFactions,
            events = generatedEvents,
            clues = generatedClues
        )
    }

    private suspend fun generateRandomItems(): List<Item> {
        val items = mutableListOf<Item>()
        
        config.rules.filter { it.type == EntityType.ITEM }.forEach { rule ->
            for (i in 0 until rule.count) {
                val id = "item_${PlatformUtils.getCurrentTimeMillis()}_${items.size}"
                var name = "随机道具 $i"
                var type = ItemType.CONSUMABLE // Default type
                var description = "这是一个随机生成的物品"
                
                if (rule.templateId != null) {
                    name = nameProvider?.generate(rule.templateId) ?: name
                    type = when {
                        rule.templateId.contains("equipment") || rule.templateId.contains("sword") || rule.templateId.contains("blade") || rule.templateId.contains("weapon") -> ItemType.EQUIPMENT
                        rule.templateId.contains("treasure") -> ItemType.MATERIAL
                        rule.templateId.contains("key") -> ItemType.KEY_ITEM
                        rule.templateId.contains("potion") || rule.templateId.contains("food") -> ItemType.CONSUMABLE
                        else -> ItemType.CONSUMABLE // Fallback
                    }
                } else {
                    // Fallback to random type if no template ID
                    type = listOf(ItemType.CONSUMABLE, ItemType.EQUIPMENT, ItemType.MATERIAL, ItemType.KEY_ITEM).random(random)
                }
                
                // 生成自定义属性
                val variables = mutableMapOf<String, String>()
                rule.customStats.forEach { statConfig ->
                    variables[statConfig.name] = random.nextInt(statConfig.min, statConfig.max + 1).toString()
                }

                items.add(
                    Item(
                        id = id,
                        name = name,
                        description = description,
                        type = type,
                        icon = null,
                        price = random.nextInt(10, 500),
                        variables = variables
                    )
                )
            }
        }
        return items
    }

    private suspend fun generateRandomVariables(): Map<String, String> {
        val variables = mutableMapOf<String, String>()
        val commonVars = listOf("honor", "karma", "sanity", "mana", "stamina", "charm")
        
        // 预设一些必然存在的变量
        variables["gold"] = "0"
        variables["exp"] = "0"
        variables["level"] = "1"

        config.rules.filter { it.type == EntityType.VARIABLE }.forEach { rule ->
             for (i in 0 until rule.count) {
                 var name = "var_${random.nextInt(1000)}"
                 
                 if (rule.templateId != null && nameProvider != null) {
                     val randomName = nameProvider.generate(rule.templateId)
                      if (!randomName.startsWith("未知模板")) {
                         name = randomName
                      }
                 } else {
                     // Fallback to common vars or generic name
                     if (i < commonVars.size && random.nextBoolean()) {
                          name = commonVars[i]
                     } else {
                         name = "random_var_${random.nextInt(1000)}"
                     }
                 }
                 
                 // Ensure unique name if generated randomly
                 var uniqueName = name
                 var counter = 0
                 while (variables.containsKey(uniqueName)) {
                     uniqueName = "${name}_${counter++}"
                 }
                 
                 // Default value for randomly generated variables
                 variables[uniqueName] = if (random.nextBoolean()) "0" else random.nextInt(100).toString()
             }
             
             // Add variables defined by customStats within the rule
             rule.customStats.forEach { stat ->
                 variables[stat.name] = random.nextInt(stat.min, stat.max + 1).toString()
             }
        }
        
        return variables
    }

    private suspend fun generateRandomEnemies(): List<Enemy> {
        val enemies = mutableListOf<Enemy>()
        
        config.rules.filter { it.type == EntityType.ENEMY }.forEach { rule ->
            for (i in 0 until rule.count) {
                val id = "enemy_${PlatformUtils.getCurrentTimeMillis()}_${enemies.size}"
                var name = "随机怪物"
                var description = "一只危险的生物"
                
                if (rule.templateId != null) {
                    name = nameProvider?.generate(rule.templateId) ?: name
                } else {
                     val fallbackTemplates = listOf("slime", "goblin", "wolf", "orc", "ghost")
                     name = fallbackTemplates.random(random)
                }
                               // 生成基础属性
                 val stats = if (rule.basicStats != null) {
                     CharacterStats(
                         maxHp = random.nextInt(rule.basicStats.hpMin, rule.basicStats.hpMax + 1),
                         currentHp = 0,
                         attack = random.nextInt(rule.basicStats.atkMin, rule.basicStats.atkMax + 1),
                         defense = random.nextInt(rule.basicStats.defMin, rule.basicStats.defMax + 1),
                         speed = random.nextInt(rule.basicStats.spdMin, rule.basicStats.spdMax + 1),
                         exp = random.nextInt(5, 20)
                     ).apply { currentHp = maxHp }
                 } else {
                     CharacterStats(
                          maxHp = ((random.nextInt(config.enemyMinHp, config.enemyMaxHp + 1)) * (1 + config.difficulty * 0.3)).toInt(),
                          currentHp = 0,
                          attack = ((random.nextInt(config.enemyMinAtk, config.enemyMaxAtk + 1)) * (1 + config.difficulty * 0.3)).toInt(),
                          defense = ((random.nextInt(config.enemyMinDef, config.enemyMaxDef + 1)) * (1 + config.difficulty * 0.2)).toInt(),
                          speed = ((random.nextInt(config.enemyMinSpeed, config.enemyMaxSpeed + 1)) * (1 + config.difficulty * 0.2)).toInt(),
                          exp = (random.nextInt(5, 20) * (1 + config.difficulty)).toInt()
                      ).apply { currentHp = maxHp }
                 }
                
                // 生成自定义属性
                val variables = mutableMapOf<String, String>()
                rule.customStats.forEach { statConfig ->
                    variables[statConfig.name] = random.nextInt(statConfig.min, statConfig.max + 1).toString()
                }
                
                enemies.add(
                     Enemy(
                        id = id,
                        name = name,
                        description = description,
                        stats = stats,
                        expReward = random.nextInt(10, 50),
                        goldReward = random.nextInt(5, 20),
                        variables = variables
                    )
                )
            }
        }
        return enemies
    }

    private suspend fun generateRandomCharacters(): List<Character> {
        val characters = mutableListOf<Character>()
        
        config.rules.filter { it.type == EntityType.CHARACTER }.forEach { rule ->
            for (i in 0 until rule.count) {
                var name = "角色 $i"
                if (nameProvider != null) {
                    if (rule.templateId != null) {
                        name = nameProvider.generate(rule.templateId)
                    } else {
                        val useChinese = when (config.namingStyle) {
                            NamingStyle.CHINESE -> true
                            NamingStyle.WESTERN -> false
                            NamingStyle.AUTO -> when(config.theme) {
                                StoryTheme.FANTASY, StoryTheme.MYSTERY, StoryTheme.ROMANCE -> true
                                else -> false
                            }
                        }
                        name = if (useChinese) nameProvider.getChineseName() else nameProvider.getEnglishName()
                    }
                }
                               // 生成基础属性
                 val stats = if (rule.basicStats != null) {
                     CharacterStats(
                         maxHp = random.nextInt(rule.basicStats.hpMin, rule.basicStats.hpMax + 1),
                         currentHp = 0, // 会在初始化时被设为maxHp
                         attack = random.nextInt(rule.basicStats.atkMin, rule.basicStats.atkMax + 1),
                         defense = random.nextInt(rule.basicStats.defMin, rule.basicStats.defMax + 1),
                         speed = random.nextInt(rule.basicStats.spdMin, rule.basicStats.spdMax + 1),
                         exp = 0
                     ).apply { currentHp = maxHp }
                 } else {
                     CharacterStats(
                         maxHp = random.nextInt(config.characterMinHp, config.characterMaxHp + 1),
                         currentHp = 0,
                         attack = random.nextInt(config.characterMinAtk, config.characterMaxAtk + 1),
                         defense = random.nextInt(config.characterMinDef, config.characterMaxDef + 1),
                         speed = random.nextInt(config.characterMinSpeed, config.characterMaxSpeed + 1),
                         exp = 0
                     ).apply { currentHp = maxHp }
                 }
                
                // 生成自定义属性
                val variables = mutableMapOf<String, String>()
                rule.customStats.forEach { statConfig ->
                    variables[statConfig.name] = random.nextInt(statConfig.min, statConfig.max + 1).toString()
                }
                
                characters.add(Character(
                    id = "char_${PlatformUtils.getCurrentTimeMillis()}_${characters.size}",
                    name = name,
                    description = "这是一个随机生成的角色",
                    baseStats = stats,
                    variables = variables
                ))
            }
        }
        return characters
    }
    
    private suspend fun generateRandomLocations(): List<Location> {
        val locations = mutableListOf<Location>()
        val locationNames = listOf("新手村", "幽暗森林", "荒芜之地", "巨龙巢穴", "神秘遗迹", "繁华城镇")
        
        // 1. 预先计算总地点数，构建全局网格
        // 防止不同 Rule 生成的地点及其内部循环生成的地点重叠
        val locationRules = config.rules.filter { it.type == EntityType.LOCATION }
        val totalLocationCount = locationRules.sumOf { it.count.coerceAtLeast(1) }
        
        // 动态计算网格行列数
        // 地图比例 800:1400 (W:H) ≈ 1:1.75
        // 设 gridCols = C, gridRows = R. 期望 R/C ≈ 1.75 且 R*C >= N
        // C * (1.75 * C) >= N  =>  1.75 * C^2 >= N  =>  C >= sqrt(N / 1.75)
        val gridCols = kotlin.math.ceil(kotlin.math.sqrt(totalLocationCount / 1.75f)).toInt().coerceAtLeast(2)
        val gridRows = kotlin.math.ceil(totalLocationCount.toFloat() / gridCols).toInt()
        
        val cellWidth = 800f / gridCols
        val cellHeight = 1400f / gridRows
        
        // 生成所有可用的格子索引 (col, row) 并随机打乱
        val allSlots = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                allSlots.add(c to r)
            }
        }
        // 使用配置的随机源打乱格子
        val shuffledSlots = allSlots.shuffled(random).toMutableList()
        
        var generatedCount = 0

        // 开始生成
        locationRules.forEach { rule ->
             for (i in 0 until rule.count.coerceAtLeast(1)) {
                 var name = if (i < locationNames.size) locationNames[i] else "地点 $i"
                 // 简化的名字生成逻辑
                 if (nameProvider != null) {
                      if (rule.templateId != null) {
                          name = nameProvider.generate(rule.templateId)
                      } else {
                          val place = nameProvider.generate("place_name")
                          if (!place.startsWith("未知模板")) name = place
                      }
                 }
                 
                // 分配唯一格子
                // 如果格子用完了（理论上不会，因为 gridRows * gridCols >= N），则回退到随机
                val slot = if (shuffledSlots.isNotEmpty()) shuffledSlots.removeAt(0) else (0 to 0)
                val col = slot.first
                val row = slot.second
                
                // 基础坐标 + 随机抖动 (保留 padding)
                // 留出一定的边距，避免贴边或贴邻居太近
                // 每个格子内部留出 padding = min(cellWidth, cellHeight) * 0.2
                val paddingX = cellWidth * 0.15f
                val paddingY = cellHeight * 0.15f
                
                val safeWidth = cellWidth - 2 * paddingX
                val safeHeight = cellHeight - 2 * paddingY
                
                val jitterX = random.nextFloat() * safeWidth
                val jitterY = random.nextFloat() * safeHeight
                
                // 坐标计算 (左上角 + padding + jitter)
                val x = col * cellWidth + paddingX + jitterX
                val y = row * cellHeight + paddingY + jitterY
                
                // 随机大小 (80-160)，但不能超过格子大小的一半太多
                val maxRadius = kotlin.math.min(cellWidth, cellHeight) / 2f
                val radius = (80f + random.nextFloat() * 80f).coerceAtMost(maxRadius)
                
                val variables = mutableMapOf<String, String>()
                rule.customStats.forEach { statConfig ->
                    variables[statConfig.name] = random.nextInt(statConfig.min, statConfig.max + 1).toString()
                }

                locations.add(Location(
                    id = "loc_${PlatformUtils.getCurrentTimeMillis()}_${locations.size}",
                    name = name,
                    description = "区域：$name",
                    position = NodePosition(x, y),
                    radius = radius,
                    variables = variables,
                    connectedLocationIds = emptyList() // 稍后连接
                ))
                generatedCount++
            }
        }
        
        if (locations.isEmpty()) return emptyList()
        if (locations.size == 1) return locations
        
        // 2. 构建连通图 (最小生成树 + 随机边)
        val connected = mutableSetOf<String>()
        val unvisited = locations.toMutableList()
        val connections = mutableMapOf<String, MutableSet<String>>()
        locations.forEach { connections[it.id] = mutableSetOf() }
        
        // 从第一个点开始
        val first = unvisited.removeAt(0)
        connected.add(first.id)
        
        // Prim算法变种生成MST
        while (unvisited.isNotEmpty()) {
            var minIsolator: Location? = null
            var minConnectorId: String? = null
            var minDistanceSq = Float.MAX_VALUE
            
            for (u in unvisited) {
                for (cId in connected) {
                    val cLoc = locations.find { it.id == cId }!!
                    val dx = u.position.x - cLoc.position.x
                    val dy = u.position.y - cLoc.position.y
                    val distSq = dx * dx + dy * dy
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        minIsolator = u
                        minConnectorId = cId
                    }
                }
            }
            
            if (minIsolator != null && minConnectorId != null) {
                unvisited.remove(minIsolator)
                connected.add(minIsolator.id)
                connections[minIsolator.id]!!.add(minConnectorId)
                connections[minConnectorId]!!.add(minIsolator.id)
            } else {
                break
            }
        }
        
        // 3. 添加一些随机额外连接
        val extraEdges = (locations.size * 0.3).toInt()
        repeat(extraEdges) {
            val locA = locations.random()
            val closest = locations
                .filter { it.id != locA.id && !connections[locA.id]!!.contains(it.id) }
                .minByOrNull { 
                    val dx = it.position.x - locA.position.x
                    val dy = it.position.y - locA.position.y
                    dx * dx + dy * dy
                }
            
            if (closest != null) {
                connections[locA.id]!!.add(closest.id)
                connections[closest.id]!!.add(locA.id)
            }
        }
        
        // 4. 更新Locaton对象
        return locations.map { loc ->
            loc.copy(connectedLocationIds = connections[loc.id]!!.toList())
        }
    }
    
    private suspend fun generateRandomFactions(): List<Faction> {
        val factions = mutableListOf<Faction>()
        
        config.rules.filter { it.type == EntityType.FACTION }.forEach { rule ->
            for (i in 0 until rule.count) {
                 var name = "阵营 $i"
                 if (nameProvider != null) {
                      if (rule.templateId != null) {
                          name = nameProvider.generate(rule.templateId)
                      } else {
                          val place = nameProvider.generate("place_name")
                          name = if (!place.startsWith("未知模板")) "$place 派" else "阵营 $i"
                      }
                 }
                 
                 // 生成自定义属性
                 val variables = mutableMapOf<String, String>()
                 rule.customStats.forEach { statConfig ->
                     variables[statConfig.name] = random.nextInt(statConfig.min, statConfig.max + 1).toString()
                 }
    
                 factions.add(Faction(
                     id = "fac_${PlatformUtils.getCurrentTimeMillis()}_${factions.size}",
                     name = name,
                     description = "随机生成的阵营",
                     reputation = 0,
                     variables = variables
                 ))
            }
        }
        return factions
    }

    private suspend fun generateRandomEvents(): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val templates = listOf("Mysterious Signal", "Alien Invasion", "Ancient Ritual", "Festival of Lights", "Dragon Attack")
        
        config.rules.filter { it.type == EntityType.EVENT }.forEach { rule ->
            for (i in 0 until rule.count) {
                val name = if (nameProvider != null) {
                     if (rule.templateId != null) {
                         nameProvider.generate(rule.templateId)
                     } else {
                         "事件: " + templates.random(random)
                     }
                } else {
                     templates.random(random)
                }
                
                // 生成自定义属性
                val variables = mutableMapOf<String, String>()
                rule.customStats.forEach { statConfig ->
                    variables[statConfig.name] = random.nextInt(statConfig.min, statConfig.max + 1).toString()
                }
    
                events.add(GameEvent(
                    id = "evt_${PlatformUtils.getCurrentTimeMillis()}_${events.size}",
                    name = name,
                    description = "随机生成的事件",
                    startNodeId = "", // Event usually connects to a start node
                    variables = variables
                ))
            }
        }
        return events
    }
    
    private suspend fun generateRandomClues(): List<Clue> {
        val clues = mutableListOf<Clue>()
        
        config.rules.filter { it.type == EntityType.CLUE }.forEach { rule ->
            for (i in 0 until rule.count) {
                 var name = "线索 $i"
                 if (nameProvider != null) {
                      if (rule.templateId != null) {
                          name = nameProvider.generate(rule.templateId)
                      } else {
                          name = nameProvider.generate("clue_name")
                          if (name.startsWith("未知模板")) name = "神秘线索 $i"
                      }
                 }
                 
                 // 生成自定义属性
                 val variables = mutableMapOf<String, String>()
                 rule.customStats.forEach { statConfig ->
                     variables[statConfig.name] = random.nextInt(statConfig.min, statConfig.max + 1).toString()
                 }
    
                 clues.add(Clue(
                     id = "clue_${PlatformUtils.getCurrentTimeMillis()}_${clues.size}",
                     name = name,
                     description = "关于故事真相的线索",
                     isKnown = false,
                     variables = variables
                 ))
            }
        }
        return clues
    }

    private suspend fun createDialogueNode(id: String, x: Float, y: Float, generatedCharacters: List<Character>): StoryNode {
        val templates = dialogueTemplates[config.theme] ?: dialogueTemplates[StoryTheme.FANTASY]!!
        val template = templates[random.nextInt(templates.size)]
        
        var speaker = template.first
        
        // 50% chance to use one of the generated characters as speaker
        if (generatedCharacters.isNotEmpty() && random.nextBoolean()) {
            val char = generatedCharacters.random(random)
            speaker = char.name
            // 偶尔加上称号??? No, character name is enough.
        } else if (speaker != "旁白" && nameProvider != null) {
             // 50%概率替换为随机名字 (Fallback if not using generated character)
             // ... existing logic ...
            if (random.nextBoolean()) {
                val useChinese = when (config.namingStyle) {
                    NamingStyle.CHINESE -> true
                    NamingStyle.WESTERN -> false
                    NamingStyle.AUTO -> when(config.theme) {
                        StoryTheme.FANTASY, StoryTheme.MYSTERY, StoryTheme.ROMANCE -> true
                        else -> false
                    }
                }
                
                speaker = if (useChinese) {
                    nameProvider.getChineseName()
                } else {
                    nameProvider.getEnglishName()
                }
            }
        }
        
        return StoryNode(
            id = id,
            type = NodeType.DIALOGUE,
            content = NodeContent.Dialogue(
                speaker = speaker,
                text = template.second,
                portrait = template.third,
                nextNodeId = ""
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
    
    private suspend fun createBattleNode(id: String, x: Float, y: Float, enemies: MutableList<Enemy>, availableEnemies: List<Enemy>, progress: Float = 0f): StoryNode {
        // Reuse or clone a generated enemy 
        // We pick an enemy based on story progress (earlier nodes get weaker enemies)
        
        val templateEnemy = if (availableEnemies.isNotEmpty()) {
            // 通过 progress 在排序后的敌人列表中选择合适的强度级别
            val index = (progress * (availableEnemies.size - 1)).toInt()
            availableEnemies[index]
        } else {
            // Fallback Create on the fly
             val fallbackName = if (nameProvider != null) nameProvider.generate("monster_beast") else "未知怪物"
             Enemy(id="temp", name=fallbackName, description="", stats=CharacterStats(), expReward=10, goldReward=5)
        }
        
        // Clone and modify
        val newEnemyId = "${templateEnemy.id}_$id" // unique ID for this instance
        val newEnemy = templateEnemy.copy(
             id = newEnemyId,
             stats = templateEnemy.stats.copy(
                 currentHp = templateEnemy.stats.maxHp // reset HP
             )
        )
        // Add to the global enemies list for persistence
        enemies.add(newEnemy)
        
        return StoryNode(
            id = id,
            type = NodeType.BATTLE,
            content = NodeContent.Battle(
                enemyId = newEnemy.id,
                winNextNodeId = "",
                loseNextNodeId = ""
            ),
            position = NodePosition(x, y)
        )
    }
    
    private fun createConditionNode(id: String, x: Float, y: Float, generatedVariables: Map<String, String>): StoryNode {
        // 根据难度调整数值
        val goldBase = (100 * config.difficulty).toInt() + 10
        val levelBase = (10 * config.difficulty).toInt() + 1
        
        val conditions = mutableListOf(
            "player_level > $levelBase",
            "gold >= $goldBase"
        )
        
        // Add conditions based on generated variables
        if (generatedVariables.isNotEmpty()) {
             val randomVar = generatedVariables.keys.random(random)
             conditions.add("$randomVar > 0")
        }
        
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
    
    private suspend fun createItemNode(id: String, x: Float, y: Float, customItems: MutableList<ItemInstance>, generatedItems: List<Item>): StoryNode {
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
    
        // Use generated items list instead of hardcoded
        val item = if (generatedItems.isNotEmpty()) {
             generatedItems.random(random)
        } else {
             // Fallback
             Item(id="potion_test", name="治疗药水", description="", type=ItemType.CONSUMABLE)
        }
        
        val actions = ItemActionType.entries
        // 难度越高，获得的数量可能越少
        val maxQty = maxOf(1, 5 - (config.difficulty * 4).toInt())
        
        return StoryNode(
            id = id,
            type = NodeType.ITEM,
            content = NodeContent.ItemAction(
                itemId = item.id,
                itemName = item.name, // Pass name for UI display
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

    private fun createVariableNode(id: String, x: Float, y: Float, generatedVariables: Map<String, String>): StoryNode {
        val operations = listOf(VariableOperation.ADD, VariableOperation.SET)
        // 难度越高，获得的奖励数值越低
        val baseValue = 100 - (config.difficulty * 80).toInt()
        val value = random.nextInt(1, maxOf(10, baseValue)).toString()

        // Pick from generated variables
        val variableName = if (generatedVariables.isNotEmpty()) {
             generatedVariables.keys.random(random)
        } else {
             "score"
        }

        return StoryNode(
            id = id,
            type = NodeType.VARIABLE,
            content = NodeContent.VariableAction(
                variableName = variableName,
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
            
            val newConnections = updatedOptions.map { Connection(it.nextNodeId) }
            
            nodes[choiceId] = choiceNode.copy(
                content = choice.copy(options = updatedOptions),
                connections = newConnections
            )
        }

        // 确保最后的对话节点有连接
        if (dialogueNodes.isNotEmpty()) {
            val lastDialogueId = dialogueNodes.last()
            val lastNode = nodes[lastDialogueId]!!
            
            if (lastNode.connections.isEmpty() || lastNode.connections[0].targetNodeId.isEmpty()) {
                val targetId = if (endingNodes.isNotEmpty()) {
                    endingNodes.first()
                } else {
                    // 无限模式：连接回到中间某个节点形成大循环
                    if (dialogueNodes.size > 2) dialogueNodes[dialogueNodes.size / 2] else "start"
                }
                nodes[lastDialogueId] = lastNode.copy(connections = listOf(Connection(targetId)))
                updateNodeNextId(nodes, lastDialogueId, targetId)
            }
        }
    }

    private fun updateNodeNextId(nodes: MutableMap<String, StoryNode>, nodeId: String, nextId: String) {
        val node = nodes[nodeId] ?: return
        val updatedContent = when (val content = node.content) {
            is NodeContent.Dialogue -> content.copy(nextNodeId = nextId)
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
