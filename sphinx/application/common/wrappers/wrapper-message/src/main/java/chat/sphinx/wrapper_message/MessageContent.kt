package chat.sphinx.wrapper_message

inline class MessageContent(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MessageContent cannot be empty"
        }
    }
}
