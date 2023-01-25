package chat.sphinx.tribes_discover.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class DiscoverTribesViewState: ViewState<DiscoverTribesViewState>() {

    object Loading: DiscoverTribesViewState()

    class Tribes(
        val tribes: List<TribeHolderViewState>,
        val isLastPage: Boolean
    ) : DiscoverTribesViewState()
}
