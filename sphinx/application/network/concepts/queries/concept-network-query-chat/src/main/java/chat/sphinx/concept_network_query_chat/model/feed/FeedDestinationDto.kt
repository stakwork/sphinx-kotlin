package chat.sphinx.concept_network_query_chat.model.feed

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedDestinationDto(
    val split: Long,
    val address: String,
    val type: String,
    val customKey: String?,
    val customValue: String?,
)