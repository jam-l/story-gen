package com.novelsim.app.data.model

import kotlinx.serialization.Serializable

/**
 * 故事节点类型枚举
 */
enum class NodeType {
    /** 对话节点 - 显示文字和对话 */
    DIALOGUE,
    /** 选择节点 - 提供玩家选项 */
    CHOICE,
    /** 条件分支 - 根据条件跳转 */
    CONDITION,
    /** 战斗节点 - 触发战斗 */
    BATTLE,
    /** 道具节点 - 获取/使用道具 */
    ITEM,
    /** 变量操作 - 修改游戏变量 */
    VARIABLE,
    /** 结局节点 - 故事结束 */
    END
}

/**
 * 故事节点在编辑器中的位置
 */
@Serializable
data class NodePosition(
    val x: Float = 0f,
    val y: Float = 0f
)

/**
 * 节点连接
 */
@Serializable
data class Connection(
    val targetNodeId: String,
    val label: String? = null
)

/**
 * 故事节点 - 叙事引擎核心数据结构
 */
@Serializable
data class StoryNode(
    val id: String,
    val type: NodeType,
    val content: NodeContent,
    val position: NodePosition = NodePosition(),
    val connections: List<Connection> = emptyList()
)

/**
 * 节点内容 - 密封类处理不同类型的节点数据
 */
@Serializable
sealed class NodeContent {
    
    /**
     * 对话内容
     */
    @Serializable
    data class Dialogue(
        val speaker: String? = null,
        val text: String,
        val portrait: String? = null,
        val background: String? = null
    ) : NodeContent()
    
    /**
     * 选择内容
     */
    @Serializable
    data class Choice(
        val prompt: String,
        val options: List<ChoiceOption>
    ) : NodeContent()
    
    /**
     * 条件分支内容
     */
    @Serializable
    data class Condition(
        val expression: String,
        val trueNextNodeId: String,
        val falseNextNodeId: String
    ) : NodeContent()
    
    /**
     * 战斗内容
     */
    @Serializable
    data class Battle(
        val enemyId: String,
        val enemyName: String? = null,
        val enemyStats: CharacterStats? = null,
        val winNextNodeId: String,
        val loseNextNodeId: String
    ) : NodeContent()
    
    /**
     * 道具操作内容
     */
    @Serializable
    data class ItemAction(
        val itemId: String,
        val itemName: String? = null,
        val quantity: Int,
        val action: ItemActionType,
        val nextNodeId: String
    ) : NodeContent()
    
    /**
     * 变量操作内容
     */
    @Serializable
    data class VariableAction(
        val variableName: String,
        val operation: VariableOperation,
        val value: String,
        val nextNodeId: String
    ) : NodeContent()
    
    /**
     * 结局内容
     */
    @Serializable
    data class Ending(
        val title: String,
        val description: String,
        val endingType: EndingType = EndingType.NORMAL,
        val expReward: Int = 0,
        val goldReward: Int = 0
    ) : NodeContent()
}

/**
 * 选项数据
 */
@Serializable
data class ChoiceOption(
    val id: String,
    val text: String,
    val nextNodeId: String,
    val condition: String? = null,
    val effects: List<Effect> = emptyList()
)

/**
 * 效果 - 选项触发的效果
 */
@Serializable
sealed class Effect {
    @Serializable
    data class ModifyVariable(
        val variableName: String,
        val operation: VariableOperation,
        val value: String
    ) : Effect()
    
    @Serializable
    data class GiveItem(
        val itemId: String,
        val quantity: Int
    ) : Effect()
    
    @Serializable
    data class RemoveItem(
        val itemId: String,
        val quantity: Int
    ) : Effect()
    
    @Serializable
    data class ModifyAttribute(
        val attribute: String,
        val value: Int
    ) : Effect()
    
    @Serializable
    data class PlaySound(
        val soundId: String
    ) : Effect()
}

/**
 * 变量操作类型
 */
enum class VariableOperation {
    SET,        // 设置值
    ADD,        // 加法
    SUBTRACT,   // 减法
    MULTIPLY,   // 乘法
    DIVIDE      // 除法
}

/**
 * 道具操作类型
 */
enum class ItemActionType {
    GIVE,       // 给予道具
    REMOVE,     // 移除道具
    CHECK       // 检查是否拥有
}

/**
 * 结局类型
 */
enum class EndingType {
    GOOD,       // 好结局
    NORMAL,     // 普通结局
    BAD,        // 坏结局
    SECRET      // 隐藏结局
}

/**
 * 完整的故事数据
 */
@Serializable
data class Story(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val version: String = "1.0.0",
    val startNodeId: String,
    val nodes: Map<String, StoryNode>,
    val variables: Map<String, String> = emptyMap(),
    val items: List<Item> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
