package chat.sphinx.wrapper_feed

@JvmInline
value class FeedModelType(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedModelType cannot be empty"
        }
    }
}