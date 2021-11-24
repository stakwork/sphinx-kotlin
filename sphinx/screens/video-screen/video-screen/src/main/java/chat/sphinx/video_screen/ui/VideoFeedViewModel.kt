package chat.sphinx.video_screen.ui

import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface VideoFeedViewModel {
    val videoFeedSharedFlow: SharedFlow<Feed?>

    val feedItemsHolderViewStateFlow: StateFlow<List<FeedItem>>

    fun episodeSelected(videoEpisode: FeedItem) {
        // TODO: Show go to watch this videoEpisode
    }
}