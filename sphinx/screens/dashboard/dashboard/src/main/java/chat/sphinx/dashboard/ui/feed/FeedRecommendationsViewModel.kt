package chat.sphinx.dashboard.ui.feed

import chat.sphinx.wrapper_podcast.FeedRecommendation
import kotlinx.coroutines.flow.StateFlow

interface FeedRecommendationsViewModel {
    val feedRecommendationsHolderViewStateFlow: StateFlow<List<FeedRecommendation>>

    fun feedRecommendationSelected(feed: FeedRecommendation)
    fun loadFeedRecommendations()
}