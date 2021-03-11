package chat.sphinx.chat_dtos.model

inline class ChatName(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatName cannot be empty"
        }
    }
}