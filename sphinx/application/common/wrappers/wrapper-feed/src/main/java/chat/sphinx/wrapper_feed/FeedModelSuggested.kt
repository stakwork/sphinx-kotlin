package chat.sphinx.wrapper_feed

@JvmInline
value class FeedModelSuggested(val value: Double) {
    init {
        require(value >= 0) {
            "FeedModelSuggested must be greater than or equal to 0"
        }
    }
}