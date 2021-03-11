package chat.sphinx.chat_dtos.model

inline class ChatHost(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatHost cannot be empty"
        }
    }
}