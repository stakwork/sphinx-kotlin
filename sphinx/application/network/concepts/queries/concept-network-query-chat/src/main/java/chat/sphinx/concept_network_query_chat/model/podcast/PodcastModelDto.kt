package chat.sphinx.concept_network_query_chat.model.podcast

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastModelDto(
    val type: String,
    val suggested: Double,
)