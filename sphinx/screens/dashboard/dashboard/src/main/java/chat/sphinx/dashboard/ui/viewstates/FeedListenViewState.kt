package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedListenViewState: ViewState<FeedListenViewState>() {

    object Idle: FeedListenViewState()
}