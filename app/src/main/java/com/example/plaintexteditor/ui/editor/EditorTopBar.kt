package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * @param onSwitchEncoding 点“更多 → 切换编码…”时调用
 * @param onSwitchLineEnding 点“更多 → 切换换行符…”时调用
 * @param showCurrentLine 是否显示当前行号（菜单里打勾）
 * @param onToggleCurrentLine 点“显示当前行号”时调用
 * @param onOpenSettings 点“设置…”时调用
 * @param canShare 当前是否有文件可以分享
 * @param onShare 点“分享当前文件”时调用
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
    onSwitchEncoding: () -> Unit,
    onSwitchLineEnding: () -> Unit,
    showCurrentLine: Boolean,
    onToggleCurrentLine: () -> Unit,
    onOpenSettings: () -> Unit,
    canShare: Boolean,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var moreMenuOpen by remember { mutableStateOf(false) }

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
            // “更多”菜单：目前放编码 / 换行符切换
            Box {
                IconButton(onClick = { moreMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(
                    expanded = moreMenuOpen,
                    onDismissRequest = { moreMenuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_switch_encoding)) },
                        onClick = {
                            moreMenuOpen = false
                            onSwitchEncoding()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_switch_line_ending)) },
                        onClick = {
                            moreMenuOpen = false
                            onSwitchLineEnding()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_toggle_current_line)) },
                        trailingIcon = {
                            if (showCurrentLine) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                )
                            }
                        },
                        // 故意不关菜单：勾选状态变了能立刻看到
                        onClick = onToggleCurrentLine,
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_share)) },
                        enabled = canShare,
                        onClick = {
                            moreMenuOpen = false
                            onShare()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.settings_title) + "…") },
                        onClick = {
                            moreMenuOpen = false
                            onOpenSettings()
                        },
                    )
                }
            }
        },
    )
}
