package com.example.plaintexteditor.ui.editor

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
 * 编辑区（真正的文本输入控件）。
 *
 * 为什么这里用 AndroidView + EditText，而不是 Compose 自带的 TextField？
 * 因为系统的 EditText 才是后续做编辑器（大文件、撤销、光标、选区、输入法细节）最稳的地基。
 * 现在先把它包好，第二阶段只要把文件内容传进来就能直接编辑。
 *
 * @param text 当前文本（由上层保存，控件只负责显示和回调）
 * @param onTextChange 用户每打一个字都会回调一次，把最新文本交回上层
 * @param monospace 是否用等宽字体。默认 false = 用系统默认字体（本阶段的要求）
 */
@Composable
fun PlainTextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    // 始终拿到最新的回调函数，避免控件里存着旧的 lambda
    val currentOnTextChange by rememberUpdatedState(newValue = onTextChange)
    val currentTextColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val density = context.resources.displayMetrics.density
            val horizontalPadding = (16 * density).toInt()
            val verticalPadding = (12 * density).toInt()

            EditText(context).apply {
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setLineSpacing(0f, 1.2f)
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                setTextColor(currentTextColor)

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

            // 只有“外部文本”和“控件里的文本”不一致时才回写内容。
            // 否则用户每打一个字，光标都会被重置。
            // 打开新文件时把光标放到开头。
            if (editText.text.toString() != text) {
                editText.setText(text)
                editText.setSelection(0)
            }
        },
    )
}
