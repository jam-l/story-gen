package com.novelsim.app.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.*

/**
 * 背包界面
 */
@Composable
fun InventoryScreen(
    inventory: List<InventorySlot>,
    itemInstances: Map<String, ItemInstance> = emptyMap(),
    equipment: Equipment,
    items: List<Item>,
    gold: Int,
    onUseItem: (InventorySlot) -> Unit,
    onEquipItem: (InventorySlot) -> Unit,
    onUnequipItem: (EquipSlot) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("背包", "装备")
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // 顶部栏
            InventoryTopBar(gold = gold, onDismiss = onDismiss)
            
            // 标签页
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // 内容
            when (selectedTab) {
                0 -> InventoryGrid(
                    inventory = inventory,
                    itemInstances = itemInstances,
                    items = items,
                    onUseItem = onUseItem,
                    onEquipItem = onEquipItem
                )
                1 -> EquipmentPanel(
                    equipment = equipment,
                    itemInstances = itemInstances,
                    items = items,
                    onUnequip = onUnequipItem
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTopBar(
    gold: Int,
    onDismiss: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("背包", fontWeight = FontWeight.Bold)
            }
        },
        actions = {
            // 金币显示
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$gold",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }
    )
}

@Composable
private fun InventoryGrid(
    inventory: List<InventorySlot>,
    itemInstances: Map<String, ItemInstance>,
    items: List<Item>,
    onUseItem: (InventorySlot) -> Unit,
    onEquipItem: (InventorySlot) -> Unit
) {
    var selectedSlot by remember { mutableStateOf<InventorySlot?>(null) }
    
    if (inventory.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "背包是空的",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(inventory) { slot ->
                // 解析显示数据
                val instance = itemInstances[slot.instanceId]
                // 如果是实例，itemId是模板ID；如果是普通物品，itemId也是模板ID
                val templateId = instance?.templateId ?: slot.itemId
                val template = items.find { it.id == templateId }
                
                if (template != null) {
                    val displayName = instance?.name ?: template.name
                    val rarity = instance?.rarity ?: ItemRarity.COMMON
                    
                    ItemSlot(
                        name = displayName,
                        icon = when (template.type) {
                            ItemType.CONSUMABLE -> Icons.Default.Favorite
                            ItemType.EQUIPMENT -> Icons.Default.Build
                            ItemType.KEY_ITEM -> Icons.Default.Lock
                            ItemType.MATERIAL -> Icons.Default.Settings
                        },
                        type = template.type,
                        rarity = rarity,
                        quantity = slot.quantity,
                        isSelected = selectedSlot == slot,
                        onClick = { selectedSlot = slot }
                    )
                }
            }
        }
    }
    
    // 道具详情对话框
    selectedSlot?.let { slot ->
        val instance = itemInstances[slot.instanceId]
        val templateId = instance?.templateId ?: slot.itemId
        val template = items.find { it.id == templateId }
        
        if (template != null) {
            ItemDetailDialog(
                template = template,
                instance = instance,
                quantity = slot.quantity,
                onUse = {
                    onUseItem(slot)
                    selectedSlot = null
                },
                onEquip = {
                    onEquipItem(slot)
                    selectedSlot = null
                },
                onDismiss = { selectedSlot = null }
            )
        }
    }
}

