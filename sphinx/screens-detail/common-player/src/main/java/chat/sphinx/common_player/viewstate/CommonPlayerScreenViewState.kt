package chat.sphinx.common_player.viewstate

import chat.sphinx.wrapper_feed.FeedRecommendation
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class CommonPlayerScreenViewState: ViewState<CommonPlayerScreenViewState>() {

    object Idle: CommonPlayerScreenViewState()

    class FeedRecommendations(
        val recommendations: List<FeedRecommendation>,
        val selectedRecommendation: FeedRecommendation,
    ): CommonPlayerScreenViewState()
}
