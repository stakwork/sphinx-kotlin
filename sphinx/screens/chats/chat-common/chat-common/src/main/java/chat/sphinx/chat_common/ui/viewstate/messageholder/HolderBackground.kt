package chat.sphinx.chat_common.ui.viewstate.messageholder

sealed class HolderBackground {

    companion object {
        const val SPACE_WIDTH_MULTIPLE: Float = 0.2F
    }

    object None: HolderBackground()

    sealed class First: HolderBackground() {
        object Grouped: First()
        object Isolated: First()
    }

    object Middle: HolderBackground()

    object Last: HolderBackground()
}
