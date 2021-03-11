package chat.sphinx.dto_chat.model

inline class ChatUUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatUUID cannot be empty"
        }
    }
}