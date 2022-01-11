package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMessageContentDecrypted(): MessageContentDecrypted? =
    try {
        MessageContentDecrypted(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val MessageContentDecrypted.isGiphy: Boolean
    get() = value.startsWith(GiphyData.MESSAGE_PREFIX)

inline val MessageContentDecrypted.isPodBoost: Boolean
    get() = value.startsWith(FeedBoost.MESSAGE_PREFIX)

@JvmInline
value class MessageContentDecrypted(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MessageContentDecrypted cannot be empty"
        }
    }
}
