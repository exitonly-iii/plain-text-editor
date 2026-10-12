package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R
import kotlinx.coroutines.delay

/**
 * 编辑区底部的「查找替换」面板（布局仿 VS Code 做了压缩）：
 *
 * - 第一行：折叠箭头 + 查找框 + 上一个 / 下一个 / 关闭
 * - 第二行：Aa / ab / .* / ¶ 四个小开关 + 右侧匹配计数
 * - 替换框默认收起，点第一行开头的箭头才展开
 *
 * 背景与编辑区同为 surface（浅色模式下就是白色），提示语颜色更淡。
 * 触摸目标仍不小于 48dp。
 *
 * @param statusMessage 已经拼好的提示（正则写错 / 匹配过多），没有时为 null
 * @param readOnly 只读模式（大文件）下不允许替换
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
    var replaceExpanded by rememberSaveable { mutableStateOf(false) }
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
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            // ---- 第一行：查找 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { replaceExpanded = !replaceExpanded }) {
                    Icon(
                        imageVector = if (replaceExpanded) {
                            Icons.Filled.KeyboardArrowDown
                        } else {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        },
                        contentDescription = stringResource(
                            if (replaceExpanded) {
                                R.string.action_collapse_replace
                            } else {
                                R.string.action_expand_replace
                            },
                        ),
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.find_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
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

            // ---- 第二行：选项开关 + 匹配情况 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompactToggle(
                    label = stringResource(R.string.option_case_short),
                    description = stringResource(R.string.option_case_sensitive),
                    selected = caseSensitive,
                    onClick = { onCaseSensitiveChange(!caseSensitive) },
                )
                CompactToggle(
                    label = stringResource(R.string.option_whole_word_short),
                    description = stringResource(R.string.option_whole_word),
                    selected = wholeWord,
                    onClick = { onWholeWordChange(!wholeWord) },
                )
                CompactToggle(
                    label = stringResource(R.string.option_regex_short),
                    description = stringResource(R.string.option_regex),
                    selected = useRegex,
                    onClick = { onUseRegexChange(!useRegex) },
                )
                CompactToggle(
                    label = stringResource(R.string.option_multiline_short),
                    description = stringResource(R.string.option_multiline),
                    selected = multiline && useRegex,
                    enabled = useRegex,
                    onClick = { onMultilineChange(!multiline) },
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = counterText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }

            // ---- 替换（默认收起） ----
            if (replaceExpanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replacement,
                    onValueChange = onReplacementChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.replace_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    },
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
            }

            if (readOnly) {
                Text(
                    text = stringResource(R.string.panel_readonly_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
        }
    }
}

/** 查找选项的小开关（Aa / ab / .* / ¶），选中时变主题色 */
@Composable
private fun CompactToggle(
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.semantics { contentDescription = description },
        )
    }
}
