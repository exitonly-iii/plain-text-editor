package com.example.plaintexteditor.ui.editor

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 只为了能在光标移动时通知外面；其它行为和普通 EditText 完全一样。
 */
private class CursorTrackingEditText(context: Context) : EditText(context) {

    /** 光标（选区起点）变化时回调，参数是字符偏移量 */
    var onCursorMoved: ((Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onCursorMoved?.invoke(selectionStart)
    }
}

/**
 * 清掉长按浮动菜单里系统 / 输入法附加的项：
 * 常见的是「AI 帮写」（textAssist）与「分享」（shareText），只留全选 / 剪切 / 复制 / 粘贴。
 */
private fun stripExtraSelectionItems(menu: Menu) {
    menu.removeItem(android.R.id.textAssist)
    menu.removeItem(android.R.id.shareText)

    val unwanted = listOf("AI", "帮写", "分享")
    val extraIds = ArrayList<Int>()
    for (index in 0 until menu.size()) {
        val item = menu.getItem(index)
        val title = item.title?.toString().orEmpty()
        if (unwanted.any { title.contains(it, ignoreCase = true) }) {
            extraIds.add(item.itemId)
        }
    }
    extraIds.forEach { menu.removeItem(it) }
}

/**
 * 一次“请把光标 / 选区放到这里”的请求。
 * token 每次变化才会被应用一次，所以不会和用户自己的光标打架。
 */
data class SelectionRequest(
    val token: Int,
    val start: Int,
    val end: Int,
)

/**
 * 编辑区（真正的文本输入控件）。
 *
 * 为什么这里用 AndroidView + EditText，而不是 Compose 自带的 TextField？
 * 因为系统的 EditText 才是后续做编辑器（大文件、撤销、光标、选区、输入法细节）最稳的地基。
 * 现在先把它包好，第二阶段只要把文件内容传进来就能直接编辑。
 *
 * @param text 当前文本（由上层保存，控件只负责显示和回调）
 * @param onTextChange 用户每打一个字都会回调一次，把最新文本交回上层
 * @param monospace 是否用等宽字体。默认 false = 用系统默认字体（本阶段的要求）
 * @param selectionRequest 一次性请求：把光标 / 选区放到指定位置（查找跳转、替换后定位用）
 * @param onCursorOffsetChanged 光标移动时回调（传字符偏移量），用来算“当前在第几行”
 * @param fontSizeSp 字号（sp），来自设置页
 */
@Composable
@Suppress("DEPRECATION")
fun PlainTextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    selectionRequest: SelectionRequest? = null,
    onCursorOffsetChanged: (Int) -> Unit = {},
    fontSizeSp: Float = 16f,
) {
    // 始终拿到最新的回调函数，避免控件里存着旧的 lambda
    val currentOnTextChange by rememberUpdatedState(newValue = onTextChange)
    val currentOnCursorMoved by rememberUpdatedState(newValue = onCursorOffsetChanged)
    val currentTextColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val density = context.resources.displayMetrics.density
            val horizontalPadding = (16 * density).toInt()
            val verticalPadding = (12 * density).toInt()

            CursorTrackingEditText(context).apply {
                onCursorMoved = { offset -> currentOnCursorMoved(offset) }

                // 去掉 EditText 自带的背景（那是一条下划线），让它铺满整个编辑区
                setBackgroundColor(AndroidColor.TRANSPARENT)
                gravity = Gravity.TOP or Gravity.START

                // 多行纯文本，并且关掉输入法的自动纠错/建议
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                setSingleLine(false)

                // 让文字自动换行，而不是横向滚动
                setHorizontallyScrolling(false)
                setVerticalScrollBarEnabled(true)

                // 字体：默认用系统默认字体；想要等宽字体（看代码更整齐）就传 monospace = true
                typeface = if (monospace) Typeface.MONOSPACE else Typeface.DEFAULT
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                setLineSpacing(0f, 1.2f)
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                setTextColor(currentTextColor)

                // 长按选中的浮动菜单：去掉系统/输入法塞进来的「AI 帮写」与「分享」，
                // 只保留全选、剪切、复制、粘贴这些基础操作
                customSelectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        menu?.let { stripExtraSelectionItems(it) }
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        menu?.let { stripExtraSelectionItems(it) }
                        return true
                    }

                    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false

                    override fun onDestroyActionMode(mode: ActionMode?) = Unit
                }

                // 底部留出约 0.9 屏的空白：内容满屏时也能继续往上拉，
                // 光标可以停在上方，而不是被按在最底部（不产生新行）
                addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    val footer = (view.height * 0.9f).toInt()
                    if (view.paddingBottom != footer) {
                        view.setPadding(
                            view.paddingLeft,
                            view.paddingTop,
                            view.paddingRight,
                            footer,
                        )
                    }
                }

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) = Unit

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) = Unit

                    override fun afterTextChanged(s: Editable?) {
                        currentOnTextChange(s?.toString().orEmpty())
                    }
                })
            }
        },
        update = { editText ->
            // 主题换了颜色也要跟着换
            if (editText.currentTextColor != currentTextColor) {
                editText.setTextColor(currentTextColor)
            }

            // 字号变了也要跟着换（换算成像素比较，避免每次重组都重排文字）
            val expectedPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                fontSizeSp,
                editText.resources.displayMetrics,
            )
            if (kotlin.math.abs(editText.textSize - expectedPx) > 0.5f) {
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
            }

            // 只有“外部文本”和“控件里的文本”不一致时才回写内容。
            // 否则用户每打一个字，光标都会被重置。
            // 打开新文件时把光标放到开头。
            if (editText.text.toString() != text) {
                editText.setText(text)
                editText.setSelection(0)
            }

            // 上层要求“把光标 / 选区放到这里”（查找跳转、替换后定位）。
            // 用控件自己的 tag 记住上一次处理过的 token，同一个请求只应用一次。
            val request = selectionRequest
            if (request != null && (editText.tag as? Int) != request.token) {
                editText.tag = request.token
                val length = editText.text.length
                val start = request.start.coerceIn(0, length)
                val end = request.end.coerceIn(0, length)
                editText.requestFocus()
                editText.setSelection(minOf(start, end), maxOf(start, end))
                // 让选中的位置滚到可见区域；个别机型不支持时忽略即可
                runCatching { editText.bringPointIntoView(end) }
            }
        },
    )
}
