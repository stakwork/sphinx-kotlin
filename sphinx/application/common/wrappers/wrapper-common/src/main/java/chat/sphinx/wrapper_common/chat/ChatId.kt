package chat.sphinx.wrapper_common.chat

@JvmInline
value class ChatId(val value: Long) {

    companion object {
        const val NULL_CHAT_ID = Int.MAX_VALUE
    }

    init {
        require(value >= 0L) {
            "ChatId must be greater than or equal 0"
        }
    }
}
