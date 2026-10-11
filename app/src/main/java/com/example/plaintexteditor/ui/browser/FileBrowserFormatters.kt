package com.example.plaintexteditor.ui.browser

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把字节数变成人能看懂的大小：512 B / 12.3 KB / 1.2 MB / 3.40 GB。
 * （用 1024 进制，和 Windows 文件管理器一致）
 */
fun formatFileSize(bytes: Long): String = when {
    bytes < 0L -> "—"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    else -> String.format(Locale.US, "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

/** 把时间戳变成 "2026-10-11 21:30" 这样的文字。 */
fun formatModifiedTime(millis: Long): String {
    if (millis <= 0L) return "—"
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

/** 路径显示：根目录显示成“私有目录”，其它直接显示相对路径。 */
fun displayPath(path: String, rootName: String): String =
    if (path.isEmpty()) rootName else path

/** 取上一级路径。已经是根目录（""）时返回 ""。 */
fun parentPathOf(path: String): String =
    if (path.isEmpty()) "" else path.substringBeforeLast('/', "")
