package com.novelsim.app.data.model

import kotlinx.serialization.Serializable

/**
 * 角色属性数据
 */
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 角色属性数据
 * 重构说明：现在底层使用 Map 存储，支持自定义属性
 */
@Serializable(with = CharacterStatsSerializer::class)
class CharacterStats(
    val data: MutableMap<String, Int> = mutableMapOf()
) {
    // -------------------------------------------------------------------------
    // 标准属性 (便捷访问器)
    // -------------------------------------------------------------------------
    
    var maxHp: Int
        get() = data.getOrElse("maxHp") { 100 }
        set(value) { data["maxHp"] = value }
        
    var currentHp: Int
        get() = data.getOrElse("currentHp") { 100 }
        set(value) { data["currentHp"] = value }
        
    var maxMp: Int
        get() = data.getOrElse("maxMp") { 50 }
        set(value) { data["maxMp"] = value }
        
    var currentMp: Int
        get() = data.getOrElse("currentMp") { 50 }
        set(value) { data["currentMp"] = value }
        
    var attack: Int
        get() = data.getOrElse("attack") { 10 }
        set(value) { data["attack"] = value }
        
    var defense: Int
        get() = data.getOrElse("defense") { 5 }
        set(value) { data["defense"] = value }
        
    var speed: Int
        get() = data.getOrElse("speed") { 10 }
        set(value) { data["speed"] = value }
        
    var luck: Int
        get() = data.getOrElse("luck") { 5 }
        set(value) { data["luck"] = value }
        
    var level: Int
        get() = data.getOrElse("level") { 1 }
        set(value) { data["level"] = value }
        
    var exp: Int
        get() = data.getOrElse("exp") { 0 }
        set(value) { data["exp"] = value }
        
    var expToNextLevel: Int
        get() = data.getOrElse("expToNextLevel") { 100 }
        set(value) { data["expToNextLevel"] = value }

    // -------------------------------------------------------------------------
    // 衍生属性
    // -------------------------------------------------------------------------

    /** 是否存活 */
    val isAlive: Boolean get() = currentHp > 0
    
    /** HP 百分比 */
    val hpPercent: Float get() = currentHp.toFloat() / maxHp.coerceAtLeast(1)
    
    /** MP 百分比 */
    val mpPercent: Float get() = currentMp.toFloat() / maxMp.coerceAtLeast(1)

    // -------------------------------------------------------------------------
    // 自定义属性访问
    // -------------------------------------------------------------------------
    
    operator fun get(key: String): Int = data[key] ?: 0
    operator fun set(key: String, value: Int) { data[key] = value }
    
    // -------------------------------------------------------------------------
    // 常用方法重写
    // -------------------------------------------------------------------------
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CharacterStats
        return data == other.data
    }

    override fun hashCode(): Int = data.hashCode()

    override fun toString(): String = "CharacterStats(data=$data)"

    // -------------------------------------------------------------------------
    // 兼容性方法
    // -------------------------------------------------------------------------
    fun copy(
        maxHp: Int = this.maxHp,
        currentHp: Int = this.currentHp,
        maxMp: Int = this.maxMp,
        currentMp: Int = this.currentMp,
        attack: Int = this.attack,
        defense: Int = this.defense,
        speed: Int = this.speed,
        luck: Int = this.luck,
        level: Int = this.level,
        exp: Int = this.exp,
        expToNextLevel: Int = this.expToNextLevel
    ): CharacterStats {
        val newMap = data.toMutableMap()
        // 覆盖标准属性
        newMap["maxHp"] = maxHp
        newMap["currentHp"] = currentHp
        newMap["maxMp"] = maxMp
        newMap["currentMp"] = currentMp
        newMap["attack"] = attack
        newMap["defense"] = defense
        newMap["speed"] = speed
        newMap["luck"] = luck
        newMap["level"] = level
        newMap["exp"] = exp
        newMap["expToNextLevel"] = expToNextLevel
        return CharacterStats(newMap)
    }

    // -------------------------------------------------------------------------
    // 经验与等级逻辑
    // -------------------------------------------------------------------------

    /**
     * 添加经验值
     * @return 是否升级
     */
    fun addExp(amount: Int): Boolean {
        exp += amount
        var leveledUp = false
        while (exp >= expToNextLevel) {
            levelUp()
            leveledUp = true
        }
        return leveledUp
    }

    /**
     * 升级逻辑
     */
    private fun levelUp() {
        exp -= expToNextLevel
        level += 1
        
        // 升级所需的经验值按指数增长
        expToNextLevel = (expToNextLevel * 1.2).toInt()
        
        // 提升基础属性
        maxHp = (maxHp * 1.1).toInt() + 5
        currentHp = maxHp // 升级回满血
        
        maxMp = (maxMp * 1.05).toInt() + 2
        currentMp = maxMp // 升级回满蓝
        
        attack = (attack * 1.1).toInt() + 2
        defense = (defense * 1.1).toInt() + 1
        speed = (speed * 1.05).toInt() + 1
        luck = (luck * 1.05).toInt() + 1
    }

    companion object {
        // 伪构造函数，保持源码兼容性
        operator fun invoke(
            maxHp: Int = 100,
            currentHp: Int = 100,
            maxMp: Int = 50,
            currentMp: Int = 50,
            attack: Int = 10,
            defense: Int = 5,
            speed: Int = 10,
            luck: Int = 5,
            level: Int = 1,
            exp: Int = 0,
            expToNextLevel: Int = 100
        ): CharacterStats {
            val map = mutableMapOf<String, Int>()
            map["maxHp"] = maxHp
            map["currentHp"] = currentHp
            map["maxMp"] = maxMp
            map["currentMp"] = currentMp
            map["attack"] = attack
            map["defense"] = defense
            map["speed"] = speed
            map["luck"] = luck
            map["level"] = level
            map["exp"] = exp
            map["expToNextLevel"] = expToNextLevel
            return CharacterStats(map)
        }
    }
}

/**
 * CharacterStats 的自定义序列化器，用于将 Map 扁平化存取
 */
object CharacterStatsSerializer : KSerializer<CharacterStats> {
    private val mapSerializer = MapSerializer(String.serializer(), Int.serializer())
    
    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: CharacterStats) {
        mapSerializer.serialize(encoder, value.data)
    }

    override fun deserialize(decoder: Decoder): CharacterStats {
        val map = mapSerializer.deserialize(decoder)
        return CharacterStats(map.toMutableMap())
    }
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
    val maxStack: Int = 99,
    val variables: Map<String, String> = emptyMap() // 实体级变量
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
    val goldReward: Int = 5,
    val variables: Map<String, String> = emptyMap() // 实体级变量
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
    val skills: List<String> = emptyList(), // Learned skill IDs
    val variables: MutableMap<String, String> = mutableMapOf(),
    // store entity specific variables: key="type:id:varKey", value="value"
    val entityVariables: MutableMap<String, String> = mutableMapOf(),
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
