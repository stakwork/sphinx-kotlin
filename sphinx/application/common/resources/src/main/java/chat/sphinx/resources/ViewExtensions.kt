package chat.sphinx.resources

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

@Suppress("NOTHING_TO_INLINE")
inline fun TextView.setTextColorExt(@ColorRes resId: Int): TextView {
    setTextColor(
        ContextCompat.getColor(context, resId)
    )
    return this
}

/**
 * Providing a Drawable [resId] will retrieve the drawable and
 * set it's tint to the random color, then set the view's background
 * to that drawable.
 *
 * Other wise it sets the view's background to the random color.
 *
 * Alternatively, you can pass a value for [color] which provides
 * flexibility if you want to use a specific random color throughout
 * a given screen.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun View.setBackgroundRandomColor(
    @DrawableRes resId: Int?,
    @ColorInt color: Int? = null
) {
    if (resId != null) {
        val drawable = ContextCompat.getDrawable(this.context, resId)
        drawable?.setTint(color ?: this.context.getRandomColor())
        this.background = drawable
    } else {
        this.setBackgroundColor(color ?: this.context.getRandomColor())
    }
}
