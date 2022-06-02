package chat.sphinx.chat_common.ui.viewstate.search

import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.PodcastClip
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class MessagesSearchViewState: ViewState<MessagesSearchViewState>() {

    object Idle: MessagesSearchViewState()

    class Searching(
        val resultsCount: Int,
        val currentMessageFound: Message?,
    ): MessagesSearchViewState()
}