package com.novelsim.app.presentation.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.novelsim.app.data.model.Story
import com.novelsim.app.data.model.SaveData
import com.novelsim.app.data.repository.StoryRepository
import com.novelsim.app.data.repository.SaveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主页 ScreenModel
 */
class HomeScreenModel(
    private val storyRepository: StoryRepository,
    private val saveRepository: SaveRepository
) : ScreenModel {
    
    /**
     * UI 状态
     */
    data class UiState(
        val stories: List<Story> = emptyList(),
        val saves: List<SaveData> = emptyList(),
        val isLoading: Boolean = true,
        val selectedTab: Tab = Tab.STORIES,
        val error: String? = null
    )
    
    /**
     * 标签页
     */
    enum class Tab {
        STORIES,    // 故事列表
        SAVES,      // 存档列表
        EDITOR      // 编辑器
    }
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    /**
     * 加载数据
     */
    private fun loadData() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val stories = storyRepository.getAllStories()
                val saves = saveRepository.getAllSaves()
                _uiState.value = _uiState.value.copy(
                    stories = stories,
                    saves = saves,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * 切换标签页
     */
    fun selectTab(tab: Tab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        loadData()
    }
    
    /**
     * 删除故事
     */
    fun deleteStory(storyId: String) {
        screenModelScope.launch {
            storyRepository.deleteStory(storyId)
            loadData()
        }
    }
    
    /**
     * 删除存档
     */
    fun deleteSave(saveId: String) {
        screenModelScope.launch {
            saveRepository.deleteSave(saveId)
            loadData()
        }
    }
    
    /**
     * 添加随机生成的故事
     */
    fun addRandomStory(story: Story) {
        screenModelScope.launch {
            storyRepository.saveStory(story)
            loadData()
        }
    }
}
