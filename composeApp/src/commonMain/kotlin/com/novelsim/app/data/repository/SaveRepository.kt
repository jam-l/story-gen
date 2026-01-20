package com.novelsim.app.data.repository

import com.novelsim.app.data.model.SaveData
import com.novelsim.app.data.model.GameState
import com.novelsim.app.util.PlatformUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 存档仓库 - 管理游戏存档的存储和读取
 */
class SaveRepository {
    
    companion object {
        /** 最大存档槽位数 */
        const val MAX_SAVE_SLOTS = 10
    }
    
    // 内存中的存档列表（后续会替换为 SQLDelight）
    private val _saves = MutableStateFlow<List<SaveData>>(emptyList())
    val saves: Flow<List<SaveData>> = _saves.asStateFlow()
    
    /**
     * 获取所有存档
     */
    suspend fun getAllSaves(): List<SaveData> {
        return _saves.value
    }
    
    /**
     * 获取指定故事的存档
     */
    suspend fun getSavesForStory(storyId: String): List<SaveData> {
        return _saves.value.filter { it.storyId == storyId }
    }
    
    /**
     * 根据 ID 获取存档
     */
    suspend fun getSaveById(saveId: String): SaveData? {
        return _saves.value.find { it.id == saveId }
    }
    
    /**
     * 根据槽位获取存档
     */
    suspend fun getSaveBySlot(slotIndex: Int): SaveData? {
        return _saves.value.find { it.slotIndex == slotIndex }
    }
    
    /**
     * 保存游戏
     */
    suspend fun saveGame(
        slotIndex: Int,
        storyId: String,
        storyTitle: String,
        gameState: GameState,
        currentNodePreview: String? = null
    ): SaveData {
        val saveData = SaveData(
            id = "save_${slotIndex}_${PlatformUtils.getCurrentTimeMillis()}",
            slotIndex = slotIndex,
            storyId = storyId,
            storyTitle = storyTitle,
            gameState = gameState,
            timestamp = PlatformUtils.getCurrentTimeMillis(),
            playTime = gameState.playTime,
            currentNodePreview = currentNodePreview
        )
        
        val currentList = _saves.value.toMutableList()
        // 移除同槽位的旧存档
        currentList.removeAll { it.slotIndex == slotIndex }
        currentList.add(saveData)
        _saves.value = currentList.sortedBy { it.slotIndex }
        
        return saveData
    }
    
    /**
     * 删除存档
     */
    suspend fun deleteSave(saveId: String) {
        _saves.value = _saves.value.filter { it.id != saveId }
    }
    
    /**
     * 删除指定槽位的存档
     */
    suspend fun deleteSaveBySlot(slotIndex: Int) {
        _saves.value = _saves.value.filter { it.slotIndex != slotIndex }
    }
    
    /**
     * 获取可用的存档槽位
     */
    suspend fun getAvailableSlots(): List<Int> {
        val usedSlots = _saves.value.map { it.slotIndex }.toSet()
        return (0 until MAX_SAVE_SLOTS).filter { it !in usedSlots }
    }
    
    /**
     * 自动存档
     */
    suspend fun autoSave(
        storyId: String,
        storyTitle: String,
        gameState: GameState,
        currentNodePreview: String? = null
    ): SaveData {
        // 自动存档使用槽位 0
        return saveGame(0, storyId, storyTitle, gameState, currentNodePreview)
    }
}
