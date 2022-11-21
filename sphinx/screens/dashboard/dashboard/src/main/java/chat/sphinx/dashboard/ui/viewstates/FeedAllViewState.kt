package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedAllViewState: ViewState<FeedAllViewState>() {

    object Idle: FeedAllViewState()
    object RecommendedList : FeedAllViewState()
}