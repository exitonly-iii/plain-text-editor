package com.example.plaintexteditor.data.repository

import com.example.plaintexteditor.data.model.FileItem
import com.example.plaintexteditor.data.text.LargeFileIndex
import com.example.plaintexteditor.data.text.TextContent
import com.example.plaintexteditor.data.text.TextEncoding
import com.example.plaintexteditor.data.text.TextFormat
import com.example.plaintexteditor.data.text.TextFormats
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 文件仓库：所有“跟文件打交道”的事情都由它负责，界面不直接碰文件。
 *
 * 三条重要的规矩：
 * 1. 只操作应用私有目录（filesDir）里面的东西 —— 不需要任何权限。
 * 2. 所有函数都是 suspend（挂起函数），真正的读写都在后台线程（默认 Dispatchers.IO）执行，
 *    不会卡住界面。
 * 3. 每个函数都返回 Result 而不是抛异常：
 *    成功时 result.getOrNull() 拿到数据，失败时 result.exceptionOrNull()?.message 拿到中文原因。
 *
 * 路径约定：所有 path 参数都是**相对私有根目录**的相对路径，"" 表示根目录本身。
 */
class FileRepository(
    private val rootDirectory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    init {
        // 私有根目录正常情况下已经存在，这里只是保险
        if (!rootDirectory.exists()) {
            rootDirectory.mkdirs()
        }
    }

    /** 私有根目录的绝对路径。只用于显示或调试，不要存进数据库。 */
    val rootAbsolutePath: String get() = rootDirectory.canonicalPath

    // ======================================================================
    // 1. 查看
    // ======================================================================

    /**
     * 列出某个目录下的所有文件和文件夹。文件夹排在前面，然后按名字排序（不区分大小写）。
     * path 传 ""（默认值）表示列根目录。
     */
    suspend fun list(path: String = ROOT_PATH): Result<List<FileItem>> = io {
        resolveDirectory(path)
            .listFiles()
            ?.map { it.toFileItem() }
            ?.sortedWith(ITEM_ORDER)
            ?: throw IOException("无法读取目录：${display(path)}")
    }

    /** 取单个文件/文件夹的信息。 */
    suspend fun getItem(path: String): Result<FileItem> = io {
        resolveExisting(path).toFileItem()
    }

    /** 某个路径是否存在。这个函数不会抛错，出错时返回 false。 */
    suspend fun exists(path: String): Boolean =
        io { resolve(path).exists() }.getOrDefault(false)

    // ======================================================================
    // 2. 新建
    // ======================================================================

    /** 在 parentPath 目录下新建一个文件夹。 */
    suspend fun createDirectory(
        parentPath: String = ROOT_PATH,
        name: String,
    ): Result<FileItem> = io {
        val parent = resolveDirectory(parentPath)
        val target = File(parent, checkName(name))
        if (target.exists()) throw IOException("这里已经有叫「$name」的文件或文件夹了，换个名字吧")
        if (!target.mkdirs()) throw IOException("创建文件夹失败：$name")
        target.toFileItem()
    }

    /** 在 parentPath 目录下新建一个文本文件（默认内容为空）。 */
    suspend fun createTextFile(
        parentPath: String = ROOT_PATH,
        name: String,
        content: String = "",
    ): Result<FileItem> = io {
        val parent = resolveDirectory(parentPath)
        val target = File(parent, checkName(name))
        if (target.exists()) throw IOException("这里已经有叫「$name」的文件或文件夹了，换个名字吧")
        if (!target.createNewFile()) throw IOException("创建文件失败：$name")
        if (content.isNotEmpty()) target.writeText(content)
        target.toFileItem()
    }

    // ======================================================================
    // 3. 重命名
    // ======================================================================

    /** 给文件或文件夹改名（保持它原来的位置不变）。 */
    suspend fun rename(path: String, newName: String): Result<FileItem> = io {
        requireNotRoot(path)
        val source = resolveExisting(path)
        val parent = source.parentFile ?: throw IOException("不能重命名根目录")
        val target = File(parent, checkName(newName))
        if (target.canonicalPath != source.canonicalPath) {
            if (target.exists()) throw IOException("这里已经有叫「$newName」的文件或文件夹了，换个名字吧")
            if (!source.renameTo(target)) throw IOException("重命名失败：${source.name}")
        }
        target.toFileItem()
    }

    // ======================================================================
    // 4. 删除
    // ======================================================================

    /** 删除文件或文件夹（文件夹会连同里面的东西一起删掉）。 */
    suspend fun delete(path: String): Result<Unit> = io {
        requireNotRoot(path)
        val target = resolveExisting(path)
        if (!target.deleteRecursively()) throw IOException("删除失败：${display(path)}")
    }

    // ======================================================================
    // 5. 复制 / 移动
    // ======================================================================

    /**
     * 把文件或文件夹复制到 destinationParentPath 目录里。
     * 如果目标目录里已经有同名项目，会自动改名成 "名字 (2)"，不会覆盖任何东西。
     */
    suspend fun copy(
        sourcePath: String,
        destinationParentPath: String = ROOT_PATH,
    ): Result<FileItem> = io {
        requireNotRoot(sourcePath)
        val source = resolveExisting(sourcePath)
        val destinationParent = resolveDirectory(destinationParentPath)
        if (source.isDirectory && isInside(destinationParent, source)) {
            throw IOException("不能把文件夹复制到它自己里面")
        }
        val target = uniqueFile(destinationParent, source.name, source.isDirectory)
        copyRecursively(source, target)
        target.toFileItem()
    }

    /**
     * 把文件或文件夹移动到 destinationParentPath 目录里。
     * 同一个分区下就是改个位置（很快）；不支持时自动退化成“复制 + 删除原文件”，结果一样。
     */
    suspend fun move(
        sourcePath: String,
        destinationParentPath: String = ROOT_PATH,
    ): Result<FileItem> = io {
        requireNotRoot(sourcePath)
        val source = resolveExisting(sourcePath)
        val destinationParent = resolveDirectory(destinationParentPath)
        if (source.parentFile?.canonicalPath == destinationParent.canonicalPath) {
            throw IOException("它已经在这个目录里了")
        }
        if (source.isDirectory && isInside(destinationParent, source)) {
            throw IOException("不能把文件夹移动到它自己里面")
        }
        val target = uniqueFile(destinationParent, source.name, source.isDirectory)
        if (!source.renameTo(target)) {
            // renameTo 失败（例如跨存储区）：退化成复制 + 删除
            copyRecursively(source, target)
            if (!source.deleteRecursively()) {
                target.deleteRecursively() // 回滚，不留下半份副本
                throw IOException("移动失败：${source.name}")
            }
        }
        target.toFileItem()
    }

    // ======================================================================
    // 6. 读写文本内容（统一按 UTF-8 处理）
    // ======================================================================

    /** 读取文本文件的全部内容。 */
    suspend fun readText(path: String): Result<String> = io {
        val file = resolveExisting(path)
        if (file.isDirectory) throw IOException("这是文件夹，不是文件：${display(path)}")
        file.readText()
    }

    /** 把文本写进文件（会覆盖原来内容；文件不存在就创建）。 */
    suspend fun writeText(path: String, content: String): Result<Unit> = io {
        requireNotRoot(path)
        val file = resolve(path)
        if (file.isDirectory) throw IOException("这是文件夹，不能写文字进去：${display(path)}")
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    /**
     * 读文本文件，并且顺便判断它是什么编码、什么换行符。
     *
     * @param overrideEncoding 用户手动指定的编码（传 null 表示“按文件自己的 BOM 判断，没有就 UTF-8”）
     */
    suspend fun readTextFile(
        path: String,
        overrideEncoding: TextEncoding? = null,
    ): Result<TextContent> = io {
        val file = resolveExisting(path)
        if (file.isDirectory) throw IOException("这是文件夹，不是文件：${display(path)}")
        val bytes = file.readBytes()
        val detected = TextFormats.detectFormat(bytes)
        val format = if (overrideEncoding == null) {
            detected
        } else {
            TextFormats.withEncodingOverride(detected, overrideEncoding)
        }
        TextFormats.decode(bytes, format)
    }

    /** 按指定编码和换行符把内容写回文件（“打开时什么样，保存后还是什么样”）。 */
    suspend fun writeTextFile(
        path: String,
        content: String,
        format: TextFormat,
    ): Result<Unit> = io {
        requireNotRoot(path)
        val file = resolve(path)
        if (file.isDirectory) throw IOException("这是文件夹，不能写文字进去：${display(path)}")
        file.parentFile?.mkdirs()
        file.writeBytes(TextFormats.encode(content, format))
    }

    /** 文件大小（字节）；文件夹返回 0 */
    suspend fun fileSize(path: String): Result<Long> = io {
        val file = resolveExisting(path)
        if (file.isDirectory) 0L else file.length()
    }

    /** 读原始字节（导出 / 分享时用，不做任何编码转换） */
    suspend fun readBinaryFile(path: String): Result<ByteArray> = io {
        val file = resolveExisting(path)
        if (file.isDirectory) throw IOException("这是文件夹，不是文件：${display(path)}")
        file.readBytes()
    }

    /** 写原始字节（导入外部文件时用，原样保存，不转码） */
    suspend fun writeBinaryFile(path: String, bytes: ByteArray): Result<Unit> = io {
        requireNotRoot(path)
        val file = resolve(path)
        if (file.isDirectory) throw IOException("这是文件夹，不能写文件进去：${display(path)}")
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    /** 在 parentPath 目录里找一个不会重名的路径（例如导入时原文件名已存在） */
    suspend fun uniquePath(parentPath: String, name: String): Result<String> = io {
        val parent = resolveDirectory(parentPath)
        relativePathOf(uniqueFile(parent, checkName(name), isDirectory = false))
    }

    /**
     * 给大文件建“分块索引”（只读模式用）：只记每个块的字节位置，不把内容读进内存。
     * 要扫一遍文件，会慢一些，注意在后台线程调用。
     */
    suspend fun buildLargeFileIndex(path: String): Result<LargeFileIndex> = io {
        val file = resolveExisting(path)
        if (file.isDirectory) throw IOException("这是文件夹，不是文件：${display(path)}")
        LargeFileIndex.build(file)
    }

    // ======================================================================
    // 内部工具（外部不需要知道）
    // ======================================================================

    /**
     * 把真正的 IO 工作丢到后台线程；顺便把异常包成 Result，而不是让它炸掉整个应用。
     * block 是 suspend 的，所以里面也可以调用别的挂起函数。
     */
    private suspend fun <T> io(block: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation // 协程被取消时要原样抛出去，不能当成普通错误
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    /**
     * 把相对路径变成真实的 File 对象，并且确保它没有跑出私有根目录。
     * （例如传 "../../../etc" 会被直接拦下来，抛 IllegalArgumentException。）
     */
    private fun resolve(path: String): File {
        val cleaned = path.trim().trim('/').replace('\\', '/')
        val candidate = if (cleaned.isEmpty()) rootDirectory else File(rootDirectory, cleaned)
        val rootPath = rootDirectory.canonicalPath
        val targetPath = candidate.canonicalPath
        val inside = targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
        if (!inside) throw IllegalArgumentException("路径必须位于应用私有目录内：$path")
        return File(targetPath)
    }

    private fun resolveExisting(path: String): File {
        val file = resolve(path)
        if (!file.exists()) throw IOException("找不到：${display(path)}")
        return file
    }

    private fun resolveDirectory(path: String): File {
        val dir = resolveExisting(path)
        if (!dir.isDirectory) throw IOException("不是文件夹：${display(path)}")
        return dir
    }

    private fun requireNotRoot(path: String) {
        if (path.isBlank()) throw IOException("这是应用私有根目录，不能对它执行这个操作")
    }

    /** 检查名字是否合法（不能为空、不能带斜杠、不能是 . 或 ..）。 */
    private fun checkName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("名字不能为空")
        if (trimmed == "." || trimmed == "..") throw IllegalArgumentException("名字不合法：$name")
        if (trimmed.contains('/') || trimmed.contains('\\')) {
            throw IllegalArgumentException("名字里不能有斜杠：$name")
        }
        if (trimmed.length > 255) throw IllegalArgumentException("名字太长了")
        return trimmed
    }

    /** 在 parent 目录里找一个不重名的目标：没有重名就用原名，重名就加 " (2)"、" (3)"…… */
    private fun uniqueFile(parent: File, name: String, isDirectory: Boolean): File {
        var candidate = File(parent, name)
        if (!candidate.exists()) return candidate

        val dot = if (isDirectory) -1 else name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""

        var index = 2
        while (candidate.exists()) {
            if (index > 999) throw IOException("目标目录里同名项目太多了")
            candidate = File(parent, "$base ($index)$extension")
            index++
        }
        return candidate
    }

    /** 递归复制（文件夹会连同里面的所有内容一起复制）。 */
    private fun copyRecursively(source: File, target: File) {
        if (source.isDirectory) {
            if (!target.mkdirs() && !target.isDirectory) {
                throw IOException("创建文件夹失败：${target.name}")
            }
            source.listFiles()?.forEach { child ->
                copyRecursively(child, File(target, child.name))
            }
        } else {
            target.parentFile?.mkdirs()
            source.inputStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            target.setLastModified(source.lastModified())
        }
    }

    /** child 是否在 parent 里面（含 parent 自己）。 */
    private fun isInside(child: File, parent: File): Boolean {
        val childPath = child.canonicalPath
        val parentPath = parent.canonicalPath
        return childPath == parentPath || childPath.startsWith(parentPath + File.separator)
    }

    private fun File.toFileItem(): FileItem = FileItem(
        name = name,
        path = relativePathOf(this),
        isDirectory = isDirectory,
        size = if (isDirectory) 0L else length(),
        lastModified = lastModified(),
    )

    private fun relativePathOf(file: File): String {
        val rootPath = rootDirectory.canonicalPath
        val filePath = file.canonicalPath
        if (filePath == rootPath) return ROOT_PATH
        return filePath.removePrefix(rootPath).trimStart(File.separatorChar).replace('\\', '/')
    }

    private fun display(path: String): String =
        if (path.isBlank()) "私有根目录" else path

    companion object {
        /** 空字符串代表私有根目录。 */
        const val ROOT_PATH: String = ""

        /** 排序规则：文件夹在前，然后按名字（忽略大小写，再按原始名字兜底）。 */
        private val ITEM_ORDER: Comparator<FileItem> =
            compareByDescending<FileItem> { it.isDirectory }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.name }
    }
}
