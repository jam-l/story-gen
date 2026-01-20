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
        if (dbStories.isEmpty()) {
            // 首次运行，插入示例故事
            saveStory(createSampleStory())
            return listOf(loadStoryWithNodes("sample_story"))
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
            createdAt = dbStory.createdAt,
            updatedAt = dbStory.updatedAt
        )
    }
    
    /**
     * 保存故事（包括所有节点）
     */
    suspend fun saveStory(story: Story) {
        // 保存故事元数据
        storyQueries.insertStory(
            id = story.id,
            title = story.title,
            author = story.author,
            description = story.description,
            version = story.version,
            startNodeId = story.startNodeId,
            variablesJson = json.encodeToString(story.variables),
            createdAt = story.createdAt,
            updatedAt = story.updatedAt
        )
        
        // 保存所有节点
        story.nodes.forEach { (nodeId, node) ->
            saveNode(story.id, node)
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
    
    /**
     * 创建示例故事
     */
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
                enemyName = "哥布林",
                enemyStats = CharacterStats(
                    maxHp = 30,
                    currentHp = 30,
                    attack = 8,
                    defense = 3
                ),
                winNextNodeId = "win_ending",
                loseNextNodeId = "lose_ending"
            ),
            position = NodePosition(50f, 550f)
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
        
        return Story(
            id = "sample_story",
            title = "示例冒险",
            author = "系统",
            description = "一个简单的冒险故事，展示游戏的基本功能。",
            startNodeId = "start",
            nodes = nodes,
            createdAt = PlatformUtils.getCurrentTimeMillis(),
            updatedAt = PlatformUtils.getCurrentTimeMillis()
        )
    }
}
