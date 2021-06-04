package chat.sphinx.wrapper_common.chat

@JvmInline
value class ChatUUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatUUID cannot be empty"
        }
    }
}
