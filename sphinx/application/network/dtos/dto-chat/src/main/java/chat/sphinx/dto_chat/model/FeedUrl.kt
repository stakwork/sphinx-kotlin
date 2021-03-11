package chat.sphinx.dto_chat.model

inline class FeedUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedUrl cannot be empty"
        }
    }
}