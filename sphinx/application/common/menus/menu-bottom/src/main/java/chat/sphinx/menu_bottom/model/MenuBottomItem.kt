package chat.sphinx.menu_bottom.model

import androidx.annotation.ColorRes
import androidx.annotation.StringRes

interface MenuBottomItem {
    val text: Int
    val textColor: Int?
    val onClick: (() -> Unit)?
}

class MenuBottomOption(
    @StringRes
    override val text: Int,

    @ColorRes
    override val textColor: Int?,

    override val onClick: () -> Unit
): MenuBottomItem {

    companion object {
        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31
    }

    override fun equals(other: Any?): Boolean {
        return  other           is  MenuBottomOption    &&
                other.text      ==  text                &&
                other.textColor ==  textColor
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + text.hashCode()
        result = _31 * result + textColor.hashCode()
        return result
    }

    override fun toString(): String {
        return  "MenuBottomOption(text=$text,textColor=$textColor,onClick=$onClick"
    }
}

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
