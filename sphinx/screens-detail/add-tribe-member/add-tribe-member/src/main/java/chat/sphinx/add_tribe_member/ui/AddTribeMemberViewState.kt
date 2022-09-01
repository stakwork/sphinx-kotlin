package chat.sphinx.add_tribe_member.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class AddTribeMemberViewState: ViewState<AddTribeMemberViewState>() {
    object Idle: AddTribeMemberViewState()
}
