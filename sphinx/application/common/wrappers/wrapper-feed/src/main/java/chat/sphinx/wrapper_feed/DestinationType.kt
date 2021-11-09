package chat.sphinx.wrapper_feed

@JvmInline
value class FeedDestinationType(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedDestinationType cannot be empty"
        }
    }
}