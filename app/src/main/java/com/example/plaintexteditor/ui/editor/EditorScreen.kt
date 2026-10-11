package com.example.plaintexteditor.ui.editor

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.plaintexteditor.data.text.LineEnding
import com.example.plaintexteditor.data.text.TextEncoding
import com.example.plaintexteditor.data.text.TextFormat
import com.example.plaintexteditor.data.text.TextFormats
import com.example.plaintexteditor.ui.browser.FileBrowserScreen
import com.example.plaintexteditor.ui.common.ActionSheetRow
import com.example.plaintexteditor.ui.theme.PlainTextEditorTheme
import com.example.plaintexteditor.ui.theme.ThemeMode
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 编辑中每隔多久把内容写一份草稿（不碰原文件） */
private const val DRAFT_AUTO_SAVE_INTERVAL_MS = 10_000L

/** 打开文件时发现有草稿，先记下来，等用户决定“恢复”还是“丢弃” */
private data class DraftPrompt(val path: String, val content: String)

/**
 * 主界面，也是编辑区的状态大本营：
 *
 * - 当前文件路径、当前文本、打开/保存时的文本（用来判断有没有改动）
 * - 编码与换行符：打开时自动检测，保存时按原样写回，也可以手动切换
 * - 草稿：编辑中定期把内容写进私有缓存目录；下次打开同一文件时问要不要恢复
 * - 退出与切换文件时的“保存 / 放弃 / 取消”提示
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    // ---------------- 界面开关 ----------------
    var treeRefreshKey by remember { mutableStateOf(0) }
    var showFileBrowser by rememberSaveable { mutableStateOf(false) }
    var showEncodingSheet by remember { mutableStateOf(false) }
    var showLineEndingSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // ---------------- 编辑区状态 ----------------
    var openFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var editorText by rememberSaveable { mutableStateOf("") }
    var savedText by rememberSaveable { mutableStateOf("") }
    var textFormat by remember { mutableStateOf(TextFormat()) }
    var isLoadingFile by remember { mutableStateOf(false) }
    var pendingOpenPath by remember { mutableStateOf<String?>(null) }
    var pendingDraft by remember { mutableStateOf<DraftPrompt?>(null) }

    val isModified = openFilePath != null && editorText != savedText
    val fileName = openFilePath?.substringAfterLast('/')

    // 这些文字要在协程里用，所以先取出来（协程里不能调用 stringResource）
    val unknownError = stringResource(R.string.error_unknown)
    val failureFormat = stringResource(R.string.message_operation_failed)
    val msgSaved = stringResource(R.string.message_saved)
    val msgDraftRestored = stringResource(R.string.message_draft_restored)
    val msgDraftDiscarded = stringResource(R.string.message_draft_discarded)
    val msgSaveBeforeSwitch = stringResource(R.string.message_save_before_switch_encoding)
    val currentSuffix = stringResource(R.string.label_current)

    fun failureText(detail: String?): String =
        String.format(Locale.getDefault(), failureFormat, detail ?: unknownError)

    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun closeApp() {
        (context as? Activity)?.finish()
    }

    // ---------------- 打开 / 保存 ----------------
    /** 真正去读文件：检测编码与换行符；需要时再检查有没有草稿 */
    suspend fun readInto(
        path: String,
        checkDraft: Boolean,
        overrideEncoding: TextEncoding? = null,
    ) {
        isLoadingFile = true
        app.fileRepository.readTextFile(path, overrideEncoding).fold(
            onSuccess = { content ->
                openFilePath = path
                editorText = content.text
                savedText = content.text
                textFormat = content.format
                if (checkDraft) {
                    val draft = app.draftStore.read(path).getOrNull()
                    if (!draft.isNullOrEmpty() && draft != content.text) {
                        pendingDraft = DraftPrompt(path, draft)
                    }
                }
            },
            onFailure = { showMessage(failureText(it.message)) },
        )
        isLoadingFile = false
    }

    fun loadFile(path: String) {
        scope.launch { readInto(path, checkDraft = true) }
    }

    /** 打开文件；如果当前文件有未保存修改，先问一句 */
    fun requestOpenFile(path: String) {
        if (isModified && path != openFilePath) {
            pendingOpenPath = path
        } else {
            loadFile(path)
        }
    }

    /** 把内容按当前编码/换行符写回文件；成功返回 true */
    suspend fun writeCurrent(path: String, content: String): Boolean =
        app.fileRepository.writeTextFile(path, content, textFormat).fold(
            onSuccess = {
                savedText = content
                // 保存成功，草稿就没用了
                app.draftStore.delete(path)
                showMessage(msgSaved)
                true
            },
            onFailure = {
                showMessage(failureText(it.message))
                false
            },
        )

    fun saveFile() {
        val path = openFilePath ?: return
        val content = editorText
        scope.launch { writeCurrent(path, content) }
    }

    /** 先保存当前文件，保存成功后再打开新文件 */
    fun saveThenOpen(path: String) {
        val currentPath = openFilePath ?: return
        val content = editorText
        scope.launch {
            if (writeCurrent(currentPath, content)) {
                readInto(path, checkDraft = true)
            }
        }
    }

    fun saveAndExit() {
        val path = openFilePath
        val content = editorText
        scope.launch {
            if (path == null || writeCurrent(path, content)) closeApp()
        }
    }

    fun discardAndExit() {
        val path = openFilePath
        scope.launch {
            if (path != null) app.draftStore.delete(path)
            closeApp()
        }
    }

    /** 切换编码 = 用新编码把文件重新读一遍（有未保存修改时先让用户处理） */
    fun switchEncoding(encoding: TextEncoding) {
        val path = openFilePath ?: return
        if (isModified) {
            showMessage(msgSaveBeforeSwitch)
            return
        }
        scope.launch { readInto(path, checkDraft = false, overrideEncoding = encoding) }
    }

    /** 切换换行符 = 直接把内存里的换行符换掉（保存时才写进文件） */
    fun switchLineEnding(lineEnding: LineEnding) {
        if (openFilePath == null) return
        textFormat = textFormat.copy(lineEnding = lineEnding)
        editorText = TextFormats.convertLineEndings(editorText, lineEnding)
    }

    // 编辑中每隔一段时间写一份草稿，这样即使应用被杀掉也不会丢内容
    LaunchedEffect(openFilePath, isModified) {
        if (openFilePath == null || !isModified) return@LaunchedEffect
        while (true) {
            delay(DRAFT_AUTO_SAVE_INTERVAL_MS)
            val path = openFilePath ?: break
            if (editorText == savedText) break
            app.draftStore.save(path, editorText)
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

    // 有未保存修改时，系统返回键先弹提示，不直接退出
    BackHandler(enabled = isModified) {
        val path = openFilePath
        if (path != null) {
            val content = editorText
            scope.launch { app.draftStore.save(path, content) }
        }
        showExitDialog = true
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
                    // TODO(后续阶段)：打开设置页
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
                        // 占位：下一阶段实现查找替换
                    },
                    onSwitchEncoding = { showEncodingSheet = true },
                    onSwitchLineEnding = { showLineEndingSheet = true },
                )
            },
            bottomBar = {
                EditorStatusBar(
                    isModified = isModified,
                    encoding = textFormat.encoding.displayName,
                    lineEnding = textFormat.lineEnding.displayName,
                )
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

    // ---------------- 切换编码 ----------------
    if (showEncodingSheet) {
        ModalBottomSheet(onDismissRequest = { showEncodingSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.sheet_title_encoding),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                )
                HorizontalDivider()
                TextEncoding.values().forEach { encoding ->
                    val label = encoding.displayName +
                        if (encoding == textFormat.encoding) currentSuffix else ""
                    ActionSheetRow(
                        text = label,
                        onClick = {
                            showEncodingSheet = false
                            switchEncoding(encoding)
                        },
                    )
                }
            }
        }
    }

    // ---------------- 切换换行符 ----------------
    if (showLineEndingSheet) {
        ModalBottomSheet(onDismissRequest = { showLineEndingSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.sheet_title_line_ending),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                )
                HorizontalDivider()
                LineEnding.values().forEach { lineEnding ->
                    val label = lineEnding.displayName +
                        if (lineEnding == textFormat.lineEnding) currentSuffix else ""
                    ActionSheetRow(
                        text = label,
                        onClick = {
                            showLineEndingSheet = false
                            switchLineEnding(lineEnding)
                        },
                    )
                }
            }
        }
    }

    // ---------------- 切换文件：未保存提示 ----------------
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
                        val currentPath = openFilePath
                        if (currentPath != null) {
                            scope.launch { app.draftStore.delete(currentPath) }
                        }
                        loadFile(path)
                    },
                ) {
                    Text(text = stringResource(R.string.action_discard))
                }
            },
        )
    }

    // ---------------- 退出：未保存提示 ----------------
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = stringResource(R.string.dialog_exit_title)) },
            text = { Text(text = stringResource(R.string.dialog_exit_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        saveAndExit()
                    },
                ) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        discardAndExit()
                    },
                ) {
                    Text(text = stringResource(R.string.action_exit_without_saving))
                }
            },
        )
    }

    // ---------------- 发现草稿：要不要恢复 ----------------
    pendingDraft?.let { prompt ->
        AlertDialog(
            onDismissRequest = { pendingDraft = null },
            title = { Text(text = stringResource(R.string.dialog_draft_title)) },
            text = { Text(text = stringResource(R.string.dialog_draft_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDraft = null
                        editorText = prompt.content
                        showMessage(msgDraftRestored)
                    },
                ) {
                    Text(text = stringResource(R.string.action_restore_draft))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDraft = null
                        scope.launch { app.draftStore.delete(prompt.path) }
                        showMessage(msgDraftDiscarded)
                    },
                ) {
                    Text(text = stringResource(R.string.action_discard_draft))
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
