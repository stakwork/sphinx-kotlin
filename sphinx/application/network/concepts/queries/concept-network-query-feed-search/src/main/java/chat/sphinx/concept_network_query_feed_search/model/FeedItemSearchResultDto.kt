package chat.sphinx.concept_network_query_feed_search.model

import chat.sphinx.wrapper_feed.htmlToPlainText
import chat.sphinx.wrapper_podcast.FeedSearchResult
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedItemSearchResultDto(
    val id: String,
    val title: String,
    val description: String,
    val datePublished: Long,
    val dateUpdated: Long,
    val author: String,
    val enclosureUrl: String,
    val enclosureType: String,
    val duration: Long,
    val imageUrl: String,
    val thumbnailUrl: String,
    val link: String,
    val feedId: String,
    val feedType: Long,
    val url: String
)

inline fun FeedItemSearchResultDto.toFeedItemSearchResult() : FeedSearchResult {
    return FeedSearchResult(
        id = feedId,
        title = title,
        description = description.htmlToPlainText().trim(),
        datePublished = datePublished,
        dateUpdated = dateUpdated,
        author = author,
        url = url,
        contentType = enclosureType,
        link = link,
        imageUrl = imageUrl,
        feedType = feedType,
        generator = "",
        ownerUrl = "",
        language = null,
        chatId = null,
        feedItemId = id
    )
}