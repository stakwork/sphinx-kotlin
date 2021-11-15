package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedGenerator(): FeedGenerator? =
    try {
        FeedGenerator(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedGenerator(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedGenerator cannot be empty"
        }
    }
}