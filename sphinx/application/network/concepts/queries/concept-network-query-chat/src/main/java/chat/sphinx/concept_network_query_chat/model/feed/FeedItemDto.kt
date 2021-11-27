package chat.sphinx.concept_network_query_chat.model.feed

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedItemDto(
    val id: String,
    val title: String,
    val description: String?,
    val datePublished: Long?,
    val dateUpdated: Long?,
    val author: String?,
    val contentType: String?,
    val enclosureUrl: String,
    val enclosureType: String?,
    val enclosureLength: Long?,
    val duration: Long?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val link: String?
)