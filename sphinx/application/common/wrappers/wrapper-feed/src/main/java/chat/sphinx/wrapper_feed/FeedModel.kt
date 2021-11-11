package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.feed.FeedId

data class FeedModel(
    val id: FeedId,
    val type: FeedModelType,
    val suggested: FeedModelSuggested
)