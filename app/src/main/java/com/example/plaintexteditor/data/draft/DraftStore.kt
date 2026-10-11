package com.example.plaintexteditor.data.draft

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 草稿仓库：把“还没保存的修改”临时写进应用私有缓存目录的 drafts 子里。
 *
 * 为什么用哈希当文件名：原文件路径里可能有中文、空格、很长的目录名，
 * 直接拿来当文件名会出问题；SHA-256 出来的定长字符串最稳，还不会重名。
 *
 * 注意：草稿放在 cacheDir 下，系统空间紧张时可能被清掉 —— 它是“救命用的临时备份”，
 * 不是正式存储，正式内容永远以你保存的文件为准。
 */
class DraftStore(
    private val draftDirectory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /** 把当前内容写成草稿（会覆盖上一个同名草稿） */
    suspend fun save(path: String, content: String): Result<Unit> = io {
        draftDirectory.mkdirs()
        File(draftDirectory, fileNameOf(path)).writeText(content)
    }

    /** 读草稿；没有草稿时返回 null（不是错误） */
    suspend fun read(path: String): Result<String?> = io {
        val file = File(draftDirectory, fileNameOf(path))
        if (file.isFile) file.readText() else null
    }

    /** 删草稿（用户保存了、或者选择放弃时调用） */
    suspend fun delete(path: String): Result<Unit> = io {
        val file = File(draftDirectory, fileNameOf(path))
        if (file.exists() && !file.delete()) throw IOException("删除草稿失败")
    }

    /** 有没有这个文件的草稿 */
    suspend fun exists(path: String): Boolean =
        io { File(draftDirectory, fileNameOf(path)).isFile }.getOrDefault(false)

    private fun fileNameOf(path: String): String = sha256(path) + ".draft"

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    private suspend fun <T> io(block: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }
}
