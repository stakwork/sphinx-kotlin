package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedUrl(): FeedUrl? =
    try {
        FeedUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline class FeedUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedUrl cannot be empty"
        }
    }
}
