package chat.sphinx.wrapper_common.feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedUrl(): FeedUrl? =
    try {
        FeedUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun String.isYoutubeVideo(): Boolean =
    false

@JvmInline
value class FeedUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedUrl cannot be empty"
        }
    }
}
