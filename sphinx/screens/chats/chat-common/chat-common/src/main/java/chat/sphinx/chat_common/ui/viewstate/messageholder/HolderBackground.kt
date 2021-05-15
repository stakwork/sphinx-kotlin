package chat.sphinx.chat_common.ui.viewstate.messageholder

sealed class HolderBackground {

    companion object {
        const val SPACE_WIDTH_MULTIPLE: Float = 0.2F
    }

    object None: HolderBackground()

    sealed class In: HolderBackground() {
        object First: In()
        object Middle: In()
        object Last: In()
    }

    sealed class Out: HolderBackground() {
        object First: Out()
        object Middle: Out()
        object Last: Out()
    }
}
