package chat.sphinx.chat_common.util

import android.text.TextPaint
import android.text.style.URLSpan
import android.view.View
import androidx.annotation.ColorInt

open class LongClickUrlSpan(
    url: String?,
    private val onLongClickListener: View.OnLongClickListener
): URLSpan(url) {
    private var isHighlighted = false

    @ColorInt
    private var highlightColor = 0
    fun onLongClick(widget: View) {
        onLongClickListener.onLongClick(widget)
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.bgColor = highlightColor
        ds.isUnderlineText = !isHighlighted
    }

    fun setHighlighted(highlighted: Boolean, @ColorInt highlightColor: Int) {
        isHighlighted = highlighted
        this.highlightColor = highlightColor
    }

}