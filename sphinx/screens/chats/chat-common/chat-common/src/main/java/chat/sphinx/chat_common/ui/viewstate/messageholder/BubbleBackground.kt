package chat.sphinx.chat_common.ui.viewstate.messageholder

internal sealed class BubbleBackground {

    companion object {
        const val SPACE_WIDTH_MULTIPLE: Float = 0.2F
    }

    object Gone: BubbleBackground()

    sealed class First: BubbleBackground() {
        object Grouped: First()
        object Isolated: First()
    }

    object Middle: BubbleBackground()

    object Last: BubbleBackground()
}
