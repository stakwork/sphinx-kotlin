package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_podcast.PodcastDestination
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastValueDto(
    val model: PodcastModelDto,
    val destinations: List<PodcastDestinationDto>,
)