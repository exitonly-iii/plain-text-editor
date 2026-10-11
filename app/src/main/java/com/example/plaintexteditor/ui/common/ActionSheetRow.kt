package com.example.plaintexteditor.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 底部弹窗里的一行操作：整行可点，高度 ≥ 48dp（手指点得准）。
 * 文件浏览器和侧边栏工作区都用它，保证两边长得一样。
 *
 * @param color 文字颜色，删除这类危险操作传红色
 */
@Composable
fun ActionSheetRow(
    text: String,
    onClick: () -> Unit,
    color: Color = Color.Unspecified,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = text,
                color = color,
            )
        },
    )
}
