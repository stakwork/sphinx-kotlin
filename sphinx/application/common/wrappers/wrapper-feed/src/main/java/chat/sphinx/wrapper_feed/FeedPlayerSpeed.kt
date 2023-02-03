package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun Double.toFeedPlayerSpeed(): FeedPlayerSpeed? =
    try {
        FeedPlayerSpeed(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedPlayerSpeed(val value: Double) {
    init {
        require(value >= 0) {
            "FeedPlayerSpeed must be greater than or equal to 0"
        }
    }
}