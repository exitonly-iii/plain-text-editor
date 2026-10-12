package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R
import com.example.plaintexteditor.data.model.FileItem
import com.example.plaintexteditor.data.model.WorkspaceItem
import com.example.plaintexteditor.data.model.WorkspaceType
import com.example.plaintexteditor.data.repository.FileRepository
import com.example.plaintexteditor.data.workspace.WorkspaceStore
import com.example.plaintexteditor.ui.common.ActionSheetRow
import com.example.plaintexteditor.ui.common.ConfirmDeleteDialog
import com.example.plaintexteditor.ui.common.NameInputDialog
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * 侧边栏中间的“工作区文件树”。
 *
 * 逻辑参考 VS Code 的资源管理器，但为手机触摸做了简化：
 * - 点文件夹 = 展开/收起；点文件 = 交给编辑区（本阶段只传路径）
 * - 长按任意一行 = 弹出菜单（从工作区移除 / 重命名 / 删除）
 * - 每一层往里缩进 16dp，整行高度 ≥ 48dp
 *
 * @param workspaces 工作区列表（来自 WorkspaceStore，DataStore 一变这里就会重新组合）
 * @param workspaceStore 工作区存储（用来执行“从工作区移除”）
 * @param refreshKey 这个数字变化时，已经展开的文件夹会重新读一遍磁盘
 * @param onFileClick 点文件时调用（把路径交给编辑区）
 * @param onMessage 显示一条提示（由外面的 Snackbar 负责显示）
 * @param onTreeChanged 做完重命名/删除这类操作后调用，用来触发刷新
 * @param dirtyPath 当前有未保存修改的文件路径（在它所在的行后面显示 *）
 * @param onRemoveWorkspace 请求把某个工作区移出（由外面决定要不要先提示保存）
 * @param onPathRenamed 某个文件/文件夹改完名后通知外面（用来更新正在编辑的路径）
 */
@Composable
fun WorkspaceListContent(
    workspaces: List<WorkspaceItem>,
    repository: FileRepository,
    workspaceStore: WorkspaceStore,
    refreshKey: Int,
    onFileClick: (FileItem) -> Unit,
    onMessage: (String) -> Unit,
    onTreeChanged: () -> Unit,
    dirtyPath: String?,
    onRemoveWorkspace: (FileItem) -> Unit,
    onPathRenamed: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val failureFormat = stringResource(R.string.message_operation_failed)
    val unknownError = stringResource(R.string.error_unknown)
    val msgRenamed = stringResource(R.string.message_renamed)
    val msgDeleted = stringResource(R.string.message_deleted)
    val msgRemoved = stringResource(R.string.workspace_removed)

    fun failureText(detail: String?): String =
        String.format(Locale.getDefault(), failureFormat, detail ?: unknownError)

    // 哪些文件夹是展开的（相对私有根目录的路径）
    var expandedPaths by remember { mutableStateOf(emptySet<String>()) }
    var actionTarget by remember { mutableStateOf<FileItem?>(null) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var deleteTarget by remember { mutableStateOf<FileItem?>(null) }

    if (workspaces.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.workspace_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        workspaces.forEach { workspace ->
            key(workspace.path) {
                WorkspaceNode(
                    item = workspace.asFileItem(),
                    depth = 0,
                    repository = repository,
                    expandedPaths = expandedPaths,
                    refreshKey = refreshKey,
                    onToggleExpand = { path ->
                        expandedPaths = if (path in expandedPaths) {
                            expandedPaths - path
                        } else {
                            expandedPaths + path
                        }
                    },
                    onFileClick = onFileClick,
                    onLongClick = { actionTarget = it },
                    dirtyPath = dirtyPath,
                )
            }
        }
        Spacer(modifier = Modifier.padding(bottom = 8.dp))
    }

    // ---------------- 长按菜单 ----------------
    actionTarget?.let { item ->
        val isWorkspace = workspaces.any { it.path == item.path }
        WorkspaceItemActionSheet(
            item = item,
            canRemoveFromWorkspace = isWorkspace,
            onDismiss = { actionTarget = null },
            onRemoveFromWorkspace = {
                actionTarget = null
                // 交给外面处理：如果这个工作区里有未保存的修改，外面会先问一句
                onRemoveWorkspace(item)
            },
            onRename = {
                actionTarget = null
                renameTarget = item
            },
            onDelete = {
                actionTarget = null
                deleteTarget = item
            },
        )
    }

    // ---------------- 重命名 ----------------
    renameTarget?.let { item ->
        NameInputDialog(
            title = stringResource(R.string.action_rename),
            confirmText = stringResource(R.string.dialog_confirm),
            initialValue = item.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                renameTarget = null
                scope.launch {
                    repository.rename(item.path, newName).fold(
                        onSuccess = { renamed ->
                            // 改的是工作区根目录时，工作区记录里的路径也要跟着改
                            if (workspaces.any { it.path == item.path }) {
                                workspaceStore.remove(item.path)
                                workspaceStore.add(
                                    renamed.path,
                                    if (renamed.isDirectory) {
                                        WorkspaceType.DIRECTORY
                                    } else {
                                        WorkspaceType.FILE
                                    },
                                )
                            }
                            onPathRenamed(item.path, renamed.path)
                            onMessage(msgRenamed)
                            onTreeChanged()
                        },
                        onFailure = { onMessage(failureText(it.message)) },
                    )
                }
            },
        )
    }

    // ---------------- 删除 ----------------
    deleteTarget?.let { item ->
        ConfirmDeleteDialog(
            item = item,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    repository.delete(item.path).fold(
                        onSuccess = {
                            onMessage(msgDeleted)
                            onTreeChanged()
                        },
                        onFailure = { onMessage(failureText(it.message)) },
                    )
                }
            },
        )
    }
}

