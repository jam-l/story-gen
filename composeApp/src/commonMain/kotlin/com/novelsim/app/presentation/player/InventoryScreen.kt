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
    equipment: Equipment,
    items: List<Item>,
    gold: Int,
    onUseItem: (Item) -> Unit,
    onEquipItem: (Item) -> Unit,
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
                    items = items,
                    onUseItem = onUseItem,
                    onEquipItem = onEquipItem
                )
                1 -> EquipmentPanel(
                    equipment = equipment,
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
    items: List<Item>,
    onUseItem: (Item) -> Unit,
    onEquipItem: (Item) -> Unit
) {
    var selectedItem by remember { mutableStateOf<Item?>(null) }
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
                val item = items.find { it.id == slot.itemId }
                if (item != null) {
                    ItemSlot(
                        item = item,
                        quantity = slot.quantity,
                        isSelected = selectedItem?.id == item.id,
                        onClick = {
                            selectedItem = item
                            selectedSlot = slot
                        }
                    )
                }
            }
        }
    }
    
    // 道具详情对话框
    selectedItem?.let { item ->
        ItemDetailDialog(
            item = item,
            quantity = selectedSlot?.quantity ?: 0,
            onUse = {
                onUseItem(item)
                selectedItem = null
            },
            onEquip = {
                onEquipItem(item)
                selectedItem = null
            },
            onDismiss = { selectedItem = null }
        )
    }
}

@Composable
private fun ItemSlot(
    item: Item,
    quantity: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (item.type) {
        ItemType.CONSUMABLE -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        ItemType.EQUIPMENT -> Color(0xFF2196F3).copy(alpha = 0.2f)
        ItemType.KEY_ITEM -> Color(0xFFFF9800).copy(alpha = 0.2f)
        ItemType.MATERIAL -> Color(0xFF9E9E9E).copy(alpha = 0.2f)
    }
    
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
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
                    imageVector = when (item.type) {
                        ItemType.CONSUMABLE -> Icons.Default.Favorite
                        ItemType.EQUIPMENT -> Icons.Default.Build
                        ItemType.KEY_ITEM -> Icons.Default.Lock
                        ItemType.MATERIAL -> Icons.Default.Settings
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
            
            // 数量标签
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
    item: Item,
    quantity: Int,
    onUse: () -> Unit,
    onEquip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "x$quantity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                Text(item.description)
                Spacer(modifier = Modifier.height(8.dp))
                
                // 显示效果
                when (val effect = item.effect) {
                    is ItemEffect.Heal -> {
                        if (effect.hp > 0) Text("恢复 HP: +${effect.hp}")
                        if (effect.mp > 0) Text("恢复 MP: +${effect.mp}")
                    }
                    is ItemEffect.EquipmentBonus -> {
                        Text("装备位置: ${effect.slot.name}")
                        if (effect.attackBonus != 0) Text("攻击力: +${effect.attackBonus}")
                        if (effect.defenseBonus != 0) Text("防御力: +${effect.defenseBonus}")
                        if (effect.hpBonus != 0) Text("HP: +${effect.hpBonus}")
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (item.type) {
                ItemType.CONSUMABLE -> {
                    Button(onClick = onUse) {
                        Text("使用")
                    }
                }
                ItemType.EQUIPMENT -> {
                    Button(onClick = onEquip) {
                        Text("装备")
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun EquipmentPanel(
    equipment: Equipment,
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
        items(slots) { (slot, itemId) ->
            val item = items.find { it.id == itemId }
            EquipmentSlotCard(
                slot = slot,
                item = item,
                onUnequip = { onUnequip(slot) }
            )
        }
    }
}

@Composable
private fun EquipmentSlotCard(
    slot: EquipSlot,
    item: Item?,
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
                        if (item != null) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slotIcon,
                    contentDescription = null,
                    tint = if (item != null) 
                        MaterialTheme.colorScheme.onPrimaryContainer
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
                    text = item?.name ?: "空",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item != null) FontWeight.Bold else FontWeight.Normal
                )
                
                // 显示装备加成
                if (item != null) {
                    val effect = item.effect as? ItemEffect.EquipmentBonus
                    effect?.let {
                        val bonuses = mutableListOf<String>()
                        if (it.attackBonus != 0) bonuses.add("攻+${it.attackBonus}")
                        if (it.defenseBonus != 0) bonuses.add("防+${it.defenseBonus}")
                        if (it.hpBonus != 0) bonuses.add("HP+${it.hpBonus}")
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
            if (item != null) {
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
