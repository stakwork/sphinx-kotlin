package chat.sphinx.dto_chat.model

inline class ChatGroupKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatGroupKey cannot be empty"
        }
    }
}