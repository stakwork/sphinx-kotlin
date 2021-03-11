package chat.sphinx.chat_dtos.model

inline class ChatGroupKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatGroupKey cannot be empty"
        }
    }
}