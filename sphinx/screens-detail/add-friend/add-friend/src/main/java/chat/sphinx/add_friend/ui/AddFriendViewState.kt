package chat.sphinx.add_friend.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class AddFriendViewState: ViewState<AddFriendViewState>() {
    object Idle: AddFriendViewState()
}
