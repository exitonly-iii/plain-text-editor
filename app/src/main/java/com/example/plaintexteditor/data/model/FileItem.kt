package com.example.plaintexteditor.data.model

/**
 * 一个文件或文件夹的“信息卡片”。
 *
 * 关于 path 的重要约定：
 * 它不是 /data/data/... 那种绝对路径，而是**相对于应用私有根目录**的相对路径，
 * 例如 "notes/日记.txt"；根目录本身用空字符串 "" 表示。
 * 这么做的好处：即使应用被“分身”或数据目录被系统换到别的位置，记录下来的路径依然有效。
 * （绝对路径随时可以从 FileRepository.rootAbsolutePath 取到，但不要保存它。）
 *
 * @param name 显示出来的名字（不含路径），例如 "日记.txt"
 * @param path 相对私有根目录的路径，例如 "notes/日记.txt"
 * @param isDirectory true = 文件夹，false = 文件
 * @param size 文件大小（字节）。文件夹固定为 0
 * @param lastModified 最后修改时间（毫秒时间戳，可直接给 java.util.Date 用）
 */
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
) {
    /** 是不是文件（方便写 if 用） */
    val isFile: Boolean get() = !isDirectory
}
