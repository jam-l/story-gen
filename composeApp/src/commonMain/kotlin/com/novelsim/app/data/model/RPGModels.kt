package com.novelsim.app.data.model

import kotlinx.serialization.Serializable

/**
 * 角色属性数据
 */
@Serializable
data class CharacterStats(
    val maxHp: Int = 100,
    val currentHp: Int = 100,
    val maxMp: Int = 50,
    val currentMp: Int = 50,
    val attack: Int = 10,
    val defense: Int = 5,
    val speed: Int = 10,
    val luck: Int = 5,
    val level: Int = 1,
    val exp: Int = 0,
    val expToNextLevel: Int = 100
) {
    /** 是否存活 */
    val isAlive: Boolean get() = currentHp > 0
    
    /** HP 百分比 */
    val hpPercent: Float get() = currentHp.toFloat() / maxHp.coerceAtLeast(1)
    
    /** MP 百分比 */
    val mpPercent: Float get() = currentMp.toFloat() / maxMp.coerceAtLeast(1)
}

/**
 * 道具类型
 */
enum class ItemType {
    /** 消耗品 - 可使用后消失 */
    CONSUMABLE,
    /** 装备 - 可穿戴 */
    EQUIPMENT,
    /** 关键道具 - 剧情道具 */
    KEY_ITEM,
    /** 材料 - 合成材料 */
    MATERIAL
}

/**
 * 装备槽位
 */
enum class EquipSlot {
    WEAPON,     // 武器
    ARMOR,      // 护甲
    ACCESSORY,  // 饰品
    HEAD,       // 头部
    BOOTS       // 鞋子
}

/**
 * 道具效果
 */
@Serializable
sealed class ItemEffect {
    /** 恢复效果 */
    @Serializable
    data class Heal(
        val hp: Int = 0,
        val mp: Int = 0
    ) : ItemEffect()
    
    /** Buff 效果 */
    @Serializable
    data class Buff(
        val attribute: String,
        val value: Int,
        val duration: Int = 3
    ) : ItemEffect()
    
    /** 装备属性加成 */
    @Serializable
    data class EquipmentBonus(
        val slot: EquipSlot,
        val attackBonus: Int = 0,
        val defenseBonus: Int = 0,
        val hpBonus: Int = 0,
        val mpBonus: Int = 0,
        val speedBonus: Int = 0
    ) : ItemEffect()
}

/**
 * 道具数据
 */
@Serializable
data class Item(
    val id: String,
    val name: String,
    val description: String,
    val type: ItemType,
    val effect: ItemEffect? = null,
    val icon: String? = null,
    val price: Int = 0,
    val stackable: Boolean = true,
    val maxStack: Int = 99
)

/**
 * 道具稀有度
 */
enum class ItemRarity {
    COMMON,     // 普通
    UNCOMMON,   // 优秀
    RARE,       // 稀有
    EPIC,       // 史诗
    LEGENDARY,  // 传说
    MYTHIC      // 神话
}

/**
 * 道具实例 - 也就是背包里的具体物品（用于支持随机属性）
 */
@Serializable
data class ItemInstance(
    val uid: String,            // 唯一ID (UUID)
    val templateId: String,     // 模板ID (关联 Item.id)
    val name: String,           // 显示名称 (如: "狂暴的铁剑")
    val level: Int = 1,         // 物品等级
    val rarity: ItemRarity = ItemRarity.COMMON,
    // 额外的属性加成 (叠加在模板基础属性上)
    val bonusAttack: Int = 0,
    val bonusDefense: Int = 0,
    val bonusHp: Int = 0,
    val bonusMp: Int = 0,
    val bonusSpeed: Int = 0,
    val creationTime: Long = 0L
)

/**
 * 背包槽位
 */
@Serializable
data class InventorySlot(
    val itemId: String,
    val quantity: Int,
    val instanceId: String? = null // 如果是不可堆叠的物品(装备)，这里存储实例ID
)

/**
 * 装备栏 (存储的是 instanceId)
 */
@Serializable
data class Equipment(
    val weapon: String? = null,
    val armor: String? = null,
    val accessory: String? = null,
    val head: String? = null,
    val boots: String? = null
)

/**
 * 敌人数据
 */
@Serializable
data class Enemy(
    val id: String,
    val name: String,
    val description: String,
    val stats: CharacterStats,
    val sprite: String? = null,
    val skills: List<String> = emptyList(),
    val drops: List<EnemyDrop> = emptyList(),
    val expReward: Int = 10,
    val goldReward: Int = 5
)

/**
 * 敌人掉落
 */
@Serializable
data class EnemyDrop(
    val itemId: String,
    val chance: Float,
    val minQuantity: Int = 1,
    val maxQuantity: Int = 1
)

/**
 * 技能数据
 */
@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val mpCost: Int,
    val damage: Int = 0,
    val heal: Int = 0,
    val effect: SkillEffect? = null,
    val animation: String? = null
)

/**
 * 技能效果
 */
@Serializable
sealed class SkillEffect {
    @Serializable
    data class Damage(
        val multiplier: Float,
        val element: String? = null
    ) : SkillEffect()
    
    @Serializable
    data class Heal(
        val amount: Int,
        val isPercentage: Boolean = false
    ) : SkillEffect()
    
    @Serializable
    data class StatusEffect(
        val status: String,
        val duration: Int,
        val chance: Float = 1f
    ) : SkillEffect()
}

/**
 * 游戏状态 - 包含当前游戏进度的所有数据
 */
@Serializable
data class GameState(
    val storyId: String,
    val currentNodeId: String,
    val playerStats: CharacterStats = CharacterStats(),
    val inventory: List<InventorySlot> = emptyList(),
    // 存储所有具体的物品实例，key = instanceUid
    val itemInstances: MutableMap<String, ItemInstance> = mutableMapOf(),
    val equipment: Equipment = Equipment(),
    val variables: MutableMap<String, String> = mutableMapOf(),
    val gold: Int = 0,
    val playTime: Long = 0L,
    val flags: MutableSet<String> = mutableSetOf(),
    val collectedClues: MutableSet<String> = mutableSetOf(),
    val factionReputations: MutableMap<String, Int> = mutableMapOf(),
    val characterRelationships: MutableMap<String, Int> = mutableMapOf(),
    val triggeredEvents: MutableSet<String> = mutableSetOf(),
    val history: List<HistoryItem> = emptyList()
)

@Serializable
data class HistoryItem(
    val nodeId: String,
    val text: String,
    val type: HistoryType,
    val timestamp: Long
)

enum class HistoryType {
    NODE, CHOICE, BATTLE
}

/**
 * 存档数据
 */
@Serializable
data class SaveData(
    val id: String,
    val slotIndex: Int,
    val storyId: String,
    val storyTitle: String,
    val gameState: GameState,
    val timestamp: Long,
    val playTime: Long,
    val screenshotPath: String? = null,
    val currentNodePreview: String? = null
) {
    /** 格式化的游玩时间 */
    val formattedPlayTime: String
        get() {
            val hours = playTime / 3600000
            val minutes = (playTime % 3600000) / 60000
            val seconds = (playTime % 60000) / 1000
            return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
}
