package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedAllViewState: ViewState<FeedAllViewState>() {

    object Loading: FeedAllViewState()
    object RecommendedList : FeedAllViewState()
    object NoRecommendations: FeedAllViewState()
    object Disabled : FeedAllViewState()
}