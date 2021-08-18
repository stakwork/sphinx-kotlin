package chat.sphinx.chat_common.ui.viewstate.selected

import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.ImageViewCompat
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.wrapper_view.*
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SelectedMessageViewState: ViewState<SelectedMessageViewState>() {

    object None: SelectedMessageViewState()

    @Suppress("DataClassPrivateConstructor")
    data class SelectedMessage private constructor(
        val messageHolderViewState: MessageHolderViewState,
        val holderYPos: Px,
        val bubbleCenterXPos: Px,
        val bubbleHeight: Px,
        val statusHeaderHeight: Px,
        val recyclerViewWidth: Px,
        val showMenuTop: Boolean,
        val imageView: AppCompatImageView? = null,
    ): SelectedMessageViewState() {

        companion object {

            fun instantiate(
                messageHolderViewState: MessageHolderViewState?,

                /* This is the y top position of the holder _within_ the recycler view */
                holderYPosTop: Px,
                holderHeight: Px,
                holderWidth: Px,

                bubbleXPosStart: Px,
                bubbleHeight: Px,
                bubbleWidth: Px,

                headerHeight: Px,

                statusHeaderHeight: Px,

                recyclerViewWidth: Px,

                screenHeight: Px,

                imageView: AppCompatImageView? = null,
            ): SelectedMessage? {
                if (messageHolderViewState == null) {
                    return null
                }

                if (messageHolderViewState.selectionMenuItems.isNullOrEmpty()) {
                    return null
                }

                val spaceTop = holderYPosTop.add(headerHeight)
                val spaceBottom = screenHeight.subtract(spaceTop.add(holderHeight))
                val margin = recyclerViewWidth.subtract(holderWidth).divideBy(2F)

                return SelectedMessage(
                    messageHolderViewState,
                    spaceTop,
                    bubbleWidth.divideBy(2F).add(bubbleXPosStart).add(margin),
                    bubbleHeight,
                    statusHeaderHeight,
                    recyclerViewWidth,
                    spaceTop.isGreaterThanOrEqualTo(spaceBottom),
                    imageView
                )
            }

        }
    }
}
