package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun String.toChatGroupKey(): ChatGroupKey? =
    try {
        ChatGroupKey(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ChatGroupKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatGroupKey cannot be empty"
        }
    }
}
