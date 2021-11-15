package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.feed.FeedId

data class FeedDestination(
    val address: FeedDestinationAddress,
    val split: FeedDestinationSplit,
    val type: FeedDestinationType,
    val feedId: FeedId
)