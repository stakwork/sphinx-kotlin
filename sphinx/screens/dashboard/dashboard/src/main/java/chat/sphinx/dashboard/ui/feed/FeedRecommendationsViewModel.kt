package chat.sphinx.dashboard.ui.feed

import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedRecommendations
import kotlinx.coroutines.flow.StateFlow

interface FeedRecommendationsViewModel {
    val feedRecommendationsHolderViewStateFlow: StateFlow<List<FeedRecommendations>>

    fun feedRecommendationSelected(feed: FeedRecommendations)
}