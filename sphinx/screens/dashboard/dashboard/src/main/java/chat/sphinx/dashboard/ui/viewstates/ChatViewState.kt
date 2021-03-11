package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ChatViewState: ViewState<ChatViewState>() {
    object Idle: ChatViewState()
}
