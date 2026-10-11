package com.example.plaintexteditor.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.data.text.LargeFileIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 大文件的【只读】浏览界面。
 *
 * 做法：文件被切成每 200 行一块，用 LazyColumn 一块一块地显示 ——
 * 只有滚到眼前的那几块才会真正从磁盘读出来，所以 50MB 的文件也不会撑爆内存。
 *
 * 只读但可用：能滚动、能长按选择文字、能复制（SelectionContainer 提供），
 * 就是不能改内容 —— 这正是“大文件保护”想要的效果。
 */
@Composable
fun LargeFileViewer(
    index: LargeFileIndex,
    listState: LazyListState,
    notice: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (notice != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count = index.chunkCount) { chunkIndex ->
                ChunkText(index = index, chunkIndex = chunkIndex)
            }
        }
    }
}

/** 一块文本：滚到眼前才从磁盘读出来 */
@Composable
private fun ChunkText(index: LargeFileIndex, chunkIndex: Int) {
    var text by remember(chunkIndex) { mutableStateOf<String?>(null) }

    LaunchedEffect(chunkIndex) {
        text = withContext(Dispatchers.IO) { index.readChunk(chunkIndex) }
    }

    val content = text
    if (content == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        // SelectionContainer：可以长按选中、复制，但不可编辑
        SelectionContainer {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