@Composable
private fun ItemSlot(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    type: ItemType,
    rarity: ItemRarity,
    quantity: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val rarityColor = getRarityColor(rarity)
    val backgroundColor = rarityColor.copy(alpha = 0.1f)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else rarityColor.copy(alpha = 0.5f)
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = getRarityColor(rarity) // 图标使用稀有度颜色
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // Quantity label ... same
            if (quantity > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "x$quantity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemDetailDialog(
    template: Item,
    instance: ItemInstance?,
    quantity: Int,
    onUse: () -> Unit,
    onEquip: () -> Unit,
    onDismiss: () -> Unit
) {
    val displayName = instance?.name ?: template.name
    val displayDesc = instance?.let { "Lv.${it.level} ${it.rarity.name}" } ?: template.description
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName)
                Spacer(modifier = Modifier.width(8.dp))
                // 稀有度标签
                if (instance != null) {
                    Surface(
                        color = getRarityColor(instance.rarity),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = instance.rarity.name.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Text(template.description) // 始终显示基础描述
                if (instance != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("等级: ${instance.level}", style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 显示效果 (合并基础效果 + 实例加成)
                when (val effect = template.effect) {
                    is ItemEffect.Heal -> {
                        if (effect.hp > 0) Text("恢复 HP: +${effect.hp}")
                        if (effect.mp > 0) Text("恢复 MP: +${effect.mp}")
                    }
                    is ItemEffect.EquipmentBonus -> {
                        Text("装备位置: ${effect.slot.name}")
                        
                        // 计算总属性
                        val atk = effect.attackBonus + (instance?.bonusAttack ?: 0)
                        val def = effect.defenseBonus + (instance?.bonusDefense ?: 0)
                        val hp = effect.hpBonus + (instance?.bonusHp ?: 0)
                        
                        if (atk != 0) Text("攻击力: +$atk ${if((instance?.bonusAttack ?: 0) > 0) "(强化 +${instance?.bonusAttack})" else ""}")
                        if (def != 0) Text("防御力: +$def ${if((instance?.bonusDefense ?: 0) > 0) "(强化 +${instance?.bonusDefense})" else ""}")
                        if (hp != 0)  Text("HP上限: +$hp")
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (template.type) {
                ItemType.CONSUMABLE -> {
                    Button(onClick = onUse) { Text("使用") }
                }
                ItemType.EQUIPMENT -> {
                    Button(onClick = onEquip) { Text("装备") }
                }
                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private fun getRarityColor(rarity: ItemRarity): Color {
    return when (rarity) {
        ItemRarity.COMMON -> Color(0xFF9E9E9E)     // Grey
        ItemRarity.UNCOMMON -> Color(0xFF4CAF50)   // Green
        ItemRarity.RARE -> Color(0xFF2196F3)       // Blue
        ItemRarity.EPIC -> Color(0xFF9C27B0)       // Purple
        ItemRarity.LEGENDARY -> Color(0xFFFF9800)  // Orange
        ItemRarity.MYTHIC -> Color(0xFFFF5252)     // Red
    }
}

@Composable
private fun EquipmentPanel(
    equipment: Equipment,
    itemInstances: Map<String, ItemInstance>,
    items: List<Item>,
    onUnequip: (EquipSlot) -> Unit
) {
    val slots = listOf(
        EquipSlot.WEAPON to equipment.weapon,
        EquipSlot.ARMOR to equipment.armor,
        EquipSlot.HEAD to equipment.head,
        EquipSlot.ACCESSORY to equipment.accessory,
        EquipSlot.BOOTS to equipment.boots
    )
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(slots) { (slot, instanceId) ->
            // instanceId might be a template ID (old) or an instance ID (new)
            val instance = itemInstances[instanceId]
            val templateId = instance?.templateId ?: instanceId
            val item = items.find { it.id == templateId }
            
            EquipmentSlotCard(
                slot = slot,
                template = item,
                instance = instance,
                onUnequip = { onUnequip(slot) }
            )
        }
    }
}

@Composable
private fun EquipmentSlotCard(
    slot: EquipSlot,
    template: Item?,
    instance: ItemInstance?,
    onUnequip: () -> Unit
) {
    val slotName = when (slot) {
        EquipSlot.WEAPON -> "武器"
        EquipSlot.ARMOR -> "护甲"
        EquipSlot.HEAD -> "头盔"
        EquipSlot.ACCESSORY -> "饰品"
        EquipSlot.BOOTS -> "鞋子"
    }
    
    val slotIcon = when (slot) {
        EquipSlot.WEAPON -> Icons.Default.Star
        EquipSlot.ARMOR -> Icons.Default.Build
        EquipSlot.HEAD -> Icons.Default.Person
        EquipSlot.ACCESSORY -> Icons.Default.Favorite
        EquipSlot.BOOTS -> Icons.Default.PlayArrow
    }
    
    val displayName = instance?.name ?: template?.name ?: "空"
    val isEquipped = template != null
    val rarity = instance?.rarity ?: ItemRarity.COMMON
    val borderColor = if (isEquipped) getRarityColor(rarity).copy(alpha = 0.5f) else Color.Transparent
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = if (isEquipped) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 槽位图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isEquipped) getRarityColor(rarity).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slotIcon,
                    contentDescription = null,
                    tint = if (isEquipped) 
                        getRarityColor(rarity)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slotName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isEquipped) FontWeight.Bold else FontWeight.Normal,
                    color = if (isEquipped) getRarityColor(rarity) else Color.Unspecified
                )
                
                // 显示装备加成
                if (template != null) {
                    val effect = template.effect as? ItemEffect.EquipmentBonus
                    effect?.let {
                        val bonuses = mutableListOf<String>()
                        
                        // 合并属性
                        val atk = it.attackBonus + (instance?.bonusAttack ?: 0)
                        val def = it.defenseBonus + (instance?.bonusDefense ?: 0)
                        val hp = it.hpBonus + (instance?.bonusHp ?: 0)
                        
                        if (atk != 0) bonuses.add("攻+${atk}")
                        if (def != 0) bonuses.add("防+${def}")
                        if (hp != 0) bonuses.add("HP+${hp}")
                        
                        if (bonuses.isNotEmpty()) {
                            Text(
                                text = bonuses.joinToString(" "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // 卸下按钮
            if (isEquipped) {
                IconButton(onClick = onUnequip) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "卸下",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
