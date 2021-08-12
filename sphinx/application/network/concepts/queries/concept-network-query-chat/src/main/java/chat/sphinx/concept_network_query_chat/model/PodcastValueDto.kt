package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_podcast.PodcastDestination
import chat.sphinx.wrapper_podcast.PodcastValue
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastValueDto(
    val model: PodcastModelDto,
    val destinations: List<PodcastDestinationDto>,
)

fun PodcastValueDto.toPodcastValue(): PodcastValue {
    val podcastDestinations = mutableListOf<PodcastDestination>()

    for (destination in this.destinations) {
        podcastDestinations.add(destination.toPodcastDestination())
    }

    return PodcastValue(this.model.toPodcastModel(), podcastDestinations)
}