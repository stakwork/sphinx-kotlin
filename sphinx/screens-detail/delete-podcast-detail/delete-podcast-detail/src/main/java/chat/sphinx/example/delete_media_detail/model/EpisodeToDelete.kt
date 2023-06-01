package chat.sphinx.example.delete_media_detail.model

import chat.sphinx.wrapper_feed.FeedItem

data class EpisodeToDelete(
    val feedItem: FeedItem,
    val size: String
)
