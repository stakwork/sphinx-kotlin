package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMessageMUID(): MessageMUID? =
    try {
        MessageMUID(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class MessageMUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MessageMUID cannot be empty"
        }
    }
}
