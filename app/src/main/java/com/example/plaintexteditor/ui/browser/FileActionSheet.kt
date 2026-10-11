package com.example.plaintexteditor.ui.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R
import com.example.plaintexteditor.data.model.FileItem
import com.example.plaintexteditor.data.repository.FileRepository
import com.example.plaintexteditor.ui.common.ActionSheetRow

/**
 * 底部弹出的操作菜单。长按某一行，或点那一行右边的“更多”，都会弹出它。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionSheet(
    item: FileItem,
    onDismiss: () -> Unit,
    onAddToWorkspace: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            )
            HorizontalDivider()
            ActionSheetRow(
                text = stringResource(R.string.action_add_to_workspace),
                onClick = onAddToWorkspace,
            )
            ActionSheetRow(text = stringResource(R.string.action_rename), onClick = onRename)
            ActionSheetRow(text = stringResource(R.string.action_copy), onClick = onCopy)
            ActionSheetRow(text = stringResource(R.string.action_move), onClick = onMove)
            HorizontalDivider()
            ActionSheetRow(
                text = stringResource(R.string.action_delete),
                onClick = onDelete,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * 顶部“新建”按钮弹出的菜单：新建文件夹 / 新建文本文件。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewItemSheet(
    onDismiss: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.action_new),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            )
            HorizontalDivider()
            ActionSheetRow(text = stringResource(R.string.action_new_folder), onClick = onNewFolder)
            ActionSheetRow(text = stringResource(R.string.action_new_file), onClick = onNewFile)
        }
    }
}

/**
 * 选择“复制/移动到哪里”的弹窗：只列文件夹，一层一层点进去，最后点底部的按钮确认。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationPickerSheet(
    title: String,
    confirmText: String,
    repository: FileRepository,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val rootName = stringResource(R.string.browser_title)
    var pickerPath by rememberSaveable { mutableStateOf(FileRepository.ROOT_PATH) }
    var directories by remember { mutableStateOf(emptyList<FileItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pickerPath) {
        isLoading = true
        repository.list(pickerPath).fold(
            onSuccess = { list ->
                directories = list.filter { it.isDirectory }
                errorMessage = null
            },
            onFailure = { errorMessage = it.message },
        )
        isLoading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.picker_here, displayPath(pickerPath, rootName)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
            if (pickerPath.isNotEmpty()) {
                ActionSheetRow(
                    text = stringResource(R.string.browser_up),
                    onClick = { pickerPath = parentPathOf(pickerPath) },
                )
            }
            HorizontalDivider()

            val error = errorMessage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
            ) {
                when {
                    isLoading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    error != null -> Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )

                    directories.isEmpty() -> Text(
                        text = stringResource(R.string.browser_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )

                    else -> LazyColumn {
                        items(directories, key = { it.path }) { directory ->
                            ActionSheetRow(
                                text = directory.name,
                                onClick = { pickerPath = directory.path },
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            Button(
                onClick = { onConfirm(pickerPath) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(text = confirmText)
            }
        }
    }
}
