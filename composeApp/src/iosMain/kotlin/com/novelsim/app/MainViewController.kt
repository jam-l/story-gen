package com.novelsim.app

import androidx.compose.ui.window.ComposeUIViewController
import com.novelsim.app.data.local.DatabaseDriverFactory
import org.koin.dsl.module

/**
 * iOS 平台入口
 */
fun MainViewController() = ComposeUIViewController { 
    val iosModule = module {
        single { DatabaseDriverFactory() }
    }
    App(platformModules = listOf(iosModule))
}
