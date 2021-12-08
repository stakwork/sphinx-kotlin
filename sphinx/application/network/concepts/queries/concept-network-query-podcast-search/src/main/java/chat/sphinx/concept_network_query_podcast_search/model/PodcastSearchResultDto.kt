package chat.sphinx.concept_network_query_podcast_search.model

import chat.sphinx.wrapper_podcast.PodcastSearchResult
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastSearchResultDto(
    val id: String,
    val feedType: Long,
    val title: String,
    val url: String,
    val description: String,
    val author: String,
    val generator: String,
    val imageUrl: String,
    val ownerUrl: String,
    val link: String,
    val datePublished: Long,
    val dateUpdated: Long,
    val contentType: String,
    val language: String,
)

inline fun PodcastSearchResultDto.toPodcastSearchResult() : PodcastSearchResult {
     return PodcastSearchResult(
         id,
         feedType,
         title,
         url,
         description,
         author,
         generator,
         imageUrl,
         ownerUrl,
         link,
         datePublished,
         dateUpdated,
         contentType,
         language,
         null
     )
}