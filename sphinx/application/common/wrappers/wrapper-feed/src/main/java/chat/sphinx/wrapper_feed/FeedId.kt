package chat.sphinx.wrapper_feed

@JvmInline
value class FeedId(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedId cannot be empty"
        }
    }
}