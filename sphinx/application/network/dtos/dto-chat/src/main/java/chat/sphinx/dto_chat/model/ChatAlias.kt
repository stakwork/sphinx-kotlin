package chat.sphinx.dto_chat.model

inline class ChatAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatAlias cannot be empty"
        }
    }
}
