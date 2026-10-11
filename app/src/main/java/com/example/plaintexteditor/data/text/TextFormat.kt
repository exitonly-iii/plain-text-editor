package com.example.plaintexteditor.data.text

import java.nio.charset.Charset

/**
 * 支持的编码。默认一律按 UTF-8 处理，只有检测到 BOM 时才用别的。
 */
enum class TextEncoding(val charsetName: String, val displayName: String) {
    UTF_8("UTF-8", "UTF-8"),
    UTF_16_LE("UTF-16LE", "UTF-16 LE"),
    UTF_16_BE("UTF-16BE", "UTF-16 BE"),
    UTF_32_LE("UTF-32LE", "UTF-32 LE"),
    UTF_32_BE("UTF-32BE", "UTF-32 BE"),
    ;

    /** 真正的字符集对象；万一系统不支持就退回 UTF-8，绝不崩 */
    val charset: Charset
        get() = runCatching { Charset.forName(charsetName) }.getOrDefault(Charsets.UTF_8)
}

/** 换行符：LF（Unix/Android 常用）或 CRLF（Windows 常用） */
enum class LineEnding(val displayName: String, val sequence: String) {
    LF("LF", "\n"),
    CRLF("CRLF", "\r\n"),
}

/**
 * 一个文本文件的“格式三件套”：编码、有没有 BOM、换行符。
 * 保存时会按它写回去，所以“打开时是什么样，保存后还是什么样”。
 */
data class TextFormat(
    val encoding: TextEncoding = TextEncoding.UTF_8,
    val hasBom: Boolean = false,
    val lineEnding: LineEnding = LineEnding.LF,
)

/** 读一个文件得到的东西：内容 + 它的格式 */
data class TextContent(
    val text: String,
    val format: TextFormat,
)

/**
 * 编码 / 换行符的“纯计算”工具：不碰文件系统，方便单独理解和测试。
 */
object TextFormats {

    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val UTF32_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00)
    private val UTF32_BE_BOM = byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte())
    private val UTF16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    private val UTF16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())

    /**
     * 看开头的几个字节，判断这是什么编码。
     * 注意顺序：UTF-32 LE 的 BOM 前两个字节和 UTF-16 LE 一样，所以必须先看 4 字节的。
     * 认不出来就一律当 UTF-8（这也是绝大多数纯文本文件的情况）。
     */
    fun detectFormat(bytes: ByteArray): TextFormat {
        if (bytes.startsWith(UTF32_LE_BOM)) return TextFormat(TextEncoding.UTF_32_LE, hasBom = true)
        if (bytes.startsWith(UTF32_BE_BOM)) return TextFormat(TextEncoding.UTF_32_BE, hasBom = true)
        if (bytes.startsWith(UTF8_BOM)) return TextFormat(TextEncoding.UTF_8, hasBom = true)
        if (bytes.startsWith(UTF16_LE_BOM)) return TextFormat(TextEncoding.UTF_16_LE, hasBom = true)
        if (bytes.startsWith(UTF16_BE_BOM)) return TextFormat(TextEncoding.UTF_16_BE, hasBom = true)
        return TextFormat(TextEncoding.UTF_8, hasBom = false)
    }

    /** 把字节按格式解成文字（去掉 BOM），顺便检测换行符 */
    fun decode(bytes: ByteArray, format: TextFormat): TextContent {
        val body = if (format.hasBom) bytes.dropBom(format.encoding) else bytes
        val text = String(body, format.encoding.charset)
        return TextContent(
            text = text,
            format = format.copy(lineEnding = detectLineEnding(text)),
        )
    }

    /** 把文字按格式变回字节：统一换行符，需要的话加上 BOM */
    fun encode(text: String, format: TextFormat): ByteArray {
        val normalized = convertLineEndings(text, format.lineEnding)
        val body = normalized.toByteArray(format.encoding.charset)
        return if (format.hasBom) bomOf(format.encoding) + body else body
    }

    /** 检测换行符：看先出现的是 "\r\n" 还是单独的 "\n" */
    fun detectLineEnding(text: String): LineEnding {
        val crlf = text.indexOf("\r\n")
        if (crlf < 0) return LineEnding.LF
        val lf = text.indexOf('\n')
        return if (lf in 0 until crlf) LineEnding.LF else LineEnding.CRLF
    }

    /** 把文字里的换行符统一成指定的那一种 */
    fun convertLineEndings(text: String, lineEnding: LineEnding): String {
        val unified = text.replace("\r\n", "\n").replace("\r", "\n")
        return if (lineEnding == LineEnding.LF) unified else unified.replace("\n", "\r\n")
    }

    /** 用户手动选编码时，决定还要不要保留 BOM：只有当选手的编码和检测结果一致时才保留 */
    fun withEncodingOverride(detected: TextFormat, override: TextEncoding): TextFormat =
        TextFormat(
            encoding = override,
            hasBom = detected.hasBom && detected.encoding == override,
            lineEnding = detected.lineEnding,
        )

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (index in prefix.indices) {
            if (this[index] != prefix[index]) return false
        }
        return true
    }

    private fun ByteArray.dropBom(encoding: TextEncoding): ByteArray = when (encoding) {
        TextEncoding.UTF_8 -> if (startsWith(UTF8_BOM)) copyOfRange(3, size) else this
        TextEncoding.UTF_16_LE -> if (startsWith(UTF16_LE_BOM)) copyOfRange(2, size) else this
        TextEncoding.UTF_16_BE -> if (startsWith(UTF16_BE_BOM)) copyOfRange(2, size) else this
        TextEncoding.UTF_32_LE -> if (size >= 4) copyOfRange(4, size) else ByteArray(0)
        TextEncoding.UTF_32_BE -> if (size >= 4) copyOfRange(4, size) else ByteArray(0)
    }

    private fun bomOf(encoding: TextEncoding): ByteArray = when (encoding) {
        TextEncoding.UTF_8 -> UTF8_BOM
        TextEncoding.UTF_16_LE -> UTF16_LE_BOM
        TextEncoding.UTF_16_BE -> UTF16_BE_BOM
        TextEncoding.UTF_32_LE -> UTF32_LE_BOM
        TextEncoding.UTF_32_BE -> UTF32_BE_BOM
    }
}
