package chat.sphinx.chat_common.ui.viewstate.thread

import androidx.annotation.IntRange
import chat.sphinx.chat_common.ui.viewstate.messageholder.LayoutState
import chat.sphinx.chat_common.ui.viewstate.messageholder.MessageHolderViewState
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.ContactAlias
import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

sealed class ThreadHeaderViewState: ViewState<ThreadHeaderViewState>() {

    object Idle: ThreadHeaderViewState()
    object BasicHeader: ThreadHeaderViewState()

    data class FullHeader(
        val senderInfo: Triple<PhotoUrl?, ContactAlias?, String>?,
        val date: String,
        val message: String?,
        val highlightedTexts: List<Pair<String, IntRange>>? = emptyList(),
        val imageAttachment: LayoutState.Bubble.ContainerSecond.ImageAttachment? = null,
        val videoAttachment: LayoutState.Bubble.ContainerSecond.VideoAttachment? = null,
        val fileAttachment: LayoutState.Bubble.ContainerSecond.FileAttachment? = null,
        val audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment? = null
    ): ThreadHeaderViewState()

}
