package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.feed.FeedId

data class ContentEpisodeStatus(
    val feedId: FeedId,
    val itemId: ItemId,
    val duration: FeedItemDuration,
    val currentTime: FeedItemDuration
)