package chat.sphinx.chat_tribe.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ChatTribeViewState: ViewState<ChatTribeViewState>() {
    object Idle: ChatTribeViewState()
}
