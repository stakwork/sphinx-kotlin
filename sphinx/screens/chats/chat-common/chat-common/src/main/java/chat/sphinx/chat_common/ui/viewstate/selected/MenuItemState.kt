package chat.sphinx.chat_common.ui.viewstate.selected

sealed class MenuItemState(
    val showImageIcon: Boolean,
    val sortPriority: Int,
) {

    companion object {
        const val TRUE = true
        const val FALSE = false

        const val PRIORITY_1 = 1
        const val PRIORITY_2 = 2
        const val PRIORITY_3 = 3
        const val PRIORITY_4 = 4
        const val PRIORITY_5 = 5
        const val PRIORITY_6 = 6
        const val PRIORITY_7 = 7
        const val PRIORITY_8 = 8
        const val PRIORITY_9 = 9
    }

    val showTextIcon: Boolean
        get() = !showImageIcon

    object Boost: MenuItemState(TRUE, PRIORITY_1)
    object CopyText: MenuItemState(FALSE, PRIORITY_2)
    object CopyCallLink: MenuItemState(FALSE, PRIORITY_3)
    object CopyLink: MenuItemState(FALSE, PRIORITY_4)
    object Reply: MenuItemState(FALSE, PRIORITY_5)
    object SaveFile: MenuItemState(FALSE, PRIORITY_6)
    object Resend: MenuItemState(FALSE, PRIORITY_7)
    object Flag: MenuItemState(FALSE, PRIORITY_8)
    object Delete: MenuItemState(FALSE, PRIORITY_9)
}
