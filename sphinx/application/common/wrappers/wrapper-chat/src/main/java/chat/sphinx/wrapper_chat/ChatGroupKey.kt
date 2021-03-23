package chat.sphinx.wrapper_chat

inline class ChatGroupKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatGroupKey cannot be empty"
        }
    }
}
