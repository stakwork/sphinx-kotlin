package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.feed.FeedId

data class ContentEpisodeStatus(
    val feedId: FeedId,
    val itemId: FeedId,
    val duration: FeedItemDuration,
    val currentTime: FeedItemDuration,
    val played: Boolean? = null
)