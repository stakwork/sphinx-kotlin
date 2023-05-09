package chat.sphinx.wrapper_common.message

inline val MessageId.isProvisionalMessage: Boolean
    get() = value < 0

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMessageId(): MessageId? =
    try {
        MessageId(this.toLong())
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class MessageId(val value: Long)
