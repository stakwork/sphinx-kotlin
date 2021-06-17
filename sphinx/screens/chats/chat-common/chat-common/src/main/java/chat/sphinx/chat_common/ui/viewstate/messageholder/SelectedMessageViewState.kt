package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_view.Px
import chat.sphinx.wrapper_view.add
import chat.sphinx.wrapper_view.isGreaterThanOrEqualTo
import chat.sphinx.wrapper_view.subtract
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SelectedMessageViewState: ViewState<SelectedMessageViewState>() {

    object None: SelectedMessageViewState()

    class SelectedMessage private constructor(
        val messageHolderViewState: MessageHolderViewState,

        /* The y position of the holder for the screen, not for the recycler view */
        val holderYPos: Px,
        
        val showMenuTop: Boolean,
    ): SelectedMessageViewState() {

        companion object {

            fun instantiate(
                messageHolderViewState: MessageHolderViewState?,

                /* This is the y top position of the holder _within_ the recycler view */
                holderYPosTop: Px,

                holderHeight: Px,
                headerHeight: Px,
                screenHeight: Px,
            ): SelectedMessage? {
                if (messageHolderViewState == null) {
                    return null
                }

                // TODO: if messageHolderViewState has no menu options available, return null

                val spaceTop = holderYPosTop.add(headerHeight)
                val spaceBottom = screenHeight.subtract(spaceTop.add(holderHeight))

                return SelectedMessage(
                    messageHolderViewState,
                    spaceTop,
                    spaceTop.isGreaterThanOrEqualTo(spaceBottom),
                )
            }

        }

        val showMenuBottom: Boolean
            get() = !showMenuTop
    }
}
