package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedEnclosureType(): FeedEnclosureType? =
    try {
        FeedEnclosureType(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedEnclosureType(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedEnclosureType cannot be empty"
        }
    }
}