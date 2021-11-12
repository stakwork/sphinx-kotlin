package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun Double.toFeedModelSuggested(): FeedModelSuggested? =
    try {
        FeedModelSuggested(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedModelSuggested(val value: Double) {
    init {
        require(value >= 0) {
            "FeedModelSuggested must be greater than or equal to 0"
        }
    }
}