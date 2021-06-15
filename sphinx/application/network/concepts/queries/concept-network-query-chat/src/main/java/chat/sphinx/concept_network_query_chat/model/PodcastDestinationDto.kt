package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastDestinationDto(
    val split: Long,
    val address: String,
    val type: String,
) {
}