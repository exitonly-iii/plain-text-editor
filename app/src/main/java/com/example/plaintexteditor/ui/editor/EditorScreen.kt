package com.example.plaintexteditor.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.PlainTextEditorApp
import com.example.plaintexteditor.R
import com.example.plaintexteditor.ui.browser.FileBrowserScreen
import com.example.plaintexteditor.ui.theme.PlainTextEditorTheme
import com.example.plaintexteditor.ui.theme.ThemeMode
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * 主界面，也是“编辑区”的状态大本营：
 *
 * - 当前打开的文件路径、当前文本、打开时的文本（用来判断“有没有改动”）
 * - 打开文件 / 保存文件 / 切换文件时提示保存
 * - 侧边栏（工作区文件树）和文件浏览器里点文件，都会回到这里打开
 */
@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = remember(context) { PlainTextEditorApp.from(context) }

    // 设计预览里拿不到 Application，这时只显示占位，不影响真机运行
    if (app == null) {
        NoFileOpenedPlaceholder(modifier = modifier.fillMaxSize())
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val workspaces by app.workspaceStore.workspaces.collectAsState(initial = emptyList())

    // 侧边栏里的文件树要不要重新读一遍磁盘（改了文件之后 +1）
    var treeRefreshKey by remember { mutableStateOf(0) }
    // 是否正在全屏显示“文件浏览器”
    var showFileBrowser by rememberSaveable { mutableStateOf(false) }

    // ---------------- 编辑区状态 ----------------
    /** 当前打开的文件（相对私有根目录的路径）；null = 还没打开文件 */
    var openFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    /** 编辑区里现在的文本 */
    var editorText by rememberSaveable { mutableStateOf("") }
    /** 打开时（或上次保存时）的文本，用来判断有没有改动 */
    var savedText by rememberSaveable { mutableStateOf("") }
    var isLoadingFile by remember { mutableStateOf(false) }
    /** 有未保存修改时，用户想打开的新文件先放这里，等他在弹窗里选“保存”或“放弃” */
    var pendingOpenPath by remember { mutableStateOf<String?>(null) }

    val isModified = openFilePath != null && editorText != savedText
    val fileName = openFilePath?.substringAfterLast('/')

    // 这些文字要在协程里用，所以先取出来（协程里不能调用 stringResource）
    val unknownError = stringResource(R.string.error_unknown)
    val failureFormat = stringResource(R.string.message_operation_failed)
    val msgSaved = stringResource(R.string.message_saved)
    fun failureText(detail: String?): String =
        String.format(Locale.getDefault(), failureFormat, detail ?: unknownError)

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    /** 真正去读文件内容（挂起函数，读完了才往下走） */
    suspend fun readInto(path: String) {
        isLoadingFile = true
        app.fileRepository.readText(path).fold(
            onSuccess = { content ->
                openFilePath = path
                editorText = content
                savedText = content
            },
            onFailure = { showMessage(failureText(it.message)) },
        )
        isLoadingFile = false
    }

    fun loadFile(path: String) {
        scope.launch { readInto(path) }
    }

    /** 打开文件；如果当前文件有未保存修改，先问一句 */
    fun requestOpenFile(path: String) {
        if (isModified && path != openFilePath) {
            pendingOpenPath = path
        } else {
            loadFile(path)
        }
    }

    fun saveFile() {
        val path = openFilePath ?: return
        val content = editorText
        scope.launch {
            app.fileRepository.writeText(path, content).fold(
                onSuccess = {
                    savedText = content
                    showMessage(msgSaved)
                },
                onFailure = { showMessage(failureText(it.message)) },
            )
        }
    }

    /** 先保存当前文件，保存成功后再打开新文件（顺序执行，避免互相打架） */
    fun saveThenOpen(path: String) {
        val currentPath = openFilePath ?: return
        val content = editorText
        scope.launch {
            app.fileRepository.writeText(currentPath, content).fold(
                onSuccess = {
                    savedText = content
                    showMessage(msgSaved)
                    readInto(path)
                },
                onFailure = { showMessage(failureText(it.message)) },
            )
        }
    }

    // ---------------- 文件浏览器 ----------------
    if (showFileBrowser) {
        BackHandler { showFileBrowser = false }
        FileBrowserScreen(
            repository = app.fileRepository,
            workspaceStore = app.workspaceStore,
            onClose = {
                showFileBrowser = false
                // 浏览器里可能新建/删除/重命名过文件，回来后让树重新读一遍
                treeRefreshKey++
            },
            onFileClick = { item ->
                showFileBrowser = false
                requestOpenFile(item.path)
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
                    scope.launch { drawerState.close() }
                    showFileBrowser = true
                },
                onOpenSettingsClick = {
                    // TODO(后续阶段)：打开设置页（主题切换等）
                },
                onFileClick = { item ->
                    scope.launch { drawerState.close() }
                    requestOpenFile(item.path)
                },
                onMessage = showMessage,
                onTreeChanged = { treeRefreshKey++ },
            )
        },
    ) {
        Scaffold(
            // imePadding：键盘弹出时把编辑区顶上去，底部状态栏不会被挡住
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                EditorTopBar(
                    title = fileName ?: stringResource(R.string.app_name),
                    isModified = isModified,
                    canSave = openFilePath != null && isModified,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSaveClick = { saveFile() },
                    onFindClick = {
                        // 占位：后续阶段实现查找
                    },
                    onMoreClick = {
                        // 占位：后续阶段实现更多菜单
                    },
                )
            },
            bottomBar = {
                EditorStatusBar(isModified = isModified)
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
                    isLoadingFile -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.editor_loading_file),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    openFilePath != null -> PlainTextEditor(
                        text = editorText,
                        onTextChange = { editorText = it },
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> NoFileOpenedPlaceholder(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    // ---------------- 未保存提醒 ----------------
    pendingOpenPath?.let { path ->
        AlertDialog(
            onDismissRequest = { pendingOpenPath = null },
            title = { Text(text = stringResource(R.string.dialog_unsaved_title)) },
            text = { Text(text = stringResource(R.string.dialog_unsaved_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingOpenPath = null
                        saveThenOpen(path)
                    },
                ) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingOpenPath = null
                        loadFile(path)
                    },
                ) {
                    Text(text = stringResource(R.string.action_discard))
                }
            },
        )
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
