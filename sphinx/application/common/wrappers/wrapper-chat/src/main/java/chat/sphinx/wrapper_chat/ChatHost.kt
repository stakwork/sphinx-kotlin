package chat.sphinx.wrapper_chat

inline class ChatHost(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatHost cannot be empty"
        }
    }
}
