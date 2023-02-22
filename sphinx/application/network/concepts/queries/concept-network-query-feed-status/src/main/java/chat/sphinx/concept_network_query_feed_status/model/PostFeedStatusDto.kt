package chat.sphinx.concept_network_query_feed_status.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostFeedStatusDto(
    val contents: List<ContentFeedStatusDto>
)
