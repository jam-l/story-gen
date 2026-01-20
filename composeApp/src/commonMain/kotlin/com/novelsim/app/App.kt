package com.novelsim.app

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.novelsim.app.presentation.home.HomeScreen
import com.novelsim.app.presentation.theme.NovelSimulatorTheme
import org.koin.compose.KoinApplication
import com.novelsim.app.di.appModule

import org.koin.core.module.Module

/**
 * 应用主入口 Composable
 * 使用 Koin 进行依赖注入，Voyager 进行导航
 */
@Composable
fun App(
    platformModules: List<Module> = emptyList()
) {
    KoinApplication(application = {
        modules(listOf(appModule) + platformModules)
    }) {
        NovelSimulatorTheme {
            Navigator(HomeScreen())
        }
    }
}
