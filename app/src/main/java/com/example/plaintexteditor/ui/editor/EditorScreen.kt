package com.example.plaintexteditor.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.plaintexteditor.PlainTextEditorApp
import com.example.plaintexteditor.ui.browser.FileBrowserScreen
import com.example.plaintexteditor.ui.theme.PlainTextEditorTheme
import com.example.plaintexteditor.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * 主界面。整个应用的界面结构都在这里：
 *
 *   侧边栏（工作区文件树）
 *   └── 主界面
 *       ├── 顶部栏：菜单 / 标题 / 保存 / 查找 / 更多
 *       ├── 编辑区：还没选文件时显示占位；选了文件先显示路径（内容读写留到下一阶段）
 *       └── 底部状态栏：编码 / 换行符 / 行数 / 行号
 *
 * 侧边栏底部“浏览私有目录”会全屏打开文件浏览器页面。
 */
@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = remember(context) { PlainTextEditorApp.from(context) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 设计预览里拿不到 Application，这时只显示占位，不影响真机运行
    if (app == null) {
        NoFileOpenedPlaceholder(modifier = modifier.fillMaxSize())
        return
    }

    val workspaces by app.workspaceStore.workspaces.collectAsState(initial = emptyList())

    // 侧边栏里的文件树要不要重新读一遍磁盘（改了文件之后 +1）
    var treeRefreshKey by remember { mutableStateOf(0) }

    // 当前“交给编辑区”的文件路径（本阶段只记路径，不读内容）
    var selectedFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    // 第一阶段还没有“打开文件内容”的功能，所以固定是 false
    var isFileOpen by rememberSaveable { mutableStateOf(false) }
    var editorText by rememberSaveable { mutableStateOf("") }

    // 是否正在全屏显示“文件浏览器”
    var showFileBrowser by rememberSaveable { mutableStateOf(false) }

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    if (showFileBrowser) {
        // 浏览器在根目录时，系统返回键由这里负责：关掉浏览器回到主界面
        BackHandler { showFileBrowser = false }
        FileBrowserScreen(
            repository = app.fileRepository,
            workspaceStore = app.workspaceStore,
            onClose = {
                showFileBrowser = false
                // 浏览器里可能新建/删除/重命名过文件，回来后让树重新读一遍
                treeRefreshKey++
            },
            modifier = modifier,
        )
        return
    }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            WorkspaceDrawer(
                workspaces = workspaces,
                repository = app.fileRepository,
                workspaceStore = app.workspaceStore,
                refreshKey = treeRefreshKey,
                onBrowsePrivateDirectoryClick = {
                    // 先收起侧边栏，再全屏打开文件浏览器
                    scope.launch { drawerState.close() }
                    showFileBrowser = true
                },
                onOpenSettingsClick = {
                    // TODO(后续阶段)：打开设置页（主题切换等）
                },
                onFileClick = { item ->
                    // 点文件：关掉侧边栏，把路径交给编辑区
                    selectedFilePath = item.path
                    scope.launch { drawerState.close() }
                },
                onMessage = showMessage,
                onTreeChanged = { treeRefreshKey++ },
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
                        // 占位：后续阶段实现保存
                    },
                    onFindClick = {
                        // 占位：后续阶段实现查找
                    },
                    onMoreClick = {
                        // 占位：后续阶段实现更多菜单
                    },
                )
            },
            bottomBar = {
                EditorStatusBar()
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when {
                    isFileOpen -> PlainTextEditor(
                        text = editorText,
                        onTextChange = { editorText = it },
                        modifier = Modifier.fillMaxSize(),
                    )

                    selectedFilePath != null -> SelectedFilePlaceholder(
                        path = selectedFilePath.orEmpty(),
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> NoFileOpenedPlaceholder(modifier = Modifier.fillMaxSize())
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
