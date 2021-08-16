package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_podcast.PodcastDestination
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastDestinationDto(
    val split: Long,
    val address: String,
    val type: String,
    val customKey: String?,
    val customValue: String?,
)

fun PodcastDestinationDto.toPodcastDestination(): PodcastDestination {
    return PodcastDestination(this.split, this.address, this.type, this.customKey, this.customValue)
}