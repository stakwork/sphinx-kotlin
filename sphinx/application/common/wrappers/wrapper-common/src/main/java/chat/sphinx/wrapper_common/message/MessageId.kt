package chat.sphinx.wrapper_common.message

inline class MessageId(val value: Long) {
    init {
        require(value >= 0L) {
            "MessageId must be greater than or equal 0"
        }
    }
}
