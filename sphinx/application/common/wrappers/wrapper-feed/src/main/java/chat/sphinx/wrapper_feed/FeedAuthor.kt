package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedAuthor(): FeedAuthor? =
    try {
        FeedAuthor(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedAuthor(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedAuthor cannot be empty"
        }
    }
}