/**
 * 一个节点（文件夹或文件）以及它展开后的孩子。
 * 递归调用自己，所以想展开多少层都可以。
 */
@Composable
private fun WorkspaceNode(
    item: FileItem,
    depth: Int,
    repository: FileRepository,
    expandedPaths: Set<String>,
    refreshKey: Int,
    onToggleExpand: (String) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onLongClick: (FileItem) -> Unit,
    dirtyPath: String?,
) {
    val expanded = item.isDirectory && item.path in expandedPaths
    val isDirty = dirtyPath != null && (
        dirtyPath == item.path ||
            (item.isDirectory && dirtyPath.startsWith(item.path + "/"))
        )

    TreeRow(
        item = item,
        depth = depth,
        emphasized = depth == 0,
        expanded = if (item.isDirectory) expanded else null,
        isDirty = isDirty,
        onClick = {
            if (item.isDirectory) {
                onToggleExpand(item.path)
            } else {
                onFileClick(item)
            }
        },
        onLongClick = { onLongClick(item) },
        onMenuClick = { onLongClick(item) },
    )

    if (!expanded) return

    var children by remember(item.path, refreshKey) { mutableStateOf<List<FileItem>?>(null) }
    var errorMessage by remember(item.path, refreshKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(item.path, refreshKey) {
        repository.list(item.path).fold(
            onSuccess = {
                children = it
                errorMessage = null
            },
            onFailure = {
                children = emptyList()
                errorMessage = it.message
            },
        )
    }

    val error = errorMessage
    when {
        error != null -> TreeHintRow(
            depth = depth + 1,
            text = error,
            isError = true,
        )

        children == null -> TreeHintRow(
            depth = depth + 1,
            text = "…",
            isError = false,
        )

        children.orEmpty().isEmpty() -> TreeHintRow(
            depth = depth + 1,
            text = stringResource(R.string.tree_empty),
            isError = false,
        )

        else -> children.orEmpty().forEach { child ->
            key(child.path) {
                WorkspaceNode(
                    item = child,
                    depth = depth + 1,
                    repository = repository,
                    expandedPaths = expandedPaths,
                    refreshKey = refreshKey,
                    onToggleExpand = onToggleExpand,
                    onFileClick = onFileClick,
                    onLongClick = onLongClick,
                    dirtyPath = dirtyPath,
                )
            }
        }
    }
}

/** 树里的一行：缩进 + 展开箭头 + 图标 + 名字 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRow(
    item: FileItem,
    depth: Int,
    emphasized: Boolean,
    expanded: Boolean?,
    isDirty: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = (8 + depth * 16).dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expanded != null) {
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = stringResource(
                    if (expanded) R.string.tree_collapse else R.string.tree_expand,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            painter = painterResource(
                if (item.isDirectory) R.drawable.ic_folder else R.drawable.ic_file,
            ),
            contentDescription = null,
            tint = if (item.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = if (isDirty) item.name + " *" else item.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isDirty) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        // 右端竖三点：和长按弹出同一个菜单
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.action_more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 树里的提示行：正在加载 / 空文件夹 / 出错 */
@Composable
private fun TreeHintRow(depth: Int, text: String, isError: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (8 + depth * 16 + 24).dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    )
}

/** 长按弹出的菜单 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceItemActionSheet(
    item: FileItem,
    canRemoveFromWorkspace: Boolean,
    onDismiss: () -> Unit,
    onRemoveFromWorkspace: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            )
            HorizontalDivider()
            if (canRemoveFromWorkspace) {
                ActionSheetRow(
                    text = stringResource(R.string.workspace_remove),
                    onClick = onRemoveFromWorkspace,
                )
            }
            ActionSheetRow(
                text = stringResource(R.string.action_rename),
                onClick = onRename,
            )
            HorizontalDivider()
            ActionSheetRow(
                text = stringResource(R.string.action_delete),
                onClick = onDelete,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** 把工作区条目变成一个普通的文件/文件夹信息，好和树里的其它节点统一处理。 */
private fun WorkspaceItem.asFileItem(): FileItem = FileItem(
    // 名字按路径现算：工作区里的文件/文件夹改名后，这里显示的名字会立刻跟上
    name = path.substringAfterLast('/').ifEmpty { WorkspaceItem.ROOT_DISPLAY_NAME },
    path = path,
    isDirectory = type == WorkspaceType.DIRECTORY,
    size = 0L,
    lastModified = 0L,
)
