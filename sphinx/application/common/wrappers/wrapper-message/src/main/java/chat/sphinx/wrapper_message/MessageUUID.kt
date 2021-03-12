package chat.sphinx.wrapper_message

inline class MessageUUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MessageUUID cannot be empty"
        }
    }
}
