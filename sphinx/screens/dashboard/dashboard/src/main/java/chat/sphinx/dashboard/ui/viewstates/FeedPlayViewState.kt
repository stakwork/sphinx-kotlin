package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedPlayViewState: ViewState<FeedPlayViewState>() {

    object Idle: FeedPlayViewState()
}