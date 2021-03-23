package chat.sphinx.wrapper_common.chat

inline class ChatId(val value: Long) {
    init {
        require(value >= 0L) {
            "ChatId must be greater than or equal 0"
        }
    }
}
