package chat.sphinx.invite_friend.ui

import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class InviteFriendViewState: ViewState<InviteFriendViewState>() {
    object Idle: InviteFriendViewState()

    data class InviteFriendLowestPrice(val price: Sat): InviteFriendViewState()

    object InviteCreationLoading: InviteFriendViewState()
    object InviteCreationFailed: InviteFriendViewState()
    object InviteCreationSucceed: InviteFriendViewState()
}
