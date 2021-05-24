package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMessageUUID(): MessageUUID? =
    try {
        MessageUUID(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class MessageUUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MessageUUID cannot be empty"
        }
    }
}
