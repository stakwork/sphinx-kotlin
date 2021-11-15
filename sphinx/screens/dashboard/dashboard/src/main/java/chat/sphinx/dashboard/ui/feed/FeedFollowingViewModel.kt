package chat.sphinx.dashboard.ui.feed

import chat.sphinx.wrapper_feed.Feed
import kotlinx.coroutines.flow.StateFlow

interface FeedFollowingViewModel {
    val feedsHolderViewStateFlow: StateFlow<List<Feed>>

    fun feedSelected(feed: Feed)
}