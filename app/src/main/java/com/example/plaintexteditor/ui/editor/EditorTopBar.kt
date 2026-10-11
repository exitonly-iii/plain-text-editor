package com.example.plaintexteditor.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.plaintexteditor.R

/**
 * 顶部栏：左边菜单按钮，中间标题，右边“保存 / 查找 / 更多”。
 * 标题显示当前打开的文件名（没打开文件时显示应用名）；
 * 有未保存的修改时，文件名后面会带一个“*”。
 *
 * @param title 中间显示的文字（文件名或应用名）
 * @param isModified 当前文件是否有未保存的修改
 * @param canSave 保存按钮是否可以点（没打开文件、或没有改动时是灰的）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    title: String,
    isModified: Boolean,
    canSave: Boolean,
    onMenuClick: () -> Unit,
    onSaveClick: () -> Unit,
    onFindClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = if (isModified) "$title *" else title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.action_open_drawer),
                )
            }
        },
        actions = {
            TextButton(
                onClick = onSaveClick,
                enabled = canSave,
            ) {
                Text(text = stringResource(R.string.action_save))
            }
            IconButton(onClick = onFindClick) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.action_find),
                )
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.action_more),
                )
            }
        },
    )
}
