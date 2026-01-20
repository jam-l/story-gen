package com.novelsim.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.novelsim.app.data.local.DatabaseDriverFactory
import org.koin.dsl.module

/**
 * Android 主入口 Activity
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val androidModule = module {
            single { DatabaseDriverFactory(applicationContext) }
        }
        
        enableEdgeToEdge()
        setContent {
            App(platformModules = listOf(androidModule))
        }
    }
}
