package chat.sphinx.wrapper_feed

@JvmInline
value class FeedEnclosureType(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedEnclosureType cannot be empty"
        }
    }
}