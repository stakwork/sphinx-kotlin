package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.FeedDestinationAddress
import chat.sphinx.wrapper_feed.FeedDestinationSplit
import chat.sphinx.wrapper_feed.FeedDestinationType


data class PodcastDestination(
    val split: FeedDestinationSplit,
    val address: FeedDestinationAddress,
    val type: FeedDestinationType,
    val podcastId: FeedId,
)