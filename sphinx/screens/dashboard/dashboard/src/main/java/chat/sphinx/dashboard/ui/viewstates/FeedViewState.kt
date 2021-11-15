package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedViewState: ViewState<FeedViewState>() {

    object Default: FeedViewState()
}