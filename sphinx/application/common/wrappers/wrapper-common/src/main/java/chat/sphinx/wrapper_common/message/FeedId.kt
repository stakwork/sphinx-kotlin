package chat.sphinx.wrapper_common.message

@JvmInline
value class FeedId(val value: Long) {
    init {
        require(value >= 0) {
            "FeedId must be greater than or equal to 0"
        }
    }
}
