package com.odnovolov.forgetmenot.presentation.screen.cardseditor.qaeditor

import android.content.ClipboardManager
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import com.odnovolov.forgetmenot.R

fun EditText.paste() {
    val clipboardText = getClipboardText()
    if (clipboardText == null) {
        Toast.makeText(context, R.string.message_no_paste_data, Toast.LENGTH_SHORT).show()
        return
    }
    val cursorFinalPosition: Int = selStart + clipboardText.length
    setText(
        StringBuilder(text).run {
            if (hasSelection()) {
                replace(selStart, selEnd, clipboardText.toString())
            } else {
                insert(selStart, clipboardText)
            }
        }
    )
    setSelection(cursorFinalPosition)
}

private fun EditText.getClipboardText(): CharSequence? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    if (!clipboard.hasPrimaryClip()) return null
    return clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)
}

private val EditText.selStart get() = minOf(selectionStart, selectionEnd)
private val EditText.selEnd get() = maxOf(selectionStart, selectionEnd)