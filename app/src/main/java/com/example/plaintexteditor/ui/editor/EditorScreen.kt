package com.example.plaintexteditor.ui.editor

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.PlainTextEditorApp
import com.example.plaintexteditor.R
import com.example.plaintexteditor.data.text.LineEnding
import com.example.plaintexteditor.data.search.LargeFileSearch
import com.example.plaintexteditor.data.search.LargeMatch
import com.example.plaintexteditor.data.text.LargeFileIndex
import com.example.plaintexteditor.data.search.SearchMatch
import com.example.plaintexteditor.data.search.SearchOptions
import com.example.plaintexteditor.data.search.TextSearch
import com.example.plaintexteditor.data.text.LineCounter
import com.example.plaintexteditor.data.text.TextEncoding
import com.example.plaintexteditor.data.text.TextFormat
import com.example.plaintexteditor.data.text.TextFormats
import com.example.plaintexteditor.ui.browser.FileBrowserScreen
import com.example.plaintexteditor.ui.common.ActionSheetRow
import com.example.plaintexteditor.ui.common.sharePrivateFile
import com.example.plaintexteditor.ui.settings.SettingsScreen
import com.example.plaintexteditor.ui.theme.PlainTextEditorTheme
import com.example.plaintexteditor.ui.theme.ThemeMode
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 一次查找最多等多久（毫秒）；超过就放弃等待，避免正则把界面卡住 */
private const val SEARCH_TIMEOUT_MS = 3_000L

/** 当前行号多久算一次（毫秒） */
private const val LINE_TICK_MS = 2_000L

/** 行号显示多久后淡出（毫秒） */
private const val LINE_BADGE_VISIBLE_MS = 1_000L

