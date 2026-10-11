package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R
import kotlinx.coroutines.delay

/**
 * 编辑区底部的「查找替换」面板。
 *
 * 放在编辑区下方、状态栏上方；因为整个页面用了 imePadding，
 * 所以键盘弹出来时它会跟着被顶上去，不会被挡住。
 *
 * 所有按钮都是 Material 3 的标准控件，触摸目标都在 48dp 以上。
 *
 * @param statusMessage 已经拼好的提示（正则写错 / 匹配过多），没有时为 null
 * @param readOnly 只读模式（大文件）下不允许替换 —— 本阶段先留好接口，一直传 false
 */
@Composable
fun FindReplacePanel(
    query: String,
    onQueryChange: (String) -> Unit,
    replacement: String,
    onReplacementChange: (String) -> Unit,
    useRegex: Boolean,
    onUseRegexChange: (Boolean) -> Unit,
    caseSensitive: Boolean,
    onCaseSensitiveChange: (Boolean) -> Unit,
    wholeWord: Boolean,
    onWholeWordChange: (Boolean) -> Unit,
    multiline: Boolean,
    onMultilineChange: (Boolean) -> Unit,
    matchCount: Int,
    currentIndex: Int,
    isSearching: Boolean,
    statusMessage: String?,
    readOnly: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // 打开面板就把光标放进查找框，键盘自动弹出
        delay(120)
        runCatching { focusRequester.requestFocus() }
    }

    val counterText = when {
        isSearching -> stringResource(R.string.find_searching)
        statusMessage != null -> statusMessage
        matchCount == 0 -> stringResource(R.string.find_no_match)
        else -> stringResource(R.string.find_match_counter, currentIndex + 1, matchCount)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // ---- 第一行：查找 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    placeholder = { Text(text = stringResource(R.string.find_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onNext() }),
                )
                IconButton(onClick = onPrevious, enabled = matchCount > 0) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.action_find_previous),
                    )
                }
                IconButton(onClick = onNext, enabled = matchCount > 0) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_find_next),
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_close),
                    )
                }
            }

            // ---- 匹配情况 ----
            Text(
                text = counterText,
                style = MaterialTheme.typography.bodySmall,
                color = if (statusMessage != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )

            // ---- 第二行：替换 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replacement,
                    onValueChange = onReplacementChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(text = stringResource(R.string.replace_hint)) },
                )
                TextButton(
                    onClick = onReplace,
                    enabled = !readOnly && matchCount > 0,
                ) {
                    Text(text = stringResource(R.string.action_replace))
                }
                TextButton(
                    onClick = onReplaceAll,
                    enabled = !readOnly && matchCount > 0,
                ) {
                    Text(text = stringResource(R.string.action_replace_all))
                }
            }

            if (readOnly) {
                Text(
                    text = stringResource(R.string.panel_readonly_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ---- 第三行：选项（窄屏可以横向滑动） ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = useRegex,
                    onClick = { onUseRegexChange(!useRegex) },
                    label = { Text(text = stringResource(R.string.option_regex)) },
                )
                FilterChip(
                    selected = caseSensitive,
                    onClick = { onCaseSensitiveChange(!caseSensitive) },
                    label = { Text(text = stringResource(R.string.option_case_sensitive)) },
                )
                FilterChip(
                    selected = wholeWord,
                    onClick = { onWholeWordChange(!wholeWord) },
                    label = { Text(text = stringResource(R.string.option_whole_word)) },
                )
                FilterChip(
                    // 多行模式只在正则有意义，所以不是正则时禁用
                    selected = multiline && useRegex,
                    enabled = useRegex,
                    onClick = { onMultilineChange(!multiline) },
                    label = { Text(text = stringResource(R.string.option_multiline)) },
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
