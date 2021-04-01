package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMessageContentDecrypted(): MessageContentDecrypted? =
    try {
        MessageContentDecrypted(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline class MessageContentDecrypted(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MessageContentDecrypted cannot be empty"
        }
    }
}