/** 大文件里流式查找最多等多久（毫秒） */
private const val LARGE_SEARCH_TIMEOUT_MS = 15_000L

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
    val showCurrentLine by app.settingsStore.showCurrentLine.collectAsState(initial = true)
    val fontSizeSp by app.settingsStore.fontSizeSp.collectAsState(initial = 16)
    val draftIntervalSeconds by app.settingsStore.draftIntervalSeconds.collectAsState(initial = 10)
    val defaultEncodingName by app.settingsStore.defaultEncodingName.collectAsState(initial = "UTF-8")
    val defaultLineEndingName by app.settingsStore.defaultLineEndingName.collectAsState(initial = "LF")

    // ---------------- 界面开关 ----------------
    var treeRefreshKey by remember { mutableStateOf(0) }
    var showFileBrowser by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
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

    // ---------------- 查找替换状态 ----------------
    var showFindPanel by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var useRegex by remember { mutableStateOf(false) }
    var caseSensitive by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var multiline by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf(emptyList<SearchMatch>()) }
    var currentMatchIndex by remember { mutableStateOf(-1) }
    var searchStatus by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var selectionRequest by remember { mutableStateOf<SelectionRequest?>(null) }
    var selectionToken by remember { mutableStateOf(0) }
    var confirmReplaceAll by remember { mutableStateOf(false) }

    // ---------------- 行号状态 ----------------
    var cursorOffset by remember { mutableStateOf(0) }
    var totalLines by remember { mutableStateOf<Int?>(null) }
    var currentLine by remember { mutableStateOf<Int?>(null) }
    var lineBadgeVisible by remember { mutableStateOf(false) }

    // ---------------- 大文件只读状态 ----------------
    var largeFileIndex by remember { mutableStateOf<LargeFileIndex?>(null) }
    var largeMatches by remember { mutableStateOf(emptyList<LargeMatch>()) }
    val largeListState = rememberLazyListState()
    val thresholdMb by app.settingsStore.largeFileThresholdMb.collectAsState(
        initial = 5,
    )
    val isLargeMode = largeFileIndex != null

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
    val findTimeoutMessage = stringResource(R.string.find_timeout)
    val findInvalidRegexFormat = stringResource(R.string.find_invalid_regex)
    val msgReplacedCount = stringResource(R.string.message_replaced_count)
    val msgReplaceFailed = stringResource(R.string.message_replace_failed)
    val msgJumpToMatch = stringResource(R.string.message_jump_to_match)
    val largeNoticeFormat = stringResource(R.string.large_file_readonly_notice)
    val msgEncodingSwitchDisabled = stringResource(R.string.message_encoding_switch_disabled)
    val msgShareFailed = stringResource(R.string.message_share_failed)

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
        // 先看文件大小：超过阈值就走“大文件只读”这条路，绝不把整篇读进内存
        val sizeBytes = app.fileRepository.fileSize(path).getOrNull() ?: 0L
        if (sizeBytes > thresholdMb.toLong() * 1024L * 1024L) {
            app.fileRepository.buildLargeFileIndex(path).fold(
                onSuccess = { index ->
                    largeFileIndex = index
                    openFilePath = path
                    largeMatches = emptyList()
                    matches = emptyList()
                    currentMatchIndex = -1
                    searchStatus = null
                    editorText = ""
                    savedText = ""
                    textFormat = TextFormat(encoding = index.encoding, hasBom = index.hasBom)
                    largeListState.scrollToItem(0)
                },
                onFailure = { showMessage(failureText(it.message)) },
            )
            isLoadingFile = false
            return
        }

        // 普通文件：读完就回到正常（可编辑）模式
        largeFileIndex = null
        largeMatches = emptyList()
        val defaultFormat = TextFormat(
            encoding = TextEncoding.values()
                .firstOrNull { it.charsetName == defaultEncodingName } ?: TextEncoding.UTF_8,
            hasBom = false,
            lineEnding = if (defaultLineEndingName == "CRLF") LineEnding.CRLF else LineEnding.LF,
        )
        app.fileRepository.readTextFile(path, overrideEncoding).fold(
            onSuccess = { content ->
                openFilePath = path
                // 空文件用“设置里的默认编码 / 默认换行符”，非空文件保持它自己的格式
                if (content.text.isEmpty() && overrideEncoding == null) {
                    editorText = ""
                    savedText = ""
                    textFormat = defaultFormat
                } else {
                    editorText = content.text
                    savedText = content.text
                    textFormat = content.format
                }
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
        if (isLargeMode) {
            showMessage(msgEncodingSwitchDisabled)
            return
        }
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
    LaunchedEffect(openFilePath, isModified, draftIntervalSeconds) {
        if (openFilePath == null || !isModified) return@LaunchedEffect
        while (true) {
            delay(draftIntervalSeconds * 1000L)
            val path = openFilePath ?: break
            if (editorText == savedText) break
            app.draftStore.save(path, editorText)
        }
    }

    // ---------------- 查找替换 ----------------
    fun currentSearchOptions(): SearchOptions = SearchOptions(
        query = searchQuery,
        replacement = replaceQuery,
        useRegex = useRegex,
        caseSensitive = caseSensitive,
        wholeWord = wholeWord,
        multiline = multiline,
    )

    /** 把光标/选区跳到第 index 个匹配上，并让编辑区滚动到它 */
    fun selectMatch(index: Int) {
        val match = matches.getOrNull(index) ?: return
        currentMatchIndex = index
        selectionToken += 1
        selectionRequest = SelectionRequest(
            token = selectionToken,
            start = match.start,
            end = match.end,
        )
    }

    /** 大文件里：滚到命中所在的那一块，并提示在第几行 */
    fun selectLargeMatch(index: Int) {
        val match = largeMatches.getOrNull(index) ?: return
        currentMatchIndex = index
        scope.launch { largeListState.animateScrollToItem(match.chunkIndex) }
        showMessage(
            String.format(Locale.getDefault(), msgJumpToMatch, match.lineNumber, match.snippet),
        )
    }

    fun findNext() {
        if (largeFileIndex != null) {
            val list = largeMatches
            if (list.isEmpty()) return
            selectLargeMatch(if (currentMatchIndex >= list.lastIndex) 0 else currentMatchIndex + 1)
            return
        }
        if (matches.isEmpty()) return
        selectMatch(if (currentMatchIndex >= matches.lastIndex) 0 else currentMatchIndex + 1)
    }

    fun findPrevious() {
        if (largeFileIndex != null) {
            val list = largeMatches
            if (list.isEmpty()) return
            selectLargeMatch(if (currentMatchIndex <= 0) list.lastIndex else currentMatchIndex - 1)
            return
        }
        if (matches.isEmpty()) return
        selectMatch(if (currentMatchIndex <= 0) matches.lastIndex else currentMatchIndex - 1)
    }

    /** 替换当前这一处匹配 */
    fun replaceCurrent() {
        if (isLargeMode) return
        if (matches.getOrNull(currentMatchIndex) == null) return
        val result = TextSearch.replaceMatch(editorText, currentMatchIndex, currentSearchOptions())
        if (!result.replaced) {
            showMessage(msgReplaceFailed)
            return
        }
        editorText = result.text
        selectionToken += 1
        selectionRequest = SelectionRequest(
            token = selectionToken,
            start = result.selection,
            end = result.selection,
        )
    }

    /** 全部替换（先由界面确认过一次才调用） */
    fun replaceAll() {
        if (isLargeMode) return
        val options = currentSearchOptions()
        scope.launch {
            val result = withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                withContext(Dispatchers.Default) { TextSearch.replaceAll(editorText, options) }
            }
            when {
                result == null || result.timedOut -> showMessage(findTimeoutMessage)
                result.error != null -> showMessage(
                    String.format(Locale.getDefault(), findInvalidRegexFormat, result.error),
                )

                result.count == 0 -> showMessage(msgReplaceFailed)
                else -> {
                    editorText = result.text
                    showMessage(String.format(Locale.getDefault(), msgReplacedCount, result.count))
                }
            }
        }
    }

    // 查找：输入停 300 毫秒后才算；真正计算丢到后台线程，最多等 SEARCH_TIMEOUT_MS
    LaunchedEffect(
        showFindPanel,
        searchQuery,
        useRegex,
        caseSensitive,
        wholeWord,
        multiline,
        editorText,
        largeFileIndex,
    ) {
        if (!showFindPanel || searchQuery.isEmpty()) {
            matches = emptyList()
            largeMatches = emptyList()
            currentMatchIndex = -1
            searchStatus = null
            isSearching = false
            return@LaunchedEffect
        }

        // 大文件：一块一块地流式扫描（不把整篇读进内存）
        val largeIndex = largeFileIndex
        if (largeIndex != null) {
            delay(300)
            isSearching = true
            val options = currentSearchOptions()
            val result = withTimeoutOrNull(LARGE_SEARCH_TIMEOUT_MS) {
                withContext(Dispatchers.IO) { LargeFileSearch.search(largeIndex, options) }
            }
            isSearching = false
            matches = emptyList()
            when {
                result == null || result.timedOut -> {
                    largeMatches = result?.matches.orEmpty()
                    currentMatchIndex = if (largeMatches.isEmpty()) -1 else 0
                    searchStatus = findTimeoutMessage
                }

                result.error != null -> {
                    largeMatches = emptyList()
                    currentMatchIndex = -1
                    searchStatus = String.format(
                        Locale.getDefault(),
                        findInvalidRegexFormat,
                        result.error,
                    )
                }

                else -> {
                    largeMatches = result.matches
                    currentMatchIndex = if (result.matches.isEmpty()) -1 else 0
                    searchStatus = null
                }
            }
            return@LaunchedEffect
        }

        largeMatches = emptyList()
        delay(300)
        isSearching = true
        val options = currentSearchOptions()
        val result = withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
            withContext(Dispatchers.Default) { TextSearch.find(editorText, options) }
        }
        isSearching = false
        when {
            result == null -> {
                matches = emptyList()
                currentMatchIndex = -1
                searchStatus = findTimeoutMessage
            }

            result.error != null -> {
                matches = emptyList()
                currentMatchIndex = -1
                searchStatus = String.format(
                    Locale.getDefault(),
                    findInvalidRegexFormat,
                    result.error,
                )
            }

            else -> {
                matches = result.matches
                // 尽量保住当前选中的那一处；越界了就回到第一处
                currentMatchIndex = when {
                    result.matches.isEmpty() -> -1
                    currentMatchIndex in result.matches.indices -> currentMatchIndex
                    else -> 0
                }
                searchStatus = if (result.timedOut) findTimeoutMessage else null
            }
        }
    }

    // 总行数：文件内容变化后（停顿 400 毫秒）在后台数一次
    LaunchedEffect(openFilePath, editorText, largeFileIndex) {
        val largeIndex = largeFileIndex
        if (largeIndex != null) {
            // 大文件：行数在建索引时已经数好了
            totalLines = largeIndex.totalLines
            return@LaunchedEffect
        }
        if (openFilePath == null) {
            totalLines = null
            return@LaunchedEffect
        }
        delay(400)
        totalLines = withContext(Dispatchers.Default) { LineCounter.countLines(editorText) }
    }

    // 当前行号：每 2 秒算一次（在后台线程算，不卡界面）
    LaunchedEffect(showCurrentLine, openFilePath, largeFileIndex) {
        if (!showCurrentLine || openFilePath == null) {
            currentLine = null
            lineBadgeVisible = false
            return@LaunchedEffect
        }
        while (true) {
            delay(LINE_TICK_MS)
            val largeIndex = largeFileIndex
            val line = if (largeIndex != null) {
                // 只读模式没有光标，就用“当前屏幕最上面那一块”的第一行
                largeIndex.firstLineOfChunk(largeListState.firstVisibleItemIndex)
            } else {
                withContext(Dispatchers.Default) {
                    LineCounter.lineOfOffset(editorText, cursorOffset)
                }
            }
            // 只有行号真的变了才更新 —— 光标没动就不会反复触发动画
            if (line != currentLine) currentLine = line
        }
    }

    // 行号变化后先显示 1 秒，再淡出消失
    LaunchedEffect(currentLine) {
        if (currentLine == null) {
            lineBadgeVisible = false
            return@LaunchedEffect
        }
        lineBadgeVisible = true
        delay(LINE_BADGE_VISIBLE_MS)
        lineBadgeVisible = false
    }

    // ---------------- 文件浏览器 ----------------
    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            settingsStore = app.settingsStore,
            onClose = { showSettings = false },
            modifier = modifier,
        )
        return
    }

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
                    scope.launch { drawerState.close() }
                    showSettings = true
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
                    canSave = openFilePath != null && isModified && !isLargeMode,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSaveClick = { saveFile() },
                    onFindClick = { showFindPanel = !showFindPanel },
                    onSwitchEncoding = { showEncodingSheet = true },
                    onSwitchLineEnding = { showLineEndingSheet = true },
                    showCurrentLine = showCurrentLine,
                    onToggleCurrentLine = {
                        scope.launch { app.settingsStore.setShowCurrentLine(!showCurrentLine) }
                    },
                    onOpenSettings = { showSettings = true },
                    canShare = openFilePath != null && !isLargeMode,
                    onShare = {
                        val path = openFilePath
                        if (path != null) {
                            val ok = sharePrivateFile(
                                context = context,
                                rootAbsolutePath = app.fileRepository.rootAbsolutePath,
                                relativePath = path,
                            )
                            if (!ok) showMessage(msgShareFailed)
                        }
                    },
                )
            },
            bottomBar = {
                EditorStatusBar(
                    isModified = isModified,
                    encoding = textFormat.encoding.displayName,
                    lineEnding = textFormat.lineEnding.displayName,
                    totalLines = totalLines,
                    currentLine = currentLine,
                    showCurrentLine = showCurrentLine,
                    readOnly = isLargeMode,
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val largeIndex = largeFileIndex
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

                        largeIndex != null -> LargeFileViewer(
                            index = largeIndex,
                            listState = largeListState,
                            notice = String.format(
                                Locale.getDefault(),
                                largeNoticeFormat,
                                String.format(Locale.US, "%.1f", largeIndex.sizeBytes / 1048576.0),
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )

                        openFilePath != null -> PlainTextEditor(
                            text = editorText,
                            onTextChange = { editorText = it },
                            modifier = Modifier.fillMaxSize(),
                            selectionRequest = selectionRequest,
                            onCursorOffsetChanged = { offset -> cursorOffset = offset },
                            fontSizeSp = fontSizeSp.toFloat(),
                        )

                        else -> NoFileOpenedPlaceholder(modifier = Modifier.fillMaxSize())
                    }

                    // 当前行号的小角标：出现 1 秒后淡出（关掉开关就不显示）。
                    // 用 alpha 动画而不是 AnimatedVisibility —— 更轻，也不用担心作用域问题
                    val badgeVisible = lineBadgeVisible && showCurrentLine && openFilePath != null
                    val badgeAlpha by animateFloatAsState(
                        targetValue = if (badgeVisible) 1f else 0f,
                        animationSpec = tween(durationMillis = 220),
                        label = "lineBadge",
                    )
                    if (badgeAlpha > 0.01f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .graphicsLayer { alpha = badgeAlpha },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.status_line_number_format,
                                    currentLine ?: 0,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                // 查找替换面板：贴在编辑区下方、状态栏上方
                if (showFindPanel) {
                    FindReplacePanel(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        replacement = replaceQuery,
                        onReplacementChange = { replaceQuery = it },
                        useRegex = useRegex,
                        onUseRegexChange = { useRegex = it },
                        caseSensitive = caseSensitive,
                        onCaseSensitiveChange = { caseSensitive = it },
                        wholeWord = wholeWord,
                        onWholeWordChange = { wholeWord = it },
                        multiline = multiline,
                        onMultilineChange = { multiline = it },
                        matchCount = if (isLargeMode) largeMatches.size else matches.size,
                        currentIndex = if (currentMatchIndex < 0) 0 else currentMatchIndex,
                        isSearching = isSearching,
                        statusMessage = searchStatus,
                        // 大文件只读：替换按钮会变灰并给提示
                        readOnly = isLargeMode,
                        onPrevious = { findPrevious() },
                        onNext = { findNext() },
                        onReplace = { replaceCurrent() },
                        onReplaceAll = { confirmReplaceAll = true },
                        onClose = { showFindPanel = false },
                    )
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

    // ---------------- 全部替换：先确认 ----------------
    if (confirmReplaceAll) {
        AlertDialog(
            onDismissRequest = { confirmReplaceAll = false },
            title = { Text(text = stringResource(R.string.dialog_replace_all_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.dialog_replace_all_message,
                        matches.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReplaceAll = false
                        replaceAll()
                    },
                ) {
                    Text(text = stringResource(R.string.action_replace_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplaceAll = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
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
