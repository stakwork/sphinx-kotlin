package chat.sphinx.concept_network_query_feed_search.model

import chat.sphinx.wrapper_podcast.FeedItemSearchResult
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

inline fun FeedItemSearchResultDto.toFeedItemSearchResult() : FeedItemSearchResult {
    return FeedItemSearchResult(
        id,
        title,
        description,
        datePublished,
        dateUpdated,
        author,
        enclosureUrl,
        enclosureType,
        duration,
        imageUrl,
        thumbnailUrl,
        link,
        feedId,
        feedType,
        url,
    )
}
