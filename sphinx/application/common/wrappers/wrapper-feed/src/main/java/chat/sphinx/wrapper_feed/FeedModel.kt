package chat.sphinx.wrapper_feed

data class FeedModel(
    val id: FeedId,
    val type: FeedModelType,
    val suggested: FeedModelSuggested
)