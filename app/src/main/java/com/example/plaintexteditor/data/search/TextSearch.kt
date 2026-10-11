package com.example.plaintexteditor.data.search

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * 查找选项。
 *
 * @param query 要找的内容（普通文本或正则表达式）
 * @param replacement 替换成什么（用正则时支持 $1、$2 这样的捕获组）
 * @param useRegex 把 query 当正则表达式
 * @param caseSensitive 区分大小写
 * @param wholeWord 全词匹配（对英文这类有词边界的文字有效）
 * @param multiline 多行模式（只在正则时生效：^ $ 可以匹配每一行的开头结尾）
 */
data class SearchOptions(
    val query: String,
    val replacement: String = "",
    val useRegex: Boolean = false,
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val multiline: Boolean = false,
)

/** 一处匹配在文本里的位置（左闭右开） */
data class SearchMatch(val start: Int, val end: Int)

/** 查找结果 */
data class FindResult(
    val matches: List<SearchMatch> = emptyList(),
    /** 正则表达式写错时的原始说明（界面会翻译成中文提示） */
    val error: String? = null,
    /** 匹配太多或表达式太复杂，结果可能不完整 */
    val timedOut: Boolean = false,
)

/** 替换一处的结果 */
data class ReplaceOneResult(
    val text: String,
    /** 替换完成后，光标应该放在哪 */
    val selection: Int,
    val replaced: Boolean,
)

/** 全部替换的结果 */
data class ReplaceAllResult(
    val text: String,
    val count: Int,
    val error: String? = null,
    val timedOut: Boolean = false,
)

/**
 * 查找与替换的“纯计算”实现：不碰界面和文件，所以可以放心丢到后台线程跑。
 *
 * 两道安全阀，防止正则写出灾难性回溯把手机拖死：
 * 1. 最多记录 MAX_MATCHES 个匹配（超过就不再找了）；
 * 2. 整个查找有 TIME_BUDGET_MS 的时间预算，超时就带着已有结果返回。
 */
object TextSearch {

    /** 最多记录多少个匹配 */
    const val MAX_MATCHES = 5000

    /** 一次查找/替换的时间预算（毫秒） */
    private const val TIME_BUDGET_MS = 2000L

    /** 全部替换时最多替换多少处（再多的文件不适合在手机上整篇替换） */
    private const val MAX_REPLACEMENTS = 20000

    fun find(text: String, options: SearchOptions): FindResult {
        if (options.query.isEmpty()) return FindResult()

        val pattern = try {
            buildPattern(options)
        } catch (syntax: PatternSyntaxException) {
            return FindResult(error = syntax.description ?: syntax.message)
        }

        val matches = ArrayList<SearchMatch>()
        val deadline = System.currentTimeMillis() + TIME_BUDGET_MS
        val matcher = pattern.matcher(text)

        while (true) {
            if (System.currentTimeMillis() > deadline) {
                return FindResult(matches = matches, timedOut = true)
            }
            val found = try {
                matcher.find()
            } catch (throwable: Throwable) {
                // 极少数正则会在运行期抛错，绝不能让它把界面带崩
                return FindResult(matches = matches, error = throwable.message)
            }
            if (!found) break
            matches.add(SearchMatch(start = matcher.start(), end = matcher.end()))
            if (matches.size >= MAX_MATCHES) {
                return FindResult(matches = matches, timedOut = true)
            }
        }
        return FindResult(matches = matches)
    }

    /** 只检查条件本身合不合法（主要是正则语法）；没问题返回 null */
    fun validate(options: SearchOptions): String? = try {
        buildPattern(options)
        null
    } catch (syntax: PatternSyntaxException) {
        syntax.description ?: syntax.message
    }

    /** 替换第 matchIndex 处匹配（从 0 开始数） */
    fun replaceMatch(text: String, matchIndex: Int, options: SearchOptions): ReplaceOneResult {
        val pattern = try {
            buildPattern(options)
        } catch (syntax: PatternSyntaxException) {
            return ReplaceOneResult(text = text, selection = 0, replaced = false)
        }

        val matcher = pattern.matcher(text)
        var index = -1
        while (matcher.find()) {
            index++
            if (index != matchIndex) continue
            val builder = StringBuilder()
            matcher.appendReplacement(builder, replacementFor(options))
            val selection = builder.length
            matcher.appendTail(builder)
            return ReplaceOneResult(text = builder.toString(), selection = selection, replaced = true)
        }
        return ReplaceOneResult(text = text, selection = 0, replaced = false)
    }

    /**
     * 全部替换。
     * 只有整篇都替换完了才返回新文本；中途超时或出错时原样返回旧文本，
     * 绝不返回“替换了一半”的中间结果。
     */
    fun replaceAll(text: String, options: SearchOptions): ReplaceAllResult {
        if (options.query.isEmpty()) return ReplaceAllResult(text = text, count = 0)

        val pattern = try {
            buildPattern(options)
        } catch (syntax: PatternSyntaxException) {
            return ReplaceAllResult(
                text = text,
                count = 0,
                error = syntax.description ?: syntax.message,
            )
        }

        val deadline = System.currentTimeMillis() + TIME_BUDGET_MS
        val matcher = pattern.matcher(text)
        val builder = StringBuilder()
        var count = 0

        while (true) {
            if (System.currentTimeMillis() > deadline || count > MAX_REPLACEMENTS) {
                return ReplaceAllResult(text = text, count = 0, timedOut = true)
            }
            if (!matcher.find()) break
            matcher.appendReplacement(builder, replacementFor(options))
            count++
        }
        matcher.appendTail(builder)
        return ReplaceAllResult(text = builder.toString(), count = count)
    }

    /** 把选项变成 java 的正则对象 */
    private fun buildPattern(options: SearchOptions): Pattern {
        val base = if (options.useRegex) options.query else Pattern.quote(options.query)
        val body = if (options.wholeWord) "\\b(?:$base)\\b" else base
        var flags = 0
        if (!options.caseSensitive) {
            flags = flags or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        }
        if (options.useRegex && options.multiline) {
            flags = flags or Pattern.MULTILINE
        }
        return Pattern.compile(body, flags)
    }

    /** 替换文本：正则替换允许 $1、$2；普通替换里的 $ 按普通字符处理 */
    private fun replacementFor(options: SearchOptions): String =
        if (options.useRegex) options.replacement else Matcher.quoteReplacement(options.replacement)
}
