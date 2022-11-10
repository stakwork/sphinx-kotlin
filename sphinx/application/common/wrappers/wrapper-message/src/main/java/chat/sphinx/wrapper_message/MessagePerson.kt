package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMessagePerson(): MessagePerson? =
    try {
        MessagePerson(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class MessagePerson(val value: String)
