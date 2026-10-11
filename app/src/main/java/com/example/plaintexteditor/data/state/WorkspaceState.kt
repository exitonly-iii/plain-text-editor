package com.example.plaintexteditor.data.state

import com.example.plaintexteditor.data.model.WorkspaceItem

/**
 * “工作区列表”这一刻的状态。同样只是个数据盒子。
 *
 * 用法示意（下一阶段写界面时）：
 * ```
 * val workspaces by store.workspaces.collectAsStateWithLifecycle()
 * // 或者需要额外信息时：
 * val state = WorkspaceState(workspaces = workspaces, isLoading = false)
 * ```
 *
 * @param workspaces 当前所有工作区（按添加顺序）
 * @param isLoading 是否正在读取（读取 DataStore 是异步的）
 * @param errorMessage 出错时的中文提示；成功时为 null
 */
data class WorkspaceState(
    val workspaces: List<WorkspaceItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    /** 一个工作区都没有 */
    val isEmpty: Boolean get() = workspaces.isEmpty()

    /** 工作区数量 */
    val count: Int get() = workspaces.size

    /** 某个路径是否已经是工作区 */
    fun contains(path: String): Boolean {
        val target = path.trim().trim('/')
        return workspaces.any { it.path == target }
    }
}
