package chat.sphinx.resources

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import chat.sphinx.wrapper_view.Dp
import chat.sphinx.wrapper_view.Px
import chat.sphinx.wrapper_view.Sp

inline val Context.inputMethodManager: InputMethodManager?
    get() = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

@ColorInt
@Suppress("NOTHING_TO_INLINE")
inline fun Context.getRandomColor(): Int {
    return ContextCompat.getColor(this, getRandomColorRes())
}

inline fun Context.getRandomHexCode(): String {
    return "#" + Integer.toHexString(
        getRandomColor()
    )
}

@ColorRes
@Suppress("NOTHING_TO_INLINE")
inline fun Context.getRandomColorRes(): Int {
    return listOf(
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
}

@Suppress("NOTHING_TO_INLINE")
inline fun Dp.toPx(context: Context): Px =
    Px(value * context.resources.displayMetrics.density)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.toDp(context: Context): Dp =
    Dp(value / context.resources.displayMetrics.density)

@Suppress("NOTHING_TO_INLINE")
inline fun Sp.toPx(context: Context): Px =
    Px(value * context.resources.displayMetrics.scaledDensity)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.toSp(context: Context): Sp =
    Sp(value / context.resources.displayMetrics.scaledDensity)

@Suppress("NOTHING_TO_INLINE")
@Throws(Resources.NotFoundException::class)
inline fun ViewBinding.getString(@StringRes stringRes: Int): String =
    root.context.resources.getString(stringRes)

@ColorInt
@Suppress("NOTHING_TO_INLINE")
@Throws(Resources.NotFoundException::class)
inline fun ViewBinding.getColor(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(root.context, colorRes)
}

@Suppress("NOTHING_TO_INLINE")
@Throws(Resources.NotFoundException::class)
inline fun ViewBinding.getColorStateList(@ColorRes colorRes: Int): ColorStateList? {
    return ContextCompat.getColorStateList(root.context, colorRes)
}

@Suppress("NOTHING_TO_INLINE")
@Throws(Resources.NotFoundException::class)
inline fun ViewBinding.getColorHexCode(@ColorRes colorRes: Int): String {
    return "#${Integer.toHexString(
        ContextCompat.getColor(root.context, colorRes) and 0xffffff
    )}"
}
