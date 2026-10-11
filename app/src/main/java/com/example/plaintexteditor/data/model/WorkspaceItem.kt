package com.example.plaintexteditor.data.model

/**
 * 工作区条目的类型。
 */
enum class WorkspaceType {
    /** 文件夹工作区（常见情况：一个目录） */
    DIRECTORY,

    /** 单个文件工作区 */
    FILE,
}

/**
 * 侧边栏“工作区”列表里的一项。
 *
 * 注意：工作区**只记录路径和类型**，不会复制任何文件。
 * 真正的文件始终只有一个，就在应用私有目录里。
 *
 * @param path 相对私有根目录的路径，例如 "notes"（根目录是 ""）
 * @param name 显示用的名字，由路径最后一段推导出来
 * @param type 文件夹还是文件
 */
data class WorkspaceItem(
    val path: String,
    val name: String,
    val type: WorkspaceType,
) {
    companion object {
        /** 根目录的显示名 */
        const val ROOT_DISPLAY_NAME = "私有目录"

        /**
         * 根据路径和类型创建一个工作区条目，名字自动从路径最后一段推导。
         * 例如 fromPath("notes/2026", DIRECTORY) -> name = "2026"
         */
        fun fromPath(path: String, type: WorkspaceType): WorkspaceItem {
            val trimmed = path.trim().trim('/')
            val name = trimmed.substringAfterLast('/').ifEmpty { ROOT_DISPLAY_NAME }
            return WorkspaceItem(path = trimmed, name = name, type = type)
        }
    }
}
