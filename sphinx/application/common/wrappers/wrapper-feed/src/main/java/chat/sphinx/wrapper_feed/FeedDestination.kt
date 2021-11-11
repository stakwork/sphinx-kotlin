package chat.sphinx.wrapper_feed

data class FeedDestination(
    val address: FeedDestinationAddress,
    val split: FeedDestinationSplit,
    val type: FeedDestinationType,
    val feedId: FeedId
)