package chat.sphinx.dto_chat.model

inline class ChatHost(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatHost cannot be empty"
        }
    }
}