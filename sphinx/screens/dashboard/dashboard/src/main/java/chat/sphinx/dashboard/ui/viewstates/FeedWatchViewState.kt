package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedWatchViewState: ViewState<FeedWatchViewState>() {

    object Idle: FeedWatchViewState()
}