package chat.sphinx.chat_common.ui.viewstate.search

import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.PodcastClip
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class MessagesSearchViewState: ViewState<MessagesSearchViewState>() {

    object Idle: MessagesSearchViewState()

    object Clear: MessagesSearchViewState()

    object Cancel: MessagesSearchViewState()

    data class Loading(
        val clearButtonVisible: Boolean
    ): MessagesSearchViewState()

    class Searching(
        val clearButtonVisible: Boolean,
        val messages: List<Message>,
        val index: Int,
        val navigatingForward: Boolean
    ): MessagesSearchViewState()
}