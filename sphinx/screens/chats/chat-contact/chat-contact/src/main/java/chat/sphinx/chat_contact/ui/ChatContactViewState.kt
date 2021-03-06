package chat.sphinx.chat_contact.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ChatContactViewState: ViewState<ChatContactViewState>() {
    object Idle: ChatContactViewState()
}
