package chat.sphinx.tribe_members_list.ui

import chat.sphinx.tribe_members_list.ui.viewstate.TribeMemberHolderViewState
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TribeMembersListViewState: ViewState<TribeMembersListViewState>() {
    abstract val list: List<TribeMemberHolderViewState>

    class ListMode(
        override val list: List<TribeMemberHolderViewState>,
        val loading: Boolean,
        val firstPage: Boolean,
    ): TribeMembersListViewState()
}
