package com.example.plaintexteditor.data.text

/**
 * 行号计算。两个函数都很轻，可以放心丢到后台线程跑。
 *
 * 这里的“行”指**逻辑行**（按 \n 数出来的行），编辑器自动换行不会多算，
 * 也就是“第几个段落”。
 */
object LineCounter {

    /** 总行数：数 \n 的个数再加 1（空文本算 1 行） */
    fun countLines(text: String): Int {
        if (text.isEmpty()) return 1
        var lines = 1
        var index = text.indexOf('\n')
        while (index >= 0) {
            lines++
            index = text.indexOf('\n', index + 1)
        }
        return lines
    }

    /** 光标落在第几行（从 1 开始）；offset 会被限制在 0..text.length 之间 */
    fun lineOfOffset(text: String, offset: Int): Int {
        val safeOffset = offset.coerceIn(0, text.length)
        var line = 1
        var index = text.indexOf('\n')
        while (index >= 0 && index < safeOffset) {
            line++
            index = text.indexOf('\n', index + 1)
        }
        return line
    }
}
