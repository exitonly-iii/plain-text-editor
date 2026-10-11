package com.example.plaintexteditor.data.text

import java.io.File
import java.io.RandomAccessFile

/**
 * 大文件的“分块索引”。
 *
 * 打开大文件时在后台把文件流式扫一遍：只记下每 CHUNK_LINES 行的起始【字节】位置，
 * 完全不把文件内容读进内存。之后界面想看哪一块，就只读那一块的字节再解码成文字。
 *
 * 所以 5MB、50MB 的文件，内存占用都很小，滚动也不会卡。
 */
class LargeFileIndex private constructor(
    private val file: File,
    val encoding: TextEncoding,
    val hasBom: Boolean,
    /** 文件总行数（精确，扫描时数出来的） */
    val totalLines: Int,
    /** 每个块起始的字节偏移；第 0 个是跳过 BOM 之后的正文起点 */
    private val chunkStartOffsets: LongArray,
) {

    val chunkCount: Int get() = chunkStartOffsets.size

    val sizeBytes: Long get() = file.length()

    /** 第 chunkIndex 块的第一行是整个文件的第几行（从 1 开始） */
    fun firstLineOfChunk(chunkIndex: Int): Int = chunkIndex * CHUNK_LINES + 1

    /**
     * 读第 chunkIndex 块的内容。
     *
     * 这是【阻塞】调用，请放在后台线程（Dispatchers.IO）里执行。
     * 块边界都落在换行符之后，所以解码时不会切坏多字节字符。
     */
    fun readChunk(chunkIndex: Int): String {
        if (chunkIndex < 0 || chunkIndex >= chunkCount) return ""
        val start = chunkStartOffsets[chunkIndex]
        val end = if (chunkIndex + 1 < chunkCount) chunkStartOffsets[chunkIndex + 1] else sizeBytes
        val length = (end - start).coerceIn(0L, MAX_CHUNK_BYTES.toLong()).toInt()
        if (length <= 0) return ""

        val bytes = ByteArray(length)
        RandomAccessFile(file, "r").use { access ->
            access.seek(start)
            access.readFully(bytes)
        }
        return String(bytes, encoding.charset)
    }

    companion object {
        /** 每个块多少行（大约一到两屏） */
        const val CHUNK_LINES = 200

        /** 单个块最多读多少字节，防止“一行超长”把内存撑爆 */
        private const val MAX_CHUNK_BYTES = 512 * 1024

        /** 扫描时的缓冲区大小 */
        private const val SCAN_BUFFER_BYTES = 128 * 1024

        /**
         * 流式扫描文件建立索引。会同步做 IO，请在后台线程调用。
         */
        fun build(file: File): LargeFileIndex {
            val detected = detectEncoding(file)
            val encoding = detected.encoding
            val unit = unitSizeOf(encoding)
            val contentStart = if (detected.hasBom) bomLengthOf(encoding) else 0L
            val newline = newlinePatternOf(encoding)

            val chunkStarts = ArrayList<Long>()
            chunkStarts.add(contentStart)

            var totalLines = 1
            var linesInChunk = 0
            val buffer = ByteArray(SCAN_BUFFER_BYTES)
            var carry = 0

            RandomAccessFile(file, "r").use { access ->
                access.seek(contentStart)
                while (true) {
                    val read = access.read(buffer, carry, buffer.size - carry)
                    if (read <= 0) break
                    val total = carry + read

                    // 只处理到“留得下一个换行序列”的位置，剩下的挪到下一轮判断
                    val safeEnd = total - (newline.size - 1)
                    var consumed = if (safeEnd > 0) (safeEnd / unit) * unit else 0
                    if (consumed < 0) consumed = 0

                    var index = 0
                    while (index + newline.size <= total && index < consumed) {
                        if (matchesAt(buffer, index, newline)) {
                            totalLines++
                            linesInChunk++
                            if (linesInChunk >= CHUNK_LINES) {
                                chunkStarts.add(contentStart + index + newline.size)
                                linesInChunk = 0
                            }
                        }
                        index += unit
                    }

                    carry = total - consumed
                    if (carry > 0) {
                        System.arraycopy(buffer, consumed, buffer, 0, carry)
                    }
                }
            }

            return LargeFileIndex(
                file = file,
                encoding = encoding,
                hasBom = detected.hasBom,
                totalLines = totalLines,
                chunkStartOffsets = chunkStarts.toLongArray(),
            )
        }

        /** 只看开头 4 个字节判断编码（和普通文件的规则完全一致） */
        private fun detectEncoding(file: File): TextFormat {
            val head = ByteArray(4)
            val read = RandomAccessFile(file, "r").use { access -> access.read(head).coerceAtLeast(0) }
            val bytes = if (read == head.size) head else head.copyOf(read)
            return TextFormats.detectFormat(bytes)
        }

        private fun unitSizeOf(encoding: TextEncoding): Int = when (encoding) {
            TextEncoding.UTF_8 -> 1
            TextEncoding.UTF_16_LE, TextEncoding.UTF_16_BE -> 2
            TextEncoding.UTF_32_LE, TextEncoding.UTF_32_BE -> 4
        }

        private fun bomLengthOf(encoding: TextEncoding): Long = when (encoding) {
            TextEncoding.UTF_8 -> 3L
            TextEncoding.UTF_16_LE, TextEncoding.UTF_16_BE -> 2L
            TextEncoding.UTF_32_LE, TextEncoding.UTF_32_BE -> 4L
        }

        private fun newlinePatternOf(encoding: TextEncoding): ByteArray = when (encoding) {
            TextEncoding.UTF_8 -> byteArrayOf(0x0A)
            TextEncoding.UTF_16_LE -> byteArrayOf(0x0A, 0x00)
            TextEncoding.UTF_16_BE -> byteArrayOf(0x00, 0x0A)
            TextEncoding.UTF_32_LE -> byteArrayOf(0x0A, 0x00, 0x00, 0x00)
            TextEncoding.UTF_32_BE -> byteArrayOf(0x00, 0x00, 0x00, 0x0A)
        }

        private fun matchesAt(buffer: ByteArray, offset: Int, pattern: ByteArray): Boolean {
            for (index in pattern.indices) {
                if (buffer[offset + index] != pattern[index]) return false
            }
            return true
        }
    }
}
