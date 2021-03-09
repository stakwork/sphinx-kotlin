package chat.sphinx.insetter_activity

import android.view.View

fun InsetterActivity.addStatusBarPadding(view: View) {
    view.setPadding(
        view.paddingLeft + statusBarInsetHeight.left,
        view.paddingTop + statusBarInsetHeight.top,
        view.paddingRight + statusBarInsetHeight.right,
        view.paddingBottom + statusBarInsetHeight.bottom
    )
}

fun InsetterActivity.addNavigationBarPadding(view: View) {
    view.setPadding(
        view.paddingLeft + navigationBarInsetHeight.left,
        view.paddingTop + navigationBarInsetHeight.top,
        view.paddingRight + navigationBarInsetHeight.right,
        view.paddingBottom + navigationBarInsetHeight.bottom
    )
}

data class InsetPadding(val left: Int, val right: Int, val top: Int, val bottom: Int)

interface InsetterActivity {
    val statusBarInsetHeight: InsetPadding
    val navigationBarInsetHeight: InsetPadding
}