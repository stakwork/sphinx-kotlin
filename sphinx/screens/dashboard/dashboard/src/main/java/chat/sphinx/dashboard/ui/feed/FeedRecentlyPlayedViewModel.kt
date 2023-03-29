package chat.sphinx.dashboard.ui.feed

import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_podcast.FeedRecommendation
import kotlinx.coroutines.flow.StateFlow

interface FeedRecentlyPlayedViewModel {
    val lastPlayedFeedsHolderViewStateFlow: StateFlow<List<Feed>>

    fun recentlyPlayedSelected(feed: Feed)

}