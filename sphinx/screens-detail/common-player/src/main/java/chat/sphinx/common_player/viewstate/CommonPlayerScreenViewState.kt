package chat.sphinx.common_player.viewstate

import chat.sphinx.wrapper_feed.FeedRecommendation
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class CommonPlayerScreenViewState: ViewState<CommonPlayerScreenViewState>() {

    object Idle: CommonPlayerScreenViewState()

    sealed class FeedRecommendations(
        val recommendations: List<FeedRecommendation>,
        val selectedItem: FeedRecommendation,
    ): CommonPlayerScreenViewState() {

        class PodcastSelected(
            recommendations: List<FeedRecommendation>,
            selectedPodcast: FeedRecommendation,
        ) : FeedRecommendations(
            recommendations,
            selectedPodcast
        )

        class YouTubeVideoSelected(
            recommendations: List<FeedRecommendation>,
            selectedYouTubeVideo: FeedRecommendation,
        ) : FeedRecommendations(
            recommendations,
            selectedYouTubeVideo
        )

    }
}
