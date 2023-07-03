package chat.sphinx.dashboard.ui.feed

import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_podcast.FeedRecommendation
import kotlinx.coroutines.flow.StateFlow

interface FeedDownloadedViewModel {
    val feedDownloadedHolderViewStateFlow: StateFlow<List<FeedItem>>

    fun feedDownloadedSelected(feedItem: FeedItem)
}