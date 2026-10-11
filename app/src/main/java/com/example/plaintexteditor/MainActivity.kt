package com.example.plaintexteditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.plaintexteditor.ui.editor.EditorScreen
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
            // TODO(第二阶段)：在“设置”里给 themeMode 提供切换入口
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.FOLLOW_SYSTEM) }

            PlainTextEditorTheme(themeMode = themeMode) {
                EditorScreen()
            }
        }
    }
}
