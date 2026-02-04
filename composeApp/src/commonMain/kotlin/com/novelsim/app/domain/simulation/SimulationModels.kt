package com.novelsim.app.domain.simulation

import kotlinx.serialization.Serializable

/**
 * 仿真对象定义 - 定义一类对象的行为和属性模板
 */
@Serializable
data class ObjectDefinition(
    val id: String,
    val name: String,
    val description: String,
    /** 初始状态变量 (e.g., "isOpen": "false", "health": "100") */
    val defaultState: Map<String, String> = emptyMap(),
    /** 对象拥有的交互动作 */
    val interactions: List<InteractionRule> = emptyList(),
    /** 对象的标签 (e.g., "flammable", "container", "weapon") */
    val tags: List<String> = emptyList()
)

/**
 * 交互规则 - 定义动作触发条件和效果
 */
@Serializable
data class InteractionRule(
    val id: String,
    val name: String,     // 动作名称，如 "打开", "攻击"
    val description: String, // 描述模板，如 "你打开了{target.name}。"
    
    /** 触发条件表达式 (e.g., "@target:isOpen == false && @player:hasItem('key')") */
    val condition: String? = null,
    
    /** 执行后的效果列表 */
    val effects: List<SimEffect> = emptyList(),
    
    /** 动作类型 (用于 UI 分组或图标) */
    val type: InteractionType = InteractionType.INTERACT,

    /** 优先级 (当多个动作可用时，高优先级的排前面) */
    val priority: Int = 0
)

enum class InteractionType {
    OBSERVE,    // 观察
    INTERACT,   // 交互 (开关、使用)
    COMBAT,     // 战斗
    MOVEMENT,   // 移动
    TALK,       // 对话
    TAKE        // 拾取
}

/**
 * 仿真效果 - 动作产生的状态变更
 */
@Serializable
sealed class SimEffect {
    /** 修改目标对象的状态 */
    @Serializable
    data class ModifyState(
        val key: String,
        val value: String,
        val targetId: String? = null // null 表示当前交互对象
    ) : SimEffect()

    /** 修改玩家或全局变量 */
    @Serializable
    data class ModifyVariable(
        val key: String,
        val value: String,
        val operation: String = "SET" // SET, ADD, SUB
    ) : SimEffect()

    /** 获得物品 */
    @Serializable
    data class GiveItem(
        val itemId: String,
        val count: Int = 1
    ) : SimEffect()
    
    /** 移除物品 */
    @Serializable
    data class RemoveItem(
        val itemId: String,
        val count: Int = 1
    ) : SimEffect()

    /** 触发事件 */
    @Serializable
    data class TriggerEvent(
        val eventId: String
    ) : SimEffect()

    /** 输出文本 (覆盖默认描述) */
    @Serializable
    data class ShowMessage(
        val message: String
    ) : SimEffect()
}

/**
 * 运行时可用的动作实例
 */
data class SimAction(
    val rule: InteractionRule,
    val targetId: String,
    val targetName: String
)
