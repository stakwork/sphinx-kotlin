package chat.sphinx.wrapper_message

inline class MessageMUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MessageMUID cannot be empty"
        }
    }
}
