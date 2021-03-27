package chat.sphinx.wrapper_message

inline class MessageContentDecrypted(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MessageContentDecrypted cannot be empty"
        }
    }
}
