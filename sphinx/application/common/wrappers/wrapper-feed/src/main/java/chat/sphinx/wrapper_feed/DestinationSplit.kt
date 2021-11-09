package chat.sphinx.wrapper_feed

@JvmInline
value class FeedDestinationSplit(val value: Double) {
    init {
        require(value >= 0) {
            "FeedDestinationSplit must be greater than or equal to 0"
        }
    }
}