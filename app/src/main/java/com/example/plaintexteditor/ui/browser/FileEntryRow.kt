package com.example.plaintexteditor.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.plaintexteditor.R
import com.example.plaintexteditor.data.model.FileItem

/**
 * 文件列表里的一行：左边图标，中间名字 + 大小/时间，右边“更多”按钮。
 *
 * 点一下 = 进入文件夹（文件的话暂时没反应，本阶段不做打开文件）
 * 长按   = 弹出操作菜单
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileEntryRow(
    item: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        leadingContent = {
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
            )
        },
        headlineContent = {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = if (item.isDirectory) {
                    formatModifiedTime(item.lastModified)
                } else {
                    "${formatFileSize(item.size)} · ${formatModifiedTime(item.lastModified)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.action_more),
                )
            }
        },
    )
}
