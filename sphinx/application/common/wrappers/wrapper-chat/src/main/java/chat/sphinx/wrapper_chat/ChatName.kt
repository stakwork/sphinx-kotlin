package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun String.toChatName(): ChatName? =
    try {
        ChatName(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ChatName(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatName cannot be empty"
        }
    }
}
