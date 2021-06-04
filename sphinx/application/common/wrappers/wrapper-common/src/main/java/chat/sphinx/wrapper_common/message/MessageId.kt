package chat.sphinx.wrapper_common.message

inline val MessageId.isProvisionalMessage: Boolean
    get() = value < 0

@JvmInline
value class MessageId(val value: Long)
