package com.novelsim.app.presentation.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novelsim.app.data.model.*

@Composable
fun EffectsListEditor(
    effects: List<Effect>,
    clues: List<Clue>,
    factions: List<Faction>,
    characters: List<Character>,
    locations: List<Location> = emptyList(),
    events: List<GameEvent> = emptyList(),
    onEffectsChange: (List<Effect>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "效果列表",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        effects.forEachIndexed { index, effect ->
            EffectItemEditor(
                effect = effect,
                clues = clues,
                factions = factions,
                characters = characters,
                locations = locations,
                events = events,
                onEffectChange = { newEffect ->
                    val newEffects = effects.toMutableList()
                    newEffects[index] = newEffect
                    onEffectsChange(newEffects)
                },
                onDelete = {
                    val newEffects = effects.toMutableList()
                    newEffects.removeAt(index)
                    onEffectsChange(newEffects)
                }
            )
        }

        OutlinedButton(
            onClick = {
                onEffectsChange(effects + Effect.ModifyVariable("var_name", VariableOperation.SET, "0"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加效果")
        }
    }
}

@Composable
private fun EffectItemEditor(
    effect: Effect,
    clues: List<Clue>,
    factions: List<Faction>,
    characters: List<Character>,
    locations: List<Location>,
    events: List<GameEvent>,
    onEffectChange: (Effect) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EffectTypeSelector(effect, onEffectChange)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            when (effect) {
                is Effect.ModifyVariable -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = effect.variableName,
                            onValueChange = { onEffectChange(effect.copy(variableName = it)) },
                            label = { Text("变量名") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = effect.value,
                            onValueChange = { onEffectChange(effect.copy(value = it)) },
                            label = { Text("值") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                is Effect.AddClue -> {
                     ClueSelector(
                        selectedClueId = effect.clueId,
                        clues = clues,
                        onClueSelect = { onEffectChange(effect.copy(clueId = it)) }
                     )
                }
                is Effect.ModifyReputation -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            FactionSelector(
                                selectedFactionId = effect.factionId,
                                factions = factions,
                                onFactionSelect = { onEffectChange(effect.copy(factionId = it)) }
                            )
                        }
                        OutlinedTextField(
                            value = effect.amount.toString(),
                            onValueChange = { onEffectChange(effect.copy(amount = it.toIntOrNull() ?: 0)) },
                            label = { Text("变化量") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )
                    }
                }
                is Effect.ModifyRelationship -> {
                     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CharacterSelector(
                                selectedCharacterId = effect.characterId,
                                characters = characters,
                                onCharacterSelect = { onEffectChange(effect.copy(characterId = it)) }
                            )
                        }
                        OutlinedTextField(
                            value = effect.amount.toString(),
                            onValueChange = { onEffectChange(effect.copy(amount = it.toIntOrNull() ?: 0)) },
                            label = { Text("变化量") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )
                    }
                }
                is Effect.GiveItem -> {
                     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = effect.itemId,
                            onValueChange = { onEffectChange(effect.copy(itemId = it)) },
                            label = { Text("道具ID") },
                            modifier = Modifier.weight(1f),
                             singleLine = true
                        )
                        OutlinedTextField(
                            value = effect.quantity.toString(),
                            onValueChange = { onEffectChange(effect.copy(quantity = it.toIntOrNull() ?: 1)) },
                            label = { Text("数量") },
                            modifier = Modifier.weight(0.5f),
                             singleLine = true
                        )
                    }
                }
                is Effect.MoveToLocation -> {
                    LocationSelector(
                        selectedLocationId = effect.locationId,
                        locations = locations,
                        onLocationSelect = { onEffectChange(effect.copy(locationId = it)) }
                    )
                }
                is Effect.TriggerEvent -> {
                    EventSelector(
                        selectedEventId = effect.eventId,
                        events = events,
                        onEventSelect = { onEffectChange(effect.copy(eventId = it)) }
                    )
                }
                else -> {
                    Text("暂不支持编辑此类型效果", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EffectTypeSelector(currentEffect: Effect, onEffectChange: (Effect) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                when (currentEffect) {
                    is Effect.ModifyVariable -> "修改变量"
                    is Effect.AddClue -> "获得线索"
                    is Effect.ModifyReputation -> "修改声望"
                    is Effect.ModifyRelationship -> "修改好感"
                    is Effect.GiveItem -> "获得道具"
                    is Effect.RemoveItem -> "移除道具"
                    is Effect.ModifyAttribute -> "修改属性"
                    is Effect.PlaySound -> "播放音效"
                    is Effect.MoveToLocation -> "移动地点"
                    is Effect.TriggerEvent -> "触发事件"
                }
            )
        }
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("修改变量") }, onClick = { onEffectChange(Effect.ModifyVariable("", VariableOperation.SET, "")); expanded = false })
            DropdownMenuItem(text = { Text("获得线索") }, onClick = { onEffectChange(Effect.AddClue("")); expanded = false })
            DropdownMenuItem(text = { Text("修改声望") }, onClick = { onEffectChange(Effect.ModifyReputation("", 0)); expanded = false })
            DropdownMenuItem(text = { Text("修改好感") }, onClick = { onEffectChange(Effect.ModifyRelationship("", 0)); expanded = false })
            DropdownMenuItem(text = { Text("获得道具") }, onClick = { onEffectChange(Effect.GiveItem("", 1)); expanded = false })
            DropdownMenuItem(text = { Text("移动地点") }, onClick = { onEffectChange(Effect.MoveToLocation("")); expanded = false })
            DropdownMenuItem(text = { Text("触发事件") }, onClick = { onEffectChange(Effect.TriggerEvent("")); expanded = false })
        }
    }
}

@Composable
private fun ClueSelector(selectedClueId: String, clues: List<Clue>, onClueSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = clues.find { it.id == selectedClueId }?.name ?: selectedClueId.ifEmpty { "选择线索" }
    
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            clues.forEach { clue ->
                DropdownMenuItem(text = { Text(clue.name) }, onClick = { onClueSelect(clue.id); expanded = false })
            }
        }
    }
}

@Composable
private fun FactionSelector(selectedFactionId: String, factions: List<Faction>, onFactionSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = factions.find { it.id == selectedFactionId }?.name ?: selectedFactionId.ifEmpty { "选择阵营" }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            factions.forEach { faction ->
                DropdownMenuItem(text = { Text(faction.name) }, onClick = { onFactionSelect(faction.id); expanded = false })
            }
        }
    }
}

@Composable
private fun CharacterSelector(selectedCharacterId: String, characters: List<Character>, onCharacterSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = characters.find { it.id == selectedCharacterId }?.name ?: selectedCharacterId.ifEmpty { "选择角色" }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            characters.forEach { character ->
                DropdownMenuItem(text = { Text(character.name) }, onClick = { onCharacterSelect(character.id); expanded = false })
            }
        }
    }
}

@Composable
private fun LocationSelector(selectedLocationId: String, locations: List<Location>, onLocationSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = locations.find { it.id == selectedLocationId }?.name ?: selectedLocationId.ifEmpty { "选择地点" }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            locations.forEach { location ->
                DropdownMenuItem(text = { Text(location.name) }, onClick = { onLocationSelect(location.id); expanded = false })
            }
        }
    }
}

@Composable
private fun EventSelector(selectedEventId: String, events: List<GameEvent>, onEventSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = events.find { it.id == selectedEventId }?.name ?: selectedEventId.ifEmpty { "选择事件" }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            events.forEach { event ->
                DropdownMenuItem(text = { Text(event.name) }, onClick = { onEventSelect(event.id); expanded = false })
            }
        }
    }
}
