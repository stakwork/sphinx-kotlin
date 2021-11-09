package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedDestinationType(): FeedDestinationType? =
    try {
        FeedDestinationType(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedDestinationType(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedDestinationType cannot be empty"
        }
    }
}