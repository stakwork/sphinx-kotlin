package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toReplyUUID(): ReplyUUID? =
    try {
        ReplyUUID(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ReplyUUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ReplyUUID cannot be empty"
        }
    }
}
