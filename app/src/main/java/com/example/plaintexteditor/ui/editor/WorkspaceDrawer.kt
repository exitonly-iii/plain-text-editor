package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R
import com.example.plaintexteditor.data.model.FileItem
import com.example.plaintexteditor.data.model.WorkspaceItem
import com.example.plaintexteditor.data.repository.FileRepository
import com.example.plaintexteditor.data.workspace.WorkspaceStore

/**
 * 侧边栏（点顶部栏左边的菜单按钮滑出来）。
 *
 * 结构：顶部标题“工作区” → 工作区文件树（中间可滚动）→ 底部“浏览私有目录”按钮 + 设置入口。
 * 工作区列表直接来自 DataStore，所以在文件浏览器里“添加到工作区”之后，这里会立刻出现。
 *
 * @param workspaces 工作区列表（由外面 collect 好了传进来）
 * @param refreshKey 变化时让已展开的文件夹重新读磁盘
 * @param onFileClick 点树里的文件时调用（把路径交给编辑区）
 * @param onMessage 显示一条提示
 * @param onTreeChanged 树里做完重命名/删除后调用，触发刷新
 * @param dirtyPath 当前有未保存修改的文件路径（树里会给它加 *）
 * @param onRemoveWorkspace 请求把某个工作区移出（未保存时由外面提示保存或丢弃）
 * @param onPathRenamed 改名后通知外面，用来同步正在编辑的路径
 */
@Composable
fun WorkspaceDrawer(
    workspaces: List<WorkspaceItem>,
    repository: FileRepository,
    workspaceStore: WorkspaceStore,
    refreshKey: Int,
    dirtyPath: String?,
    onBrowsePrivateDirectoryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onMessage: (String) -> Unit,
    onTreeChanged: () -> Unit,
    onRemoveWorkspace: (FileItem) -> Unit,
    onPathRenamed: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. 顶部标题
            Text(
                text = stringResource(R.string.workspace_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = 24.dp,
                    bottom = 16.dp,
                ),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. 工作区文件树（为空时会显示“暂无工作区…”的提示）
            WorkspaceListContent(
                workspaces = workspaces,
                repository = repository,
                workspaceStore = workspaceStore,
                refreshKey = refreshKey,
                onFileClick = onFileClick,
                onMessage = onMessage,
                onTreeChanged = onTreeChanged,
                dirtyPath = dirtyPath,
                onRemoveWorkspace = onRemoveWorkspace,
                onPathRenamed = onPathRenamed,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 3. 底部：浏览私有目录按钮 + 设置入口
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Button(
                    onClick = onBrowsePrivateDirectoryClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.workspace_browse_private_dir))
                }

                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text(text = stringResource(R.string.action_settings)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                        )
                    },
                    selected = false,
                    onClick = onOpenSettingsClick,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
