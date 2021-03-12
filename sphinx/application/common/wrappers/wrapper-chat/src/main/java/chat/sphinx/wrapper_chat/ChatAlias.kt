package chat.sphinx.wrapper_chat

inline class ChatAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatAlias cannot be empty"
        }
    }
}
