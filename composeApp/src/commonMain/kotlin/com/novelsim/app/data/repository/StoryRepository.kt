package com.novelsim.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.novelsim.app.data.local.DatabaseDriverFactory
import com.novelsim.app.data.model.*
import com.novelsim.app.database.NovelSimulatorDatabase
import com.novelsim.app.database.StoryNode as DbStoryNode
import com.novelsim.app.util.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 故事仓库 - 使用 SQLDelight 分表存储
 * Story 表存储元数据，StoryNode 表存储节点（高效读写）
 */
class StoryRepository(
    databaseDriverFactory: DatabaseDriverFactory
) {
    
    private val database = NovelSimulatorDatabase(databaseDriverFactory.createDriver())
    private val storyQueries = database.storyQueries
    private val nodeQueries = database.storyNodeQueries
    private val characterQueries = database.characterQueries
    private val locationQueries = database.locationQueries
    private val eventQueries = database.gameEventQueries
    private val clueQueries = database.clueQueries
    private val factionQueries = database.factionQueries
    private val enemyQueries = database.enemyQueries
    private val itemQueries = database.itemQueries
    private val skillQueries = database.skillQueries

    
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
        coerceInputValues = true
    }
    
    /**
     * 获取所有故事（作为 Flow）
     */
    val stories: Flow<List<Story>> = storyQueries.getAllStories()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { dbStories -> 
            dbStories.map { loadStoryWithNodes(it.id) }
        }
    
    /**
     * 获取所有故事
     */
    suspend fun getAllStories(): List<Story> {
        val dbStories = storyQueries.getAllStories().executeAsList()
        val sampleId = "sample_story_v2"
        
        // 检查是否存在新版示例故事，如果不存在则自动添加
        // 这确保了老用户也能获得带有新地图功能的示例故事
        if (dbStories.none { it.id == sampleId }) {
            try {
                val newSample = createSampleStory()
                saveStory(newSample)
                // 重新获取列表
                return storyQueries.getAllStories().executeAsList().map { loadStoryWithNodes(it.id) }
            } catch (e: Exception) {
                // 如果插入失败（例如旧数据冲突），则忽略
                e.printStackTrace()
            }
        }
        
        if (dbStories.isEmpty()) {
             // 理论上上面的逻辑已经处理了空的情况，这里作为兜底
             return emptyList()
        }
        
        return dbStories.map { loadStoryWithNodes(it.id) }
    }
    
    /**
     * 根据 ID 获取故事（包含节点）
     */
    suspend fun getStoryById(storyId: String): Story? {
        val dbStory = storyQueries.getStoryById(storyId).executeAsOneOrNull()
            ?: return null
        return loadStoryWithNodes(storyId)
    }
    
    /**
     * 加载故事及其所有节点
     */
    private fun loadStoryWithNodes(storyId: String): Story {
        val dbStory = storyQueries.getStoryById(storyId).executeAsOne()
        val dbNodes = nodeQueries.getNodesForStory(storyId).executeAsList()
        
        val nodesMap = dbNodes.associate { dbNode ->
            dbNode.id to dbNode.toStoryNode()
        }
        
        val variables: Map<String, String> = try {
            json.decodeFromString(dbStory.variablesJson)
        } catch (e: Exception) {
            emptyMap()
        }
        
        return Story(
            id = dbStory.id,
            title = dbStory.title,
            author = dbStory.author,
            description = dbStory.description,
            version = dbStory.version,
            startNodeId = dbStory.startNodeId,
            nodes = nodesMap,
            variables = variables,

            // 从独立表加载怪物
            enemies = enemyQueries.getEnemiesForStory(storyId).executeAsList().map { it.toEnemy() },
            // 从独立表加载物品
            items = itemQueries.getItemsForStory(storyId).executeAsList().map { it.toItem() },
            
            // 加载其他核心元素
            characters = characterQueries.getCharactersForStory(storyId).executeAsList().map { it.toCharacter() },
            locations = locationQueries.getLocationsForStory(storyId).executeAsList().map { it.toLocation() },
            events = eventQueries.getEventsForStory(storyId).executeAsList().map { it.toGameEvent() },
            clues = clueQueries.getCluesForStory(storyId).executeAsList().map { it.toClue() },
            factions = factionQueries.getFactionsForStory(storyId).executeAsList().map { it.toFaction() },
            skills = skillQueries.getSkillsForStory(storyId).executeAsList().map { it.toSkill() },
            
            createdAt = dbStory.createdAt,
            updatedAt = dbStory.updatedAt
        )
    }
    
    /**
     * 保存故事（包括所有节点）
     */
    suspend fun saveStory(story: Story) {
        storyQueries.transaction {
            // 保存故事元数据
            storyQueries.insertStory(
                id = story.id,
                title = story.title,
                author = story.author,
                description = story.description,
                version = story.version,
                startNodeId = story.startNodeId,
                variablesJson = json.encodeToString(story.variables),
                enemiesJson = json.encodeToString(story.enemies),  // 保留字段用于兼容，但不再关键
                createdAt = story.createdAt,
                updatedAt = story.updatedAt
            )

            // 批量保存所有节点
            story.nodes.forEach { (nodeId, node) ->
                nodeQueries.insertNode(
                    id = node.id,
                    storyId = story.id,
                    type = node.type.name,
                    contentJson = json.encodeToString(node.content),
                    positionX = node.position.x.toDouble(),
                    positionY = node.position.y.toDouble(),
                    connectionsJson = json.encodeToString(node.connections)
                )
            }
            
            // 批量保存所有怪物
            story.enemies.forEach { enemy ->
                enemyQueries.insertEnemy(
                    id = enemy.id,
                    storyId = story.id,
                    name = enemy.name,
                    description = enemy.description,
                    statsJson = json.encodeToString(enemy.stats),
                    expReward = enemy.expReward.toLong(),
                    goldReward = enemy.goldReward.toLong(),
                    skillsJson = json.encodeToString(enemy.skills),
                    variablesJson = json.encodeToString(enemy.variables)
                )
            }
            
            // 批量保存所有物品
            story.items.forEach { item ->
                itemQueries.insertItem(
                    id = item.id,
                    storyId = story.id,
                    name = item.name,
                    description = item.description,
                    type = item.type.name,
                    effectJson = json.encodeToString(item.effect),
                    price = item.price.toLong(),
                    stackable = if (item.stackable) 1L else 0L,
                    maxStack = item.maxStack.toLong(),
                    icon = item.icon,
                    variablesJson = json.encodeToString(item.variables)
                )
            }
            
            // 批量保存角色
            story.characters.forEach { character ->
                characterQueries.insertCharacter(
                    id = character.id,
                    storyId = story.id,
                    name = character.name,
                    description = character.description,
                    avatar = character.avatar,
                    baseStatsJson = json.encodeToString(character.baseStats),
                    factionId = character.factionId,
                    relationshipsJson = json.encodeToString(character.relationships),
                    tagsJson = json.encodeToString(character.tags),
                    skillsJson = json.encodeToString(character.skills),
                    variablesJson = json.encodeToString(character.variables)
                )
            }
            
            // 批量保存地点
            story.locations.forEach { location ->
                 locationQueries.insertLocation(
                    id = location.id,
                    storyId = story.id,
                    name = location.name,
                    description = location.description,
                    background = location.background,
                    connectedLocationIdsJson = json.encodeToString(location.connectedLocationIds),
                    npcsJson = json.encodeToString(location.npcs),
                    eventsJson = json.encodeToString(location.events),
                    variablesJson = json.encodeToString(location.variables),
                    positionX = location.position.x.toDouble(),
                    positionY = location.position.y.toDouble(),
                    radius = location.radius.toDouble()
                )
            }

            // 批量保存事件
            story.events.forEach { event ->
                eventQueries.insertEvent(
                    id = event.id,
                    storyId = story.id,
                    name = event.name,
                    description = event.description,
                    startNodeId = event.startNodeId,
                    triggerCondition = event.triggerCondition,
                    priority = event.priority.toLong(),
                    isRepeatable = if (event.isRepeatable) 1L else 0L
                )
            }
            
            // 批量保存线索
            story.clues.forEach { clue ->
                clueQueries.insertClue(
                    id = clue.id,
                    storyId = story.id,
                    name = clue.name,
                    description = clue.description,
                    isKnown = if (clue.isKnown) 1L else 0L
                )
            }

            // 批量保存阵营
            story.factions.forEach { faction ->
                factionQueries.insertFaction(
                    id = faction.id,
                    storyId = story.id,
                    name = faction.name,
                    description = faction.description,
                    reputation = faction.reputation.toLong(),
                    variablesJson = json.encodeToString(faction.variables)
                )
            }
            
            // 批量保存技能
            story.skills.forEach { skill ->
                skillQueries.insertSkill(
                    id = skill.id,
                    storyId = story.id,
                    name = skill.name,
                    description = skill.description,
                    mpCost = skill.mpCost.toLong(),
                    damage = skill.damage.toLong(),
                    heal = skill.heal.toLong(),
                    effectJson = if (skill.effect != null) json.encodeToString(skill.effect) else null,
                    animation = skill.animation
                )
            }
        }
    }
    
    /**
     * 保存单个节点（高效更新）
     */
    suspend fun saveNode(storyId: String, node: StoryNode) {
        nodeQueries.insertNode(
            id = node.id,
            storyId = storyId,
            type = node.type.name,
            contentJson = json.encodeToString(node.content),
            positionX = node.position.x.toDouble(),
            positionY = node.position.y.toDouble(),
            connectionsJson = json.encodeToString(node.connections)
        )
        // 更新故事的 updatedAt
        storyQueries.updateStoryMeta(
            title = storyQueries.getStoryById(storyId).executeAsOne().title,
            description = storyQueries.getStoryById(storyId).executeAsOne().description,
            updatedAt = PlatformUtils.getCurrentTimeMillis(),
            id = storyId
        )
    }
    
    /**
     * 更新节点位置（拖拽优化）
     */
    suspend fun updateNodePosition(storyId: String, nodeId: String, x: Float, y: Float) {
        nodeQueries.updateNodePosition(
            positionX = x.toDouble(),
            positionY = y.toDouble(),
            id = nodeId,
            storyId = storyId
        )
    }
    
    /**
     * 更新节点内容
     */
    suspend fun updateNodeContent(storyId: String, node: StoryNode) {
        nodeQueries.updateNodeContent(
            contentJson = json.encodeToString(node.content),
            id = node.id,
            storyId = storyId
        )
    }
    
    /**
     * 删除节点
     */
    suspend fun deleteNode(storyId: String, nodeId: String) {
        nodeQueries.deleteNode(nodeId, storyId)
    }
    
    /**
     * 删除故事（级联删除所有节点）
     */
    suspend fun deleteStory(storyId: String) {
        nodeQueries.deleteNodesForStory(storyId)
        storyQueries.deleteStory(storyId)
    }
    
    /**
     * 搜索故事
     */
    suspend fun searchStories(query: String): List<Story> {
        return storyQueries.searchStories(query).executeAsList().map { 
            loadStoryWithNodes(it.id) 
        }
    }
    
    /**
     * 将数据库节点转换为领域模型
     */
    private fun DbStoryNode.toStoryNode(): StoryNode {
        val nodeContent: NodeContent = try {
            json.decodeFromString(contentJson)
        } catch (e: Exception) {
            NodeContent.Dialogue(text = "解析错误")
        }
        
        val nodeConnections: List<Connection> = try {
            json.decodeFromString(connectionsJson)
        } catch (e: Exception) {
            emptyList()
        }
        
        return StoryNode(
            id = id,
            type = NodeType.valueOf(type),
            content = nodeContent,
            position = NodePosition(positionX.toFloat(), positionY.toFloat()),
            connections = nodeConnections
        )
    }

    // ============================================================================================
    // 角色管理方法
    // ============================================================================================

    /**
     * 获取故事的所有角色
     */
    suspend fun getCharacters(storyId: String): List<Character> {
        return characterQueries.getCharactersForStory(storyId).executeAsList().map { it.toCharacter() }
    }

    /**
     * 保存角色
     */
    suspend fun saveCharacter(character: Character, storyId: String) {
        characterQueries.insertCharacter(
            id = character.id,
            storyId = storyId,
            name = character.name,
            description = character.description,
            avatar = character.avatar,
            baseStatsJson = json.encodeToString(character.baseStats),
            factionId = character.factionId,
            relationshipsJson = json.encodeToString(character.relationships),
            tagsJson = json.encodeToString(character.tags),
            skillsJson = json.encodeToString(character.skills),
            variablesJson = json.encodeToString(character.variables)
        )
    }

    /**
     * 删除角色
     */
    suspend fun deleteCharacter(characterId: String, storyId: String) {
        characterQueries.deleteCharacter(characterId, storyId)
    }

    /**
     * 数据库实体转领域模型
     */
    private fun com.novelsim.app.database.Character.toCharacter(): Character {
        return Character(
            id = id,
            name = name,
            description = description,
            avatar = avatar,
            baseStats = try { json.decodeFromString(baseStatsJson) } catch (e: Exception) { CharacterStats() },
            factionId = factionId,
            relationships = try { json.decodeFromString(relationshipsJson) } catch (e: Exception) { emptyMap() },
            tags = try { json.decodeFromString(tagsJson) } catch (e: Exception) { emptyList() },
            skills = try { json.decodeFromString(skillsJson) } catch (e: Exception) { emptyList() },
            variables = try { json.decodeFromString(variablesJson) } catch (e: Exception) { emptyMap() }
        )
    }
    
    // ============================================================================================
    // 地点管理方法
    // ============================================================================================

    /**
     * 获取故事的所有地点
     */
    suspend fun getLocations(storyId: String): List<Location> {
        return locationQueries.getLocationsForStory(storyId).executeAsList().map { it.toLocation() }
    }

    /**
     * 保存地点
     */
    suspend fun saveLocation(location: Location, storyId: String) {
        locationQueries.insertLocation(
            id = location.id,
            storyId = storyId,
            name = location.name,
            description = location.description,
            background = location.background,
            connectedLocationIdsJson = json.encodeToString(location.connectedLocationIds),
            npcsJson = json.encodeToString(location.npcs),
            eventsJson = json.encodeToString(location.events),
            variablesJson = json.encodeToString(location.variables),
            positionX = location.position.x.toDouble(),
            positionY = location.position.y.toDouble(),
            radius = location.radius.toDouble()
        )
    }

    /**
     * 删除地点
     */
    suspend fun deleteLocation(locationId: String, storyId: String) {
        locationQueries.deleteLocation(locationId, storyId)
    }

    /**
     * 数据库实体转领域模型
     */
    private fun com.novelsim.app.database.Location.toLocation(): Location {
        return Location(
            id = id,
            name = name,
            description = description,
            background = background,
            connectedLocationIds = try { json.decodeFromString(connectedLocationIdsJson) } catch (e: Exception) { emptyList() },
            npcs = try { json.decodeFromString(npcsJson) } catch (e: Exception) { emptyList() },
            events = try { json.decodeFromString(eventsJson) } catch (e: Exception) { emptyList() },
            variables = try { json.decodeFromString(variablesJson) } catch (e: Exception) { emptyMap() },
            position = NodePosition(positionX.toFloat(), positionY.toFloat()),
            radius = radius.toFloat()
        )
    }
    
    // ============================================================================================
    // 事件管理方法
    // ============================================================================================

    /**
     * 获取故事的所有事件
     */
    suspend fun getEvents(storyId: String): List<GameEvent> {
        return eventQueries.getEventsForStory(storyId).executeAsList().map { it.toGameEvent() }
    }

    /**
     * 保存事件
     */
    suspend fun saveEvent(event: GameEvent, storyId: String) {
        eventQueries.insertEvent(
            id = event.id,
            storyId = storyId,
            name = event.name,
            description = event.description,
            startNodeId = event.startNodeId,
            triggerCondition = event.triggerCondition,
            priority = event.priority.toLong(),
            isRepeatable = if (event.isRepeatable) 1L else 0L
        )
    }

    /**
     * 删除事件
     */
    suspend fun deleteEvent(eventId: String, storyId: String) {
        eventQueries.deleteEvent(eventId, storyId)
    }

    /**
     * 数据库实体转领域模型
     */
    private fun com.novelsim.app.database.GameEvent.toGameEvent(): GameEvent {
        return GameEvent(
            id = id,
            name = name,
            description = description,
            startNodeId = startNodeId,
            triggerCondition = triggerCondition,
            priority = priority.toInt(),
            isRepeatable = isRepeatable == 1L
        )
    }
    // ============================================================================================
    // 线索管理方法
    // ============================================================================================

    /**
     * 获取故事的所有线索
     */
    suspend fun getClues(storyId: String): List<Clue> {
        return clueQueries.getCluesForStory(storyId).executeAsList().map { it.toClue() }
    }

    /**
     * 保存线索
     */
    suspend fun saveClue(clue: Clue, storyId: String) {
        clueQueries.insertClue(
            id = clue.id,
            storyId = storyId,
            name = clue.name,
            description = clue.description,
            isKnown = if (clue.isKnown) 1L else 0L
        )
    }

    /**
     * 删除线索
     */
    suspend fun deleteClue(clueId: String, storyId: String) {
        clueQueries.deleteClue(clueId, storyId)
    }

    /**
     * 数据库实体转领域模型
     */
    private fun com.novelsim.app.database.Clue.toClue(): Clue {
        return Clue(
            id = id,
            name = name,
            description = description,
            isKnown = isKnown == 1L
        )
    }

    // ============================================================================================
    // 阵营管理方法
    // ============================================================================================

    /**
     * 获取故事的所有阵营
     */
    suspend fun getFactions(storyId: String): List<Faction> {
        return factionQueries.getFactionsForStory(storyId).executeAsList().map { it.toFaction() }
    }

    /**
     * 保存阵营
     */
    suspend fun saveFaction(faction: Faction, storyId: String) {
        factionQueries.insertFaction(
            id = faction.id,
            storyId = storyId,
            name = faction.name,
            description = faction.description,
            reputation = faction.reputation.toLong(),
            variablesJson = json.encodeToString(faction.variables)
        )
    }

    /**
     * 删除阵营
     */
    suspend fun deleteFaction(factionId: String, storyId: String) {
        factionQueries.deleteFaction(factionId, storyId)
    }

    /**
     * 数据库实体转领域模型
     */
    private fun com.novelsim.app.database.Faction.toFaction(): Faction {
        return Faction(
            id = id,
            name = name,
            description = description,
            reputation = reputation.toInt(),
            variables = try { json.decodeFromString(variablesJson) } catch (e: Exception) { emptyMap() }
        )
    }

    // ============================================================================================
    // 怪物管理方法 (SQL Table)
    // ============================================================================================

    /**
     * 获取故事的所有怪物
     */
    suspend fun getEnemies(storyId: String): List<Enemy> {
        return enemyQueries.getEnemiesForStory(storyId).executeAsList().map { it.toEnemy() }
    }

    /**
     * 保存怪物
     */
    suspend fun saveEnemy(enemy: Enemy, storyId: String) {
        enemyQueries.insertEnemy(
            id = enemy.id,
            storyId = storyId,
            name = enemy.name,
            description = enemy.description,
            statsJson = json.encodeToString(enemy.stats),
            expReward = enemy.expReward.toLong(),
            goldReward = enemy.goldReward.toLong(),
            skillsJson = json.encodeToString(enemy.skills),
            variablesJson = json.encodeToString(enemy.variables)
        )
    }

    /**
     * 删除怪物
     */
    suspend fun deleteEnemy(enemyId: String, storyId: String) {
        enemyQueries.deleteEnemy(enemyId, storyId)
    }

    /**
     * 数据库实体转领域模型
     */
    private fun com.novelsim.app.database.Enemy.toEnemy(): Enemy {
        return Enemy(
            id = id,
            name = name,
            description = description,
            stats = try { json.decodeFromString(statsJson) } catch (e: Exception) { CharacterStats() },
            expReward = expReward.toInt(),
            goldReward = goldReward.toInt(),
            skills = try { json.decodeFromString(skillsJson) } catch (e: Exception) { emptyList() },
            variables = try { json.decodeFromString(variablesJson) } catch (e: Exception) { emptyMap() }
        )
    }

    // ============================================================================================
    // 道具管理方法 (SQL Table)
    // ============================================================================================

    /**
     * 获取故事的所有道具
     */
    suspend fun getItems(storyId: String): List<Item> {
        return itemQueries.getItemsForStory(storyId).executeAsList().map { it.toItem() }
    }

    /**
     * 保存道具
     */
    suspend fun saveItem(item: Item, storyId: String) {
        itemQueries.insertItem(
            id = item.id,
            storyId = storyId,
            name = item.name,
            description = item.description,
            type = item.type.name,
            effectJson = json.encodeToString(item.effect),
            price = item.price.toLong(),
            stackable = if (item.stackable) 1L else 0L,
            maxStack = item.maxStack.toLong(),
            icon = item.icon,
            variablesJson = json.encodeToString(item.variables)
        )
    }

    /**
     * 删除道具
     */
    suspend fun deleteItem(itemId: String, storyId: String) {
        itemQueries.deleteItem(itemId, storyId)
    }

    /**
     * 数据库实体转领域模型
     */
    private fun com.novelsim.app.database.Item.toItem(): Item {
        return Item(
            id = id,
            name = name,
            description = description,
            type = ItemType.valueOf(type),
            effect = try { effectJson?.let { json.decodeFromString(it) } } catch (e: Exception) { null },
            price = price.toInt(),
            stackable = stackable == 1L,
            maxStack = maxStack.toInt(),
            icon = icon,
            variables = try { json.decodeFromString(variablesJson) } catch (e: Exception) { emptyMap() }
        )
    }
    // ============================================================================================
    // 创建示例故事
    // ============================================================================================
    private fun createSampleStory(): Story {
        val nodes = mutableMapOf<String, StoryNode>()
        
        // 开始节点
        nodes["start"] = StoryNode(
            id = "start",
            type = NodeType.DIALOGUE,
            content = NodeContent.Dialogue(
                speaker = "旁白",
                text = "欢迎来到小说模拟器！这是一个示例故事。"
            ),
            position = NodePosition(200f, 100f),
            connections = listOf(Connection("choice1"))
        )
        
        // 选择节点
        nodes["choice1"] = StoryNode(
            id = "choice1",
            type = NodeType.CHOICE,
            content = NodeContent.Choice(
                prompt = "你想要探索哪里？",
                options = listOf(
                    ChoiceOption("opt1", "进入森林", "forest"),
                    ChoiceOption("opt2", "前往城镇", "town"),
                    ChoiceOption("opt3", "查看背包", "inventory")
                )
            ),
            position = NodePosition(200f, 250f)
        )
        
        // 森林节点
        nodes["forest"] = StoryNode(
            id = "forest",
            type = NodeType.DIALOGUE,
            content = NodeContent.Dialogue(
                speaker = "旁白",
                text = "你走进了一片茂密的森林，阳光透过树叶洒落。"
            ),
            position = NodePosition(50f, 400f),
            connections = listOf(Connection("battle1"))
        )
        
        // 战斗节点
        nodes["battle1"] = StoryNode(
            id = "battle1",
            type = NodeType.BATTLE,
            content = NodeContent.Battle(
                enemyId = "goblin",
                winNextNodeId = "win_ending",
                loseNextNodeId = "lose_ending"
            ),
            position = NodePosition(50f, 550f)
        )
        
        // ... (省略其他节点)

        // 示例敌人
        val stats = CharacterStats(
            maxHp = 30,
            currentHp = 30,
            attack = 8,
            defense = 3
        )
        // 演示：添加自定义属性
        stats["sanity"] = 50 
        
        val sampleEnemy = Enemy(
            id = "goblin",
            name = "哥布林",
            description = "狡猾的绿皮小怪物",
            stats = stats,
            expReward = 10,
            goldReward = 5
        )
        
        // 城镇节点
        nodes["town"] = StoryNode(
            id = "town",
            type = NodeType.DIALOGUE,
            content = NodeContent.Dialogue(
                speaker = "商人",
                text = "欢迎来到和平小镇！这里有各种商品出售。"
            ),
            position = NodePosition(350f, 400f),
            connections = listOf(Connection("good_ending"))
        )
        
        // 背包节点
        nodes["inventory"] = StoryNode(
            id = "inventory",
            type = NodeType.ITEM,
            content = NodeContent.ItemAction(
                itemId = "potion",
                itemName = "生命药水",
                quantity = 1,
                action = ItemActionType.GIVE,
                nextNodeId = "choice1"
            ),
            position = NodePosition(200f, 400f)
        )
        
        // 好结局
        nodes["good_ending"] = StoryNode(
            id = "good_ending",
            type = NodeType.END,
            content = NodeContent.Ending(
                title = "和平结局",
                description = "你在小镇度过了愉快的时光，结交了许多朋友。",
                endingType = EndingType.GOOD
            ),
            position = NodePosition(350f, 550f)
        )
        
        // 胜利结局
        nodes["win_ending"] = StoryNode(
            id = "win_ending",
            type = NodeType.END,
            content = NodeContent.Ending(
                title = "英雄结局",
                description = "你击败了哥布林，成为了森林的守护者！",
                endingType = EndingType.GOOD,
                expReward = 50,
                goldReward = 20
            ),
            position = NodePosition(50f, 700f)
        )
        
        // 失败结局
        nodes["lose_ending"] = StoryNode(
            id = "lose_ending",
            type = NodeType.END,
            content = NodeContent.Ending(
                title = "冒险失败",
                description = "你被哥布林击败了...",
                endingType = EndingType.BAD
            ),
            position = NodePosition(200f, 700f)
        )
        
        // 创建地点
        val townLoc = Location(
            id = "loc_town",
            name = "和平小镇",
            description = "新手村，也就是你的家乡。",
            position = NodePosition(350f, 400f),
            radius = 300f,
            connectedLocationIds = listOf("loc_forest")
        )
        
        val forestLoc = Location(
            id = "loc_forest",
            name = "迷雾森林",
            description = "充满了危险和机遇的森林。",
            position = NodePosition(50f, 400f),
            radius = 400f,
            connectedLocationIds = listOf("loc_town", "loc_mountain")
        )
        
        val mountainLoc = Location(
            id = "loc_mountain",
            name = "厄运山脉",
            description = "决战之地。",
            position = NodePosition(200f, 800f),
            radius = 250f,
            connectedLocationIds = listOf("loc_forest")
        )

        // 更新节点所属地点
        nodes["town"] = nodes["town"]!!.copy(locationId = townLoc.id)
        nodes["choice1"] = nodes["choice1"]!!.copy(locationId = townLoc.id)
        nodes["inventory"] = nodes["inventory"]!!.copy(locationId = townLoc.id)
        nodes["good_ending"] = nodes["good_ending"]!!.copy(locationId = townLoc.id)
        
        nodes["forest"] = nodes["forest"]!!.copy(locationId = forestLoc.id)
        nodes["battle1"] = nodes["battle1"]!!.copy(locationId = forestLoc.id)
        
        nodes["win_ending"] = nodes["win_ending"]!!.copy(locationId = mountainLoc.id)
        nodes["lose_ending"] = nodes["lose_ending"]!!.copy(locationId = mountainLoc.id)

        return Story(
            id = "sample_story_v3", // Force update for new map features and coordinate fix
            title = "示例冒险",
            author = "系统",
            description = "一个简单的冒险故事，展示游戏的基本功能。",
            startNodeId = "start",
            nodes = nodes,
            enemies = listOf(sampleEnemy),
            locations = listOf(townLoc, forestLoc, mountainLoc), // 添加地点列表
            skills = emptyList(),
            createdAt = PlatformUtils.getCurrentTimeMillis(),
            updatedAt = PlatformUtils.getCurrentTimeMillis()
        )
    }

    
    // ============================================================================================
    // 技能管理方法
    // ============================================================================================

    /**
     * 获取故事的所有技能
     */
    suspend fun getSkills(storyId: String): List<Skill> {
        return skillQueries.getSkillsForStory(storyId).executeAsList().map { it.toSkill() }
    }

    /**
     * 保存技能
     */
    suspend fun saveSkill(skill: Skill, storyId: String) {
        skillQueries.insertSkill(
            id = skill.id,
            storyId = storyId,
            name = skill.name,
            description = skill.description,
            mpCost = skill.mpCost.toLong(),
            damage = skill.damage.toLong(),
            heal = skill.heal.toLong(),
            effectJson = if (skill.effect != null) json.encodeToString(skill.effect) else null,
            animation = skill.animation
        )
    }

    /**
     * 删除技能
     */
    suspend fun deleteSkill(skillId: String, storyId: String) {
        skillQueries.deleteSkill(skillId, storyId)
    }

    /**
     * 数据库实体转领域模型
     */
    private fun com.novelsim.app.database.Skill.toSkill(): Skill {
        return Skill(
            id = id,
            name = name,
            description = description,
            mpCost = mpCost.toInt(),
            damage = damage.toInt(),
            heal = heal.toInt(),
            effect = if (effectJson != null) json.decodeFromString(effectJson) else null,
            animation = animation
        )
    }
}
