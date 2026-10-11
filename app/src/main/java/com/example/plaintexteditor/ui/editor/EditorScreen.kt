package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.plaintexteditor.ui.theme.PlainTextEditorTheme
import com.example.plaintexteditor.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * 主界面。整个应用的界面结构都在这里：
 *
 *   侧边栏（工作区）
 *   └── 主界面
 *       ├── 顶部栏：菜单 / 标题 / 保存 / 查找 / 更多
 *       ├── 编辑区：占位文字，或（第二阶段）真正的编辑框
 *       └── 底部状态栏：编码 / 换行符 / 行数 / 行号
 */
@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 第一阶段还没有“打开文件”的功能，所以固定是 false，编辑区显示“未打开文件”。
    // 第二阶段接入文件之后，只要把它改成 true，编辑区就会显示真正的文本编辑框。
    var isFileOpen by rememberSaveable { mutableStateOf(false) }
    var editorText by rememberSaveable { mutableStateOf("") }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            WorkspaceDrawer(
                onBrowsePrivateDirectoryClick = {
                    // TODO(第二阶段)：打开应用私有目录的文件浏览界面
                },
                onOpenSettingsClick = {
                    // TODO(第二阶段)：打开设置页（主题切换等）
                },
            )
        },
    ) {
        Scaffold(
            // imePadding：键盘弹出时把内容整体顶上去，底部状态栏不会被挡住
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                EditorTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSaveClick = {
                        // 占位：第二阶段实现保存
                    },
                    onFindClick = {
                        // 占位：第二阶段实现查找
                    },
                    onMoreClick = {
                        // 占位：第二阶段实现更多菜单
                    },
                )
            },
            bottomBar = {
                EditorStatusBar()
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isFileOpen) {
                    PlainTextEditor(
                        text = editorText,
                        onTextChange = { editorText = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    NoFileOpenedPlaceholder(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Preview(name = "主界面 - 浅色", showBackground = true)
@Composable
private fun EditorScreenLightPreview() {
    PlainTextEditorTheme(themeMode = ThemeMode.LIGHT) {
        EditorScreen()
    }
}

@Preview(name = "主界面 - 深色", showBackground = true)
@Composable
private fun EditorScreenDarkPreview() {
    PlainTextEditorTheme(themeMode = ThemeMode.DARK) {
        EditorScreen()
    }
}
