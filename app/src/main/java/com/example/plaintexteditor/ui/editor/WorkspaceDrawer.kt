package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R

/**
 * 侧边栏（点顶部栏左边的菜单按钮滑出来）。
 * 结构：顶部标题“工作区” → 工作区列表（现在是空的）→ 底部“浏览私有目录”按钮 + 设置入口。
 * 第一阶段列表是空的，按钮和设置也都是占位。
 */
@Composable
fun WorkspaceDrawer(
    onBrowsePrivateDirectoryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
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

            // 2. 工作区列表：第一阶段没有数据，所以居中显示空列表提示
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.workspace_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

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
