package chat.sphinx.resources

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

@Suppress("NOTHING_TO_INLINE")
inline fun TextView.setTextColorExt(@ColorRes resId: Int): TextView {
    setTextColor(
        ContextCompat.getColor(context, resId)
    )
    return this
}
