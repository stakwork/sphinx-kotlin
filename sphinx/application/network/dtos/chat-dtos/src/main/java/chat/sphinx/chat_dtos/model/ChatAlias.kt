package chat.sphinx.chat_dtos.model

inline class ChatAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatAlias cannot be empty"
        }
    }
}