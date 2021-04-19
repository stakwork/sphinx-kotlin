package chat.sphinx.chat_common.ui

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ChatViewState: ViewState<ChatViewState>() {
    object Idle: ChatViewState()
}
