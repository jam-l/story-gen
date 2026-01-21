package com.novelsim.app.data.repository

import com.novelsim.app.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 故事导入导出工具
 */
object StoryExporter {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 将故事导出为 JSON 字符串
     */
    fun exportToJson(
        story: Story,
        characters: List<Character> = emptyList(),
        locations: List<Location> = emptyList(),
        events: List<GameEvent> = emptyList(),
        clues: List<Clue> = emptyList(),
        factions: List<Faction> = emptyList()
    ): String {
        val pkg = StoryPackage(
            story = story,
            characters = characters,
            locations = locations,
            events = events,
            clues = clues,
            factions = factions
        )
        return json.encodeToString(pkg)
    }
    
    /**
     * 从 JSON 字符串导入故事
     */
    fun importFromJson(jsonString: String): Result<StoryPackage> {
        return try {
            // 尝试作为 StoryPackage 解析
            val pkg = json.decodeFromString<StoryPackage>(jsonString)
            Result.success(pkg)
        } catch (e: Exception) {
            try {
                // 向后兼容：尝试作为旧版 Story 解析
                val story = json.decodeFromString<Story>(jsonString)
                Result.success(StoryPackage(story))
            } catch (e2: Exception) {
                Result.failure(Exception("导入失败: ${e.message}"))
            }
        }
    }
    
    /**
     * 验证故事结构的完整性
     */
    fun validateStory(story: Story): List<String> {
        val errors = mutableListOf<String>()
        
        // 检查起始节点
        if (story.nodes[story.startNodeId] == null) {
            errors.add("起始节点 '${story.startNodeId}' 不存在")
        }
        
        // 检查所有节点的连接
        story.nodes.values.forEach { node ->
            node.connections.forEach { connection ->
                if (story.nodes[connection.targetNodeId] == null) {
                    errors.add("节点 '${node.id}' 引用了不存在的目标节点 '${connection.targetNodeId}'")
                }
            }
            
            // 检查选项节点的目标
            when (val content = node.content) {
                is com.novelsim.app.data.model.NodeContent.Choice -> {
                    content.options.forEach { option ->
                        if (option.nextNodeId.isNotEmpty() && story.nodes[option.nextNodeId] == null) {
                            errors.add("选项 '${option.text}' 引用了不存在的节点 '${option.nextNodeId}'")
                        }
                    }
                }
                is com.novelsim.app.data.model.NodeContent.Condition -> {
                    if (content.trueNextNodeId.isNotEmpty() && story.nodes[content.trueNextNodeId] == null) {
                        errors.add("条件节点 '${node.id}' 的 true 分支引用了不存在的节点")
                    }
                    if (content.falseNextNodeId.isNotEmpty() && story.nodes[content.falseNextNodeId] == null) {
                        errors.add("条件节点 '${node.id}' 的 false 分支引用了不存在的节点")
                    }
                }
                is com.novelsim.app.data.model.NodeContent.Battle -> {
                    if (content.winNextNodeId.isNotEmpty() && story.nodes[content.winNextNodeId] == null) {
                        errors.add("战斗节点 '${node.id}' 的胜利分支引用了不存在的节点")
                    }
                    if (content.loseNextNodeId.isNotEmpty() && story.nodes[content.loseNextNodeId] == null) {
                        errors.add("战斗节点 '${node.id}' 的失败分支引用了不存在的节点")
                    }
                }
                else -> {}
            }
        }
        
        return errors
    }
}
