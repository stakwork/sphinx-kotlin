package chat.sphinx.chat_group.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ChatGroupViewState: ViewState<ChatGroupViewState>() {
    object Idle: ChatGroupViewState()
}
