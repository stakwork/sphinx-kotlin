package chat.sphinx.wrapper_feed

@JvmInline
value class FeedEnclosureLength(val value: Long) {
    init {
        require(value >= 0) {
            "FeedEnclosureLength must be greater than or equal to 0"
        }
    }
}