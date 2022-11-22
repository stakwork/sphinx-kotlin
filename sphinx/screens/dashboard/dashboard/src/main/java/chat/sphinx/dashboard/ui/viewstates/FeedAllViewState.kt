package chat.sphinx.dashboard.ui.viewstates

import chat.sphinx.wrapper_feed.FeedRecommendation
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class FeedAllViewState: ViewState<FeedAllViewState>() {

    object Idle: FeedAllViewState()
    class RecommendedList(feedRecommendedList: List<FeedRecommendation>) : FeedAllViewState()
}