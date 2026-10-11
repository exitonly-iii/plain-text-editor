package com.example.plaintexteditor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 主题模式：应用支持三种。
 * 第一阶段还没有设置界面，所以默认“跟随系统”；
 * 第二阶段在设置入口里把它改掉，就能实现手动切换深浅色。
 */
enum class ThemeMode {
    /** 跟随系统（手机切深色，应用也跟着变深色） */
    FOLLOW_SYSTEM,

    /** 始终浅色 */
    LIGHT,

    /** 始终深色 */
    DARK,
}

/**
 * 应用主题。所有界面都包在这层里面。
 *
 * @param themeMode 浅色 / 深色 / 跟随系统
 * @param dynamicColor 是否使用“动态取色”（Android 12+ 会跟壁纸配色）。
 *                     默认关闭，这样在荣耀手机上也能保持稳定的 Google 系配色。
 */
@Composable
fun PlainTextEditorTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        // Android 12 及以上，并且用户开启了动态取色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 让状态栏、导航栏上的图标颜色跟着主题走（浅色底用深色图标，反之亦然）
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
