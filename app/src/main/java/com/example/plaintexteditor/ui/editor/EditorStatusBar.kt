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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R

/**
 * 底部状态栏。
 * 左边显示编码和换行符，右边显示行数和行号 —— 第一阶段全部是占位文字。
 */
@Composable
fun EditorStatusBar(modifier: Modifier = Modifier) {
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
            StatusBarLabel(text = stringResource(R.string.status_encoding))
            StatusBarSeparator()
            StatusBarLabel(text = stringResource(R.string.status_line_ending))

            // 把后面的内容挤到右边
            Spacer(modifier = Modifier.weight(1f))

            StatusBarLabel(text = stringResource(R.string.status_line_count))
            StatusBarSeparator()
            StatusBarLabel(text = stringResource(R.string.status_cursor_position))
        }
    }
}

@Composable
private fun StatusBarLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
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
