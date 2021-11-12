package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedTitle(): FeedTitle? =
    try {
        FeedTitle(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedTitle(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedTitle cannot be empty"
        }
    }
}