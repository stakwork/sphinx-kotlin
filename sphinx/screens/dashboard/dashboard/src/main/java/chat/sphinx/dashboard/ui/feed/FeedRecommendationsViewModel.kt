package chat.sphinx.dashboard.ui.feed

import chat.sphinx.wrapper_feed.Feed
import kotlinx.coroutines.flow.StateFlow

interface FeedRecommendationsViewModel {
    val feedRecommendationsHolderViewStateFlow: StateFlow<List<Feed>>

    fun feedRecommendationSelected(feed: Feed)
}