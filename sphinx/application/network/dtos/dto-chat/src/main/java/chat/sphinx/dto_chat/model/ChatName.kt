package chat.sphinx.dto_chat.model

inline class ChatName(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatName cannot be empty"
        }
    }
}
