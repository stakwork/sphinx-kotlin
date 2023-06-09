package chat.sphinx.example.delete_media.model

import chat.sphinx.wrapper_common.feed.FeedId

data class PodcastToDelete(
    val title: String,
    val image: String,
    val size: String,
    val feedId: FeedId
)
