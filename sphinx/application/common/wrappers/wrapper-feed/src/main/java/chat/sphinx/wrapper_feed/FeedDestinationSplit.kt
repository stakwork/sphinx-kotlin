package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun Double.toFeedDestinationSplit(): FeedDestinationSplit? =
    try {
        FeedDestinationSplit(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedDestinationSplit(val value: Double) {
    init {
        require(value >= 0) {
            "FeedDestinationSplit must be greater than or equal to 0"
        }
    }
}