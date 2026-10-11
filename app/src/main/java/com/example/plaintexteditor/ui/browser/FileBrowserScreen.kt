package com.example.plaintexteditor.ui.browser

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R
import com.example.plaintexteditor.data.model.FileItem
import com.example.plaintexteditor.data.model.WorkspaceType
import com.example.plaintexteditor.data.repository.FileRepository
import com.example.plaintexteditor.data.workspace.WorkspaceStore
import com.example.plaintexteditor.ui.common.ConfirmDeleteDialog
import com.example.plaintexteditor.ui.common.NameInputDialog
import com.example.plaintexteditor.ui.common.displayNameOf
import com.example.plaintexteditor.ui.common.readUriBytes
import com.example.plaintexteditor.ui.common.sharePrivateFile
import com.example.plaintexteditor.ui.common.writeUriBytes
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 正在输入名字的三种情况 */
private enum class NameDialogKind { NEW_FOLDER, NEW_FILE, RENAME }

/** 输入名字的弹窗状态（重命名时要带上被改的那一项） */
private data class NameDialogState(
    val kind: NameDialogKind,
    val item: FileItem? = null,
)

/** 正在挑选目标文件夹的两种情况 */
private enum class PickerKind { COPY, MOVE }

private data class PickerState(
    val kind: PickerKind,
    val item: FileItem,
)

