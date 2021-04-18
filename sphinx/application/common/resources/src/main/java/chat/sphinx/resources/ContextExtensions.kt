package chat.sphinx.resources

import android.content.Context
import android.view.inputmethod.InputMethodManager
import chat.sphinx.wrapper_view.Dp
import chat.sphinx.wrapper_view.Px

inline val Context.inputMethodManager: InputMethodManager?
    get() = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

@Suppress("NOTHING_TO_INLINE")
inline fun Dp.toPx(context: Context): Px =
    Px(value * context.resources.displayMetrics.density)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.toDp(context: Context): Dp =
    Dp(value / context.resources.displayMetrics.density)
