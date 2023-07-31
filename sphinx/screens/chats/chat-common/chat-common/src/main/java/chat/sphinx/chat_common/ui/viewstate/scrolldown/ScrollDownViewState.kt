package chat.sphinx.chat_common.ui.viewstate.scrolldown

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ScrollDownViewState: ViewState<ScrollDownViewState>() {
    data class On(val unseenMessagesCount: String?): ScrollDownViewState()
    object Off: ScrollDownViewState()
}