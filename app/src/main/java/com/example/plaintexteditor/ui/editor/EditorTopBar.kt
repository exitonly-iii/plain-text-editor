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
 * 第一阶段这些按钮都只是占位，点了不会有任何动作。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
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
                text = stringResource(R.string.app_name),
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
            TextButton(onClick = onSaveClick) {
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
