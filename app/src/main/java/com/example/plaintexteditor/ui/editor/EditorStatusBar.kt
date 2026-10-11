package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R

/**
 * 底部状态栏。
 * 左边显示“已修改”（有未保存修改时）+ 编码和换行符（真实值），右边显示行数和行号占位。
 *
 * @param isModified 当前文件是否有未保存的修改
 * @param encoding 当前编码的显示名，例如 "UTF-8"
 * @param lineEnding 当前换行符的显示名，例如 "LF"
 * @param totalLines 总行数（还没算出来时传 null）
 * @param currentLine 光标所在行（还没算出来时传 null）
 * @param showCurrentLine 是否显示当前行号（设置里的开关）
 * @param readOnly 是否处于只读模式（大文件）
 */
@Composable
fun EditorStatusBar(
    modifier: Modifier = Modifier,
    isModified: Boolean = false,
    encoding: String,
    lineEnding: String,
    totalLines: Int? = null,
    currentLine: Int? = null,
    showCurrentLine: Boolean = true,
    readOnly: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 让状态栏的文字排在手机底部导航条上方，同时背景色依然铺满
                .navigationBarsPadding()
                .height(32.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isModified) {
                StatusBarLabel(
                    text = stringResource(R.string.status_modified),
                    color = MaterialTheme.colorScheme.primary,
                )
                StatusBarSeparator()
            }
            if (readOnly) {
                StatusBarLabel(
                    text = stringResource(R.string.status_read_only),
                    color = MaterialTheme.colorScheme.error,
                )
                StatusBarSeparator()
            }
            StatusBarLabel(text = encoding)
            StatusBarSeparator()
            StatusBarLabel(text = lineEnding)

            // 把后面的内容挤到右边
            Spacer(modifier = Modifier.weight(1f))

            StatusBarLabel(
                text = totalLines?.let {
                    stringResource(R.string.status_line_count_format, it)
                } ?: stringResource(R.string.status_line_count),
            )
            StatusBarSeparator()
            StatusBarLabel(
                text = if (!showCurrentLine) {
                    stringResource(R.string.status_line_number_hidden)
                } else {
                    currentLine?.let {
                        stringResource(R.string.status_line_number_format, it)
                    } ?: stringResource(R.string.status_line_number_placeholder)
                },
            )
        }
    }
}

@Composable
private fun StatusBarLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        modifier = modifier,
    )
}

/** 两段文字之间的竖线分隔符。 */
@Composable
private fun StatusBarSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(width = 1.dp, height = 12.dp)
            .background(color = MaterialTheme.colorScheme.outlineVariant),
    )
}
