package com.novelsim.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 小说模拟器主题颜色
 */

// 主色调 - 深邃的紫蓝色
private val PrimaryLight = Color(0xFF5C6BC0)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFDDE1FF)
private val OnPrimaryContainerLight = Color(0xFF00105C)

// 次要色 - 温暖的琥珀色
private val SecondaryLight = Color(0xFFFF8F00)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFFFE0B2)
private val OnSecondaryContainerLight = Color(0xFF2D1600)

// 第三色 - 柔和的青色
private val TertiaryLight = Color(0xFF00897B)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFB2DFDB)
private val OnTertiaryContainerLight = Color(0xFF00201E)

// 背景和表面
private val BackgroundLight = Color(0xFFFEFBFF)
private val OnBackgroundLight = Color(0xFF1B1B1F)
private val SurfaceLight = Color(0xFFFEFBFF)
private val OnSurfaceLight = Color(0xFF1B1B1F)

// 深色主题颜色
private val PrimaryDark = Color(0xFFB9C3FF)
private val OnPrimaryDark = Color(0xFF243189)
private val PrimaryContainerDark = Color(0xFF3B4BA0)
private val OnPrimaryContainerDark = Color(0xFFDDE1FF)

private val SecondaryDark = Color(0xFFFFB74D)
private val OnSecondaryDark = Color(0xFF4A2800)
private val SecondaryContainerDark = Color(0xFF6A3A00)
private val OnSecondaryContainerDark = Color(0xFFFFE0B2)

private val TertiaryDark = Color(0xFF80CBC4)
private val OnTertiaryDark = Color(0xFF003733)
private val TertiaryContainerDark = Color(0xFF00504A)
private val OnTertiaryContainerDark = Color(0xFFB2DFDB)

private val BackgroundDark = Color(0xFF1B1B1F)
private val OnBackgroundDark = Color(0xFFE4E1E6)
private val SurfaceDark = Color(0xFF1B1B1F)
private val OnSurfaceDark = Color(0xFFE4E1E6)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark
)

/**
 * 小说模拟器主题
 */
@Composable
fun NovelSimulatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
