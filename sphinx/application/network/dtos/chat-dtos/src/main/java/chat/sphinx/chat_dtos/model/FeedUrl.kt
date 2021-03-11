package chat.sphinx.chat_dtos.model

inline class FeedUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedUrl cannot be empty"
        }
    }
}