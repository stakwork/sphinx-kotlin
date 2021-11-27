package chat.sphinx.concept_network_query_chat.model.podcast

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastDto(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val value: PodcastValueDto,
    val episodes: List<PodcastEpisodeDto>,
)