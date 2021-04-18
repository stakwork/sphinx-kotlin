package chat.sphinx.resources

import android.view.View
import android.widget.TextView
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
 * set it's tint to the random, then set the view's background
 * to that drawable.
 *
 * Other wise it sets the view's background to the random color.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun View.setBackgroundRandomColor(@DrawableRes resId: Int?) {
    val randomColorResource = listOf<@ColorRes Int>(
        R.color.randomColor1,
        R.color.randomColor2,
        R.color.randomColor3,
        R.color.randomColor4,
        R.color.randomColor5,
        R.color.randomColor6,
        R.color.randomColor7,
        R.color.randomColor8,
        R.color.randomColor9,
        R.color.randomColor10,
        R.color.randomColor11,
        R.color.randomColor12,
        R.color.randomColor13,
        R.color.randomColor14,
        R.color.randomColor15,
        R.color.randomColor16,
        R.color.randomColor17,
        R.color.randomColor18,
        R.color.randomColor19,
        R.color.randomColor20,
    ).shuffled()[0]

    val color = ContextCompat.getColor(this.context, randomColorResource)

    if (resId != null) {
        val drawable = ContextCompat.getDrawable(this.context, resId)
        drawable?.setTint(color)
        this.background = drawable
    } else {
        this.setBackgroundColor(color)
    }

}