/**
 * 全屏“文件浏览器”页面。
 *
 * 从应用私有目录根开始，一层一层往下看；点文件夹进入，点面包屑跳回某一层，
 * 长按或点右边的“更多”弹出操作菜单（添加到工作区 / 重命名 / 复制 / 移动 / 删除）。
 *
 * @param repository 文件仓库（阶段二写的）
 * @param workspaceStore 工作区存储（阶段二写的）
 * @param onClose 点左上角关闭按钮时做什么（回到主界面）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    repository: FileRepository,
    workspaceStore: WorkspaceStore,
    onClose: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val rootName = stringResource(R.string.browser_title)
    val msgCreated = stringResource(R.string.message_created)
    val msgRenamed = stringResource(R.string.message_renamed)
    val msgDeleted = stringResource(R.string.message_deleted)
    val msgCopied = stringResource(R.string.message_copied)
    val msgMoved = stringResource(R.string.message_moved)
    val msgAdded = stringResource(R.string.message_added_to_workspace)
    val msgAlreadyInWorkspace = stringResource(R.string.message_already_in_workspace)
    val msgUnknownError = stringResource(R.string.error_unknown)
    val failureFormat = stringResource(R.string.message_operation_failed)
    val msgImportFailed = stringResource(R.string.message_import_failed)
    val msgImportDoneFormat = stringResource(R.string.message_import_done)
    val msgExportDone = stringResource(R.string.message_export_done)
    val msgExportFailed = stringResource(R.string.message_export_failed)
    val msgShareFailed = stringResource(R.string.message_share_failed)

    fun failureText(detail: String?): String =
        String.format(Locale.getDefault(), failureFormat, detail ?: msgUnknownError)

    // ---------- 页面状态 ----------
    var currentPath by rememberSaveable { mutableStateOf(FileRepository.ROOT_PATH) }
    var items by remember { mutableStateOf(emptyList<FileItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    var showNewSheet by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf<NameDialogState?>(null) }
    var actionTarget by remember { mutableStateOf<FileItem?>(null) }
    var pendingDelete by remember { mutableStateOf<FileItem?>(null) }
    var pickerState by remember { mutableStateOf<PickerState?>(null) }
    var exportTarget by remember { mutableStateOf<FileItem?>(null) }

    fun reload() {
        reloadKey++
    }

    fun toast(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    // 每次换目录（或做完一次操作）就重新读一次
    LaunchedEffect(currentPath, reloadKey) {
        isLoading = true
        repository.list(currentPath).fold(
            onSuccess = { list ->
                items = list
                errorMessage = null
            },
            onFailure = { errorMessage = it.message },
        )
        isLoading = false
    }

    // 系统返回键：不在根目录就先回上一级，已经在根目录就交给外面（关闭浏览器）
    BackHandler(enabled = currentPath.isNotEmpty()) {
        currentPath = parentPathOf(currentPath)
    }

    // ---------- 各种操作的执行 ----------
    fun submitName(state: NameDialogState, name: String) {
        scope.launch {
            val result = when (state.kind) {
                NameDialogKind.NEW_FOLDER -> repository.createDirectory(currentPath, name)
                NameDialogKind.NEW_FILE -> repository.createTextFile(currentPath, name)
                NameDialogKind.RENAME -> {
                    val target = state.item
                    if (target == null) {
                        null
                    } else {
                        repository.rename(target.path, name)
                    }
                }
            }
            if (result == null) {
                toast(failureText(null))
                return@launch
            }
            result.fold(
                onSuccess = {
                    reload()
                    toast(
                        when (state.kind) {
                            NameDialogKind.NEW_FOLDER, NameDialogKind.NEW_FILE -> msgCreated
                            NameDialogKind.RENAME -> msgRenamed
                        },
                    )
                },
                onFailure = { toast(failureText(it.message)) },
            )
        }
    }

    fun deleteItem(item: FileItem) {
        scope.launch {
            repository.delete(item.path).fold(
                onSuccess = {
                    reload()
                    toast(msgDeleted)
                },
                onFailure = { toast(failureText(it.message)) },
            )
        }
    }

    fun addToWorkspace(item: FileItem) {
        scope.launch {
            if (workspaceStore.contains(item.path)) {
                toast(msgAlreadyInWorkspace)
                return@launch
            }
            val type = if (item.isDirectory) WorkspaceType.DIRECTORY else WorkspaceType.FILE
            workspaceStore.add(item.path, type).fold(
                onSuccess = { toast(msgAdded) },
                onFailure = { toast(failureText(it.message)) },
            )
        }
    }

    fun transfer(target: PickerState, destinationParent: String) {
        scope.launch {
            val result = when (target.kind) {
                PickerKind.COPY -> repository.copy(target.item.path, destinationParent)
                PickerKind.MOVE -> repository.move(target.item.path, destinationParent)
            }
            result.fold(
                onSuccess = {
                    reload()
                    toast(if (target.kind == PickerKind.COPY) msgCopied else msgMoved)
                },
                onFailure = { toast(failureText(it.message)) },
            )
        }
    }

    // ---------- 界面 ----------
    // ---------- 导入 / 导出 / 分享 ----------

    /** 从系统文件选择器选中的外部文件：读出字节，原样复制进私有目录 */
    fun importExternalFile(uri: android.net.Uri) {
        scope.launch {
            val name = displayNameOf(context, uri)
            val bytes = withContext(Dispatchers.IO) { readUriBytes(context, uri) }
            if (bytes == null) {
                toast(msgImportFailed)
                return@launch
            }
            val path = repository.uniquePath(currentPath, name).getOrNull()
            if (path == null) {
                toast(msgImportFailed)
                return@launch
            }
            repository.writeBinaryFile(path, bytes).fold(
                onSuccess = {
                    reload()
                    toast(
                        String.format(
                            Locale.getDefault(),
                            msgImportDoneFormat,
                            path.substringAfterLast('/'),
                        ),
                    )
                },
                onFailure = { toast(failureText(it.message)) },
            )
        }
    }

    /** 导出到用户选中的位置（public 目录也行，靠系统授权，不需要权限） */
    fun exportTo(uri: android.net.Uri, item: FileItem) {
        scope.launch {
            val bytes = repository.readBinaryFile(item.path).getOrNull()
            if (bytes == null) {
                toast(msgExportFailed)
                return@launch
            }
            val ok = withContext(Dispatchers.IO) { writeUriBytes(context, uri, bytes) }
            toast(if (ok) msgExportDone else msgExportFailed)
        }
    }

    /** 分享：交给系统分享面板 */
    fun shareItem(item: FileItem) {
        val ok = sharePrivateFile(
            context = context,
            rootAbsolutePath = repository.rootAbsolutePath,
            relativePath = item.path,
        )
        if (!ok) toast(msgShareFailed)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) importExternalFile(uri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        val target = exportTarget
        exportTarget = null
        if (uri != null && target != null) exportTo(uri, target)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.browser_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_import),
                            contentDescription = stringResource(R.string.action_import),
                        )
                    }
                    IconButton(onClick = { showNewSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_new),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            BreadcrumbRow(
                currentPath = currentPath,
                rootName = rootName,
                onNavigate = { currentPath = it },
            )
            HorizontalDivider()

            val error = errorMessage
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                error != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { reload() }) {
                        Text(text = stringResource(R.string.browser_retry))
                    }
                }

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (currentPath.isNotEmpty()) {
                        item(key = "__up__") {
                            UpRow(onClick = { currentPath = parentPathOf(currentPath) })
                        }
                    }
                    if (items.isEmpty()) {
                        item(key = "__empty__") {
                            Text(
                                text = stringResource(R.string.browser_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                            )
                        }
                    }
                    items(items, key = { it.path }) { item ->
                        FileEntryRow(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    currentPath = item.path
                                } else {
                                    // 点文件 = 交给编辑区打开（页面由外面负责关掉）
                                    onFileClick(item)
                                }
                            },
                            onLongClick = { actionTarget = item },
                            onMenuClick = { actionTarget = item },
                        )
                    }
                }
            }
        }
    }

    // ---------- 弹窗与底部菜单 ----------
    if (showNewSheet) {
        NewItemSheet(
            onDismiss = { showNewSheet = false },
            onNewFolder = {
                showNewSheet = false
                nameDialog = NameDialogState(NameDialogKind.NEW_FOLDER)
            },
            onNewFile = {
                showNewSheet = false
                nameDialog = NameDialogState(NameDialogKind.NEW_FILE)
            },
        )
    }

    nameDialog?.let { state ->
        NameInputDialog(
            title = when (state.kind) {
                NameDialogKind.NEW_FOLDER -> stringResource(R.string.action_new_folder)
                NameDialogKind.NEW_FILE -> stringResource(R.string.action_new_file)
                NameDialogKind.RENAME -> stringResource(R.string.action_rename)
            },
            confirmText = stringResource(R.string.dialog_confirm),
            initialValue = state.item?.name.orEmpty(),
            onDismiss = { nameDialog = null },
            onConfirm = { name ->
                nameDialog = null
                submitName(state, name)
            },
        )
    }

    actionTarget?.let { item ->
        FileActionSheet(
            item = item,
            onDismiss = { actionTarget = null },
            onAddToWorkspace = {
                actionTarget = null
                addToWorkspace(item)
            },
            onShare = {
                actionTarget = null
                shareItem(item)
            },
            onExport = {
                actionTarget = null
                exportTarget = item
                exportLauncher.launch(item.name)
            },
            onRename = {
                actionTarget = null
                nameDialog = NameDialogState(NameDialogKind.RENAME, item)
            },
            onCopy = {
                actionTarget = null
                pickerState = PickerState(PickerKind.COPY, item)
            },
            onMove = {
                actionTarget = null
                pickerState = PickerState(PickerKind.MOVE, item)
            },
            onDelete = {
                actionTarget = null
                pendingDelete = item
            },
        )
    }

    pendingDelete?.let { item ->
        ConfirmDeleteDialog(
            item = item,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                deleteItem(item)
            },
        )
    }

    pickerState?.let { state ->
        DestinationPickerSheet(
            title = if (state.kind == PickerKind.COPY) {
                stringResource(R.string.picker_title_copy)
            } else {
                stringResource(R.string.picker_title_move)
            },
            confirmText = if (state.kind == PickerKind.COPY) {
                stringResource(R.string.picker_confirm_copy)
            } else {
                stringResource(R.string.picker_confirm_move)
            },
            repository = repository,
            onDismiss = { pickerState = null },
            onConfirm = { destination ->
                pickerState = null
                transfer(state, destination)
            },
        )
    }
}

/**
 * 面包屑：私有目录 › notes › 2026 ，点哪一段就跳到哪一层。
 */
@Composable
private fun BreadcrumbRow(
    currentPath: String,
    rootName: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = remember(currentPath) {
        if (currentPath.isEmpty()) emptyList() else currentPath.split('/')
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onNavigate(FileRepository.ROOT_PATH) }) {
            Text(
                text = rootName,
                style = MaterialTheme.typography.labelLarge,
                color = if (segments.isEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        segments.forEachIndexed { index, segment ->
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            val targetPath = segments.take(index + 1).joinToString("/")
            val isCurrent = index == segments.lastIndex
            TextButton(onClick = { onNavigate(targetPath) }) {
                Text(
                    text = segment,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** “上一级”那一行 */
@Composable
private fun UpRow(onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.browser_up),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
