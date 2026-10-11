package com.example.plaintexteditor.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 浅色主题配色。
 * 主色用 Google 蓝，背景是极浅的冷灰 —— 整体接近 Google 系 App 的感觉。
 */
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),

    secondary = Color(0xFF5F6368),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F0FE),
    onSecondaryContainer = Color(0xFF001D35),

    tertiary = Color(0xFF146C2E),
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFF8FAFD),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFDFDFD),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE1E3E6),
    onSurfaceVariant = Color(0xFF444746),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C7C5),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

/**
 * 深色主题配色。
 * 用的是 Google 深色模式下那套偏亮的蓝色，长时间看代码/文字不刺眼。
 */
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),

    secondary = Color(0xFFC4C7C5),
    onSecondary = Color(0xFF2F3033),
    secondaryContainer = Color(0xFF444746),
    onSecondaryContainer = Color(0xFFE3E3E3),

    tertiary = Color(0xFF6DD58C),
    onTertiary = Color(0xFF0A3818),

    background = Color(0xFF101418),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF1B1F23),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF444746),
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF8E918F),
    outlineVariant = Color(0xFF444746),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)
