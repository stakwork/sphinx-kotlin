package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ChatListFooterButtonsViewState: ViewState<ChatListFooterButtonsViewState>() {

    object Idle: ChatListFooterButtonsViewState()

    data class ButtonsVisibility(
        val addFriendVisible: Boolean,
        val createTribeVisible: Boolean,
        val discoverTribesVisible: Boolean
    ) : ChatListFooterButtonsViewState()
}