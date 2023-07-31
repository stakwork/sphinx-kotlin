package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.ContactAlias
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ThreadViewState: ViewState<ThreadViewState>() {
    object Idle: ThreadViewState()

    data class ThreadHeader(
        val aliasAndColorKey: Pair<ContactAlias?, String?>,
        val photoUrl: PhotoUrl?,
        val date: String,
        val message: String,
        val isExpanded: Boolean = false
    ): ThreadViewState()


}
