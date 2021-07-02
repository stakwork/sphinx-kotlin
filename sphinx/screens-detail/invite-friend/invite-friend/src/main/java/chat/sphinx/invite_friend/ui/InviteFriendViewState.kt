package chat.sphinx.invite_friend.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class InviteFriendViewState: ViewState<InviteFriendViewState>() {
    object Idle: InviteFriendViewState()
}
