package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun String.toChatAlias(): ChatAlias? =
    try {
        ChatAlias(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline class ChatAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatAlias cannot be empty"
        }
    }
}
