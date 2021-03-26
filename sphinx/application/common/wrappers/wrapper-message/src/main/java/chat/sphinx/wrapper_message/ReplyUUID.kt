package chat.sphinx.wrapper_message

inline class ReplyUUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ReplyUUID cannot be empty"
        }
    }
}
