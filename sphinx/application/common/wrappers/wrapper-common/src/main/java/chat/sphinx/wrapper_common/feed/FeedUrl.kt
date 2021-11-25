package chat.sphinx.wrapper_common.feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedUrl(): FeedUrl? =
    try {
        FeedUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun FeedUrl.isYoutubeVideo(): Boolean =
    value.contains("www.youtube.com")

@JvmInline
value class FeedUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedUrl cannot be empty"
        }
    }
}
