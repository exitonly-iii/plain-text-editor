package com.example.plaintexteditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.plaintexteditor.ui.editor.EditorScreen
import com.example.plaintexteditor.data.settings.SettingsStore
import com.example.plaintexteditor.ui.theme.PlainTextEditorTheme
import com.example.plaintexteditor.ui.theme.ThemeMode

/**
 * 应用唯一的 Activity（可以理解为“整个应用的窗口”）。
 * 它只做三件事：铺满全屏、套上主题、显示编辑器主界面。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 让内容延伸到状态栏/导航栏底下（现代 Android 的“边到边”显示）
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val app = remember(context) { PlainTextEditorApp.from(context) }
            if (app == null) {
                // 设计预览里没有 Application，用默认主题
                PlainTextEditorTheme { EditorScreen() }
            } else {
                // 主题来自设置页，改完立刻生效
                val storedName by app.settingsStore.themeModeName.collectAsState(
                    initial = SettingsStore.THEME_FOLLOW_SYSTEM,
                )
                val themeMode = remember(storedName) {
                    runCatching { ThemeMode.valueOf(storedName) }
                        .getOrDefault(ThemeMode.FOLLOW_SYSTEM)
                }
                PlainTextEditorTheme(themeMode = themeMode) {
                    EditorScreen()
                }
            }
        }
    }
}
