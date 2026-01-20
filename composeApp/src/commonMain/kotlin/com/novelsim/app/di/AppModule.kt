package com.novelsim.app.di

import com.novelsim.app.domain.engine.StoryEngine
import com.novelsim.app.domain.rpg.BattleSystem
import com.novelsim.app.domain.rpg.InventoryManager
import com.novelsim.app.data.repository.StoryRepository
import com.novelsim.app.data.repository.SaveRepository
import com.novelsim.app.presentation.home.HomeScreenModel
import com.novelsim.app.presentation.player.StoryPlayerScreenModel
import com.novelsim.app.presentation.editor.EditorScreenModel
import org.koin.dsl.module

/**
 * Koin 依赖注入模块
 */
val appModule = module {
    // 仓库
    single { StoryRepository(get()) }
    single { SaveRepository() }
    
    // 领域层
    factory { StoryEngine(get()) }
    factory { BattleSystem() }
    factory { InventoryManager() }
    
    // ScreenModel (ViewModel)
    factory { HomeScreenModel(get(), get()) }
    factory { params -> StoryPlayerScreenModel(params.get(), get(), get()) }
    factory { params -> EditorScreenModel(params.getOrNull(), get()) }
}
