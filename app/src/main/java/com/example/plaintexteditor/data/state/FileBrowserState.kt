package com.example.plaintexteditor.data.state

import com.example.plaintexteditor.data.model.FileItem

/**
 * “文件列表”这一刻的状态。它只是一个数据盒子，不含任何逻辑，
 * 下一阶段的界面会用它来画列表、显示转圈和错误提示。
 *
 * 用法示意（下一阶段写界面时）：
 * ```
 * var state by remember { mutableStateOf(FileBrowserState()) }
 * // 进入某个目录时：
 * state = FileBrowserState(currentPath = path, isLoading = true)
 * state = repository.list(path).fold(
 *     onSuccess = { state.copy(currentPath = path, items = it, isLoading = false) },
 *     onFailure = { state.copy(isLoading = false, errorMessage = it.message) },
 * )
 * ```
 *
 * @param currentPath 当前目录（相对私有根目录；"" 表示根目录）
 * @param items 当前目录下的内容，文件夹已经排在前面
 * @param isLoading 是否正在读取（界面可以显示转圈）
 * @param errorMessage 出错时的中文提示；成功时为 null
 */
data class FileBrowserState(
    val currentPath: String = "",
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** 是否还能返回上一层（已经在根目录就不能了） */
    val canGoUp: Boolean get() = currentPath.isNotEmpty()

    /** 当前目录下是不是空的 */
    val isEmpty: Boolean get() = items.isEmpty()

    /** 只要文件夹 */
    val directories: List<FileItem> get() = items.filter { it.isDirectory }

    /** 只要文件 */
    val files: List<FileItem> get() = items.filter { it.isDirectory.not() }

    /** 是否处在根目录 */
    val isAtRoot: Boolean get() = currentPath.isEmpty()
}
