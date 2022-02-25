package chat.sphinx.insetter_activity

import android.view.View

@Suppress("NOTHING_TO_INLINE")
inline fun InsetterActivity.addStatusBarPadding(view: View): InsetterActivity {
    view.setPadding(
        view.paddingLeft + statusBarInsetHeight.left,
        view.paddingTop + statusBarInsetHeight.top,
        view.paddingRight + statusBarInsetHeight.right,
        view.paddingBottom + statusBarInsetHeight.bottom
    )
    return this
}

@Suppress("NOTHING_TO_INLINE")
inline fun InsetterActivity.addNavigationBarPadding(view: View): InsetterActivity {
    view.setPadding(
        view.paddingLeft + navigationBarInsetHeight.left,
        view.paddingTop + navigationBarInsetHeight.top,
        view.paddingRight + navigationBarInsetHeight.right,
        view.paddingBottom + navigationBarInsetHeight.bottom
    )
    return this
}

@Suppress("NOTHING_TO_INLINE")
inline fun InsetterActivity.addKeyboardPadding(view: View): InsetterActivity {
    view.setPadding(
        view.paddingLeft,
        view.paddingTop,
        view.paddingRight,
        keyboardInsetHeight.bottom
    )
    return this
}

data class InsetPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * This is a workaround to using window insets in fragments that are loaded via
 * an activity using motion layout, as the motion layout state needs to be updated
 * for apply insets to be triggered. This specific implementation is viable as the
 * app is locked to portrait.
 * */
interface InsetterActivity {
    val statusBarInsetHeight: InsetPadding
    val navigationBarInsetHeight: InsetPadding
    val keyboardInsetHeight: InsetPadding

    var isKeyboardVisible: Boolean
}