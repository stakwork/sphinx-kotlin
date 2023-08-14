package chat.sphinx.chat_common.ui.viewstate.thread

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.ContactAlias
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ThreadHeaderViewState: ViewState<ThreadHeaderViewState>() {

    object Idle: ThreadHeaderViewState()
    object BasicHeader: ThreadHeaderViewState()

    data class FullHeader(
        val aliasAndColorKey: Pair<ContactAlias?, String?>,
        val photoUrl: PhotoUrl?,
        val date: String,
        val message: String,
    ): ThreadHeaderViewState()

}
