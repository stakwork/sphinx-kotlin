package chat.sphinx.discover_tribes.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class DiscoverTribesViewState: ViewState<DiscoverTribesViewState>() {
    object Idle: DiscoverTribesViewState()
}
