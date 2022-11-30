package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_common.feed.*

data class FeedSearchResult(
    val id: String,
    val feedType: Long,
    val title: String,
    val url: String,
    val description: String?,
    val author: String?,
    val generator: String?,
    val imageUrl: String?,
    val ownerUrl: String?,
    val link: String?,
    val datePublished: Long?,
    val dateUpdated: Long?,
    val contentType: String?,
    val language: String?,
    val chatId: Long?
)