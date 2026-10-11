package com.example.plaintexteditor.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

/**
 * 导入 / 导出 / 分享的小工具。
 *
 * 全部走系统提供的“文件选择器”和 FileProvider：
 * - 导入：ACTION_OPEN_DOCUMENT（系统让用户挑一个文件，临时授权给我们读）
 * - 导出：ACTION_CREATE_DOCUMENT（系统让用户挑保存位置，临时授权给我们写）
 * - 分享：FileProvider 把私有目录里的文件变成安全的 Uri 交给分享面板
 *
 * 这几种方式【都不需要任何权限】，也不需要访问整个存储。
 */

/** 把私有目录里的文件变成可对外分享的 Uri */
fun privateFileUri(context: Context, rootAbsolutePath: String, relativePath: String): Uri {
    val file = File(rootAbsolutePath, relativePath)
    return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
}

/** 分享文件（系统分享面板）。成功发起返回 true。 */
fun sharePrivateFile(
    context: Context,
    rootAbsolutePath: String,
    relativePath: String,
    mimeType: String = "text/plain",
): Boolean = runCatching {
    val uri = privateFileUri(context, rootAbsolutePath, relativePath)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, relativePath.substringAfterLast('/'))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}.isSuccess

/** 读出系统文件选择器给的那个文件的内容 */
fun readUriBytes(context: Context, uri: Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
}.getOrNull()

/** 把字节写进系统文件选择器给的目标位置 */
fun writeUriBytes(context: Context, uri: Uri, bytes: ByteArray): Boolean = runCatching {
    context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
        stream.write(bytes)
        stream.flush()
    } != null
}.getOrDefault(false)

/** 读出外部文件的名字（例如 "笔记.txt"）；读不到就给个兜底名字 */
fun displayNameOf(context: Context, uri: Uri): String {
    val fallback = "imported.txt"
    return runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use fallback
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) fallback else cursor.getString(index)?.takeIf { it.isNotBlank() } ?: fallback
            } ?: fallback
    }.getOrDefault(fallback)
}
