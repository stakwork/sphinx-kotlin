package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ChatListViewState: ViewState<ChatListViewState>() {

    object Idle: ChatListViewState()
}