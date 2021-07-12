package chat.sphinx.menu_bottom.model

import androidx.annotation.ColorRes
import androidx.annotation.StringRes

interface MenuBottomItem {
    val text: Int
    val textColor: Int?
    val onClick: (() -> Unit)?
}

data class MenuBottomOption(
    @StringRes
    override val text: Int,

    @ColorRes
    override val textColor: Int?,

    override val onClick: () -> Unit
): MenuBottomItem

/**
 * [onClick] defaults to closing the menu, but functionality can be overridden here.
 * */
data class MenuBottomDismiss(
    @StringRes
    override val text: Int,

    @ColorRes
    override val textColor: Int?,

    override val onClick: (() -> Unit)? = null,
): MenuBottomItem
