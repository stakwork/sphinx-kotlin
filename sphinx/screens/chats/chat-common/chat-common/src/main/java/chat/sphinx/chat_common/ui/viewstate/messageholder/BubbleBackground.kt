package chat.sphinx.chat_common.ui.viewstate.messageholder

internal sealed class BubbleBackground {
    /**
     * If [setSpacingEqual] is false, will set the spacing based on if
     * it is a received or sent message.
     * */
    data class Gone(val setSpacingEqual: Boolean): BubbleBackground()

    sealed class First: BubbleBackground() {
        object Grouped: First()
        object Isolated: First()
    }

    object Middle: BubbleBackground()

    object Last: BubbleBackground()
}
