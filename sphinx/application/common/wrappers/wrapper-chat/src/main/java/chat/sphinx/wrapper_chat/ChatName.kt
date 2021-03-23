package chat.sphinx.wrapper_chat

inline class ChatName(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatName cannot be empty"
        }
    }
}
