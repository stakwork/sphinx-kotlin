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

@Suppress("NOTHING_TO_INLINE")
inline fun MessagePerson.uuid(): String {
    this.value.split("/")?.let { elements ->
        if (elements.isNotEmpty()) {
            return elements.last()
        }
    }
    return ""
}


@Suppress("NOTHING_TO_INLINE")
inline fun MessagePerson.host(): String? {
    this.uuid()?.let { uuid ->
        return this.value.replace("/$uuid", "")
    }
    return ""
}


