package com.novelsim.app.domain.rpg

import com.novelsim.app.data.model.*

/**
 * 背包管理器 - 管理玩家道具和装备
 */
class InventoryManager {
    
    /**
     * 添加道具到背包
     */
    fun addItem(
        inventory: List<InventorySlot>,
        itemId: String,
        quantity: Int,
        maxStack: Int = 99
    ): List<InventorySlot> {
        val mutableInventory = inventory.toMutableList()
        val existingSlot = mutableInventory.find { it.itemId == itemId }
        
        if (existingSlot != null) {
            val index = mutableInventory.indexOf(existingSlot)
            val newQuantity = (existingSlot.quantity + quantity).coerceAtMost(maxStack)
            mutableInventory[index] = existingSlot.copy(quantity = newQuantity)
        } else {
            mutableInventory.add(InventorySlot(itemId, quantity.coerceAtMost(maxStack)))
        }
        
        return mutableInventory
    }
    
    /**
     * 从背包移除道具
     */
    fun removeItem(
        inventory: List<InventorySlot>,
        itemId: String,
        quantity: Int
    ): Pair<List<InventorySlot>, Boolean> {
        val mutableInventory = inventory.toMutableList()
        val existingSlot = mutableInventory.find { it.itemId == itemId }
            ?: return Pair(inventory, false)
        
        if (existingSlot.quantity < quantity) {
            return Pair(inventory, false)
        }
        
        val newQuantity = existingSlot.quantity - quantity
        if (newQuantity <= 0) {
            mutableInventory.remove(existingSlot)
        } else {
            val index = mutableInventory.indexOf(existingSlot)
            mutableInventory[index] = existingSlot.copy(quantity = newQuantity)
        }
        
        return Pair(mutableInventory, true)
    }
    
    /**
     * 检查是否拥有道具
     */
    fun hasItem(inventory: List<InventorySlot>, itemId: String, quantity: Int = 1): Boolean {
        val slot = inventory.find { it.itemId == itemId } ?: return false
        return slot.quantity >= quantity
    }
    
    /**
     * 获取道具数量
     */
    fun getItemCount(inventory: List<InventorySlot>, itemId: String): Int {
        return inventory.find { it.itemId == itemId }?.quantity ?: 0
    }
    
    /**
     * 使用消耗品
     */
    fun useConsumable(
        stats: CharacterStats,
        inventory: List<InventorySlot>,
        item: Item
    ): Triple<CharacterStats, List<InventorySlot>, Boolean> {
        if (item.type != ItemType.CONSUMABLE) {
            return Triple(stats, inventory, false)
        }
        
        // 检查是否拥有道具
        if (!hasItem(inventory, item.id)) {
            return Triple(stats, inventory, false)
        }
        
        // 应用效果
        var newStats = stats
        when (val effect = item.effect) {
            is ItemEffect.Heal -> {
                newStats = stats.copy(
                    currentHp = (stats.currentHp + effect.hp).coerceAtMost(stats.maxHp),
                    currentMp = (stats.currentMp + effect.mp).coerceAtMost(stats.maxMp)
                )
            }
            is ItemEffect.Buff -> {
                // Buff 效果需要战斗系统支持，这里简化处理
                when (effect.attribute.lowercase()) {
                    "attack" -> newStats = stats.copy(attack = stats.attack + effect.value)
                    "defense" -> newStats = stats.copy(defense = stats.defense + effect.value)
                    "speed" -> newStats = stats.copy(speed = stats.speed + effect.value)
                }
            }
            else -> {}
        }
        
        // 移除使用的道具
        val (newInventory, _) = removeItem(inventory, item.id, 1)
        
        return Triple(newStats, newInventory, true)
    }
    
    /**
     * 装备道具
     */
    fun equipItem(
        equipment: Equipment,
        inventory: List<InventorySlot>,
        item: Item,
        items: List<Item>
    ): Triple<Equipment, List<InventorySlot>, Boolean> {
        if (item.type != ItemType.EQUIPMENT) {
            return Triple(equipment, inventory, false)
        }
        
        val effect = item.effect as? ItemEffect.EquipmentBonus
            ?: return Triple(equipment, inventory, false)
        
        // 检查是否拥有道具
        if (!hasItem(inventory, item.id)) {
            return Triple(equipment, inventory, false)
        }
        
        // 获取当前slot 装备的道具
        val currentEquippedId = when (effect.slot) {
            EquipSlot.WEAPON -> equipment.weapon
            EquipSlot.ARMOR -> equipment.armor
            EquipSlot.ACCESSORY -> equipment.accessory
            EquipSlot.HEAD -> equipment.head
            EquipSlot.BOOTS -> equipment.boots
        }
        
        // 更新装备
        val newEquipment = when (effect.slot) {
            EquipSlot.WEAPON -> equipment.copy(weapon = item.id)
            EquipSlot.ARMOR -> equipment.copy(armor = item.id)
            EquipSlot.ACCESSORY -> equipment.copy(accessory = item.id)
            EquipSlot.HEAD -> equipment.copy(head = item.id)
            EquipSlot.BOOTS -> equipment.copy(boots = item.id)
        }
        
        // 更新背包：移除新装备，放回旧装备
        var newInventory = removeItem(inventory, item.id, 1).first
        if (currentEquippedId != null) {
            newInventory = addItem(newInventory, currentEquippedId, 1)
        }
        
        return Triple(newEquipment, newInventory, true)
    }
    
    /**
     * 卸下装备
     */
    fun unequipItem(
        equipment: Equipment,
        inventory: List<InventorySlot>,
        slot: EquipSlot
    ): Pair<Equipment, List<InventorySlot>> {
        val equippedItemId = when (slot) {
            EquipSlot.WEAPON -> equipment.weapon
            EquipSlot.ARMOR -> equipment.armor
            EquipSlot.ACCESSORY -> equipment.accessory
            EquipSlot.HEAD -> equipment.head
            EquipSlot.BOOTS -> equipment.boots
        } ?: return Pair(equipment, inventory)
        
        val newEquipment = when (slot) {
            EquipSlot.WEAPON -> equipment.copy(weapon = null)
            EquipSlot.ARMOR -> equipment.copy(armor = null)
            EquipSlot.ACCESSORY -> equipment.copy(accessory = null)
            EquipSlot.HEAD -> equipment.copy(head = null)
            EquipSlot.BOOTS -> equipment.copy(boots = null)
        }
        
        val newInventory = addItem(inventory, equippedItemId, 1)
        
        return Pair(newEquipment, newInventory)
    }
    
    /**
     * 计算装备加成后的属性
     */
    fun calculateStatsWithEquipment(
        baseStats: CharacterStats,
        equipment: Equipment,
        items: List<Item>
    ): CharacterStats {
        var stats = baseStats
        
        val equippedIds = listOfNotNull(
            equipment.weapon,
            equipment.armor,
            equipment.accessory,
            equipment.head,
            equipment.boots
        )
        
        for (itemId in equippedIds) {
            val item = items.find { it.id == itemId } ?: continue
            val effect = item.effect as? ItemEffect.EquipmentBonus ?: continue
            
            stats = stats.copy(
                maxHp = stats.maxHp + effect.hpBonus,
                currentHp = stats.currentHp + effect.hpBonus,
                maxMp = stats.maxMp + effect.mpBonus,
                currentMp = stats.currentMp + effect.mpBonus,
                attack = stats.attack + effect.attackBonus,
                defense = stats.defense + effect.defenseBonus,
                speed = stats.speed + effect.speedBonus
            )
        }
        
        return stats
    }
}
