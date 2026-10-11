package com.example.plaintexteditor.data.search

import com.example.plaintexteditor.data.text.LargeFileIndex
import com.example.plaintexteditor.data.text.LineCounter
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

/** 大文件里的一处匹配 */
data class LargeMatch(
    val chunkIndex: Int,
    val lineNumber: Int,
    val startInChunk: Int,
    val endInChunk: Int,
    val snippet: String,
)

data class LargeSearchResult(
    val matches: List<LargeMatch> = emptyList(),
    val error: String? = null,
    val timedOut: Boolean = false,
)

/**
 * 大文件的流式查找：一块一块地读、一块一块地找，【从不把整个文件读进内存】。
 *
 * 匹配规则和编辑器里的查找完全一样（普通 / 正则 / 大小写 / 全词），
 * 只是结果记的是“第几块 + 块内位置 + 第几行 + 一小段上下文”，这样点结果就能直接跳过去。
 */
object LargeFileSearch {

    /** 大文件最多记录多少处匹配 */
    const val MAX_MATCHES = 1000

    /** 整次扫描的时间预算（毫秒） */
    private const val TIME_BUDGET_MS = 8_000L

    /** 上下文片段多长 */
    private const val SNIPPET_LENGTH = 60

    /**
     * 流式查找（挂起函数，请在协程里调用）。
     * 查询条件一变，外面的 LaunchedEffect 会取消这个协程，扫描立刻停下。
     *
     * @param onProgress 每扫完一块回调一次（已扫块数）
     */
    suspend fun search(
        index: LargeFileIndex,
        options: SearchOptions,
        onProgress: ((Int) -> Unit)? = null,
    ): LargeSearchResult {
        if (options.query.isEmpty()) return LargeSearchResult()

        // 先只检查条件本身：正则写错就在这里返回，不白扫一遍文件
        TextSearch.validate(options)?.let { return LargeSearchResult(error = it) }

        val matches = ArrayList<LargeMatch>()
        val deadline = System.currentTimeMillis() + TIME_BUDGET_MS

        for (chunkIndex in 0 until index.chunkCount) {
            coroutineContext.ensureActive()
            if (System.currentTimeMillis() > deadline) {
                return LargeSearchResult(matches = matches, timedOut = true)
            }

            val text = index.readChunk(chunkIndex)
            if (text.isNotEmpty()) {
                val found = TextSearch.find(text, options)
                if (found.error != null) {
                    return LargeSearchResult(matches = matches, error = found.error)
                }
                val baseLine = index.firstLineOfChunk(chunkIndex)
                for (match in found.matches) {
                    val lineInChunk = LineCounter.lineOfOffset(text, match.start)
                    matches.add(
                        LargeMatch(
                            chunkIndex = chunkIndex,
                            lineNumber = baseLine + lineInChunk - 1,
                            startInChunk = match.start,
                            endInChunk = match.end,
                            snippet = snippetOf(text, match),
                        ),
                    )
                    if (matches.size >= MAX_MATCHES) {
                        return LargeSearchResult(matches = matches, timedOut = true)
                    }
                }
            }
            onProgress?.invoke(chunkIndex + 1)
        }
        return LargeSearchResult(matches = matches)
    }

    private fun snippetOf(text: String, match: SearchMatch): String {
        val start = (match.start - 20).coerceAtLeast(0)
        val end = (match.start + SNIPPET_LENGTH).coerceAtMost(text.length)
        return text.substring(start, end)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
    }
}
