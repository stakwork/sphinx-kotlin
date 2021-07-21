package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastEpisodeDto(
    val id: Long,
    val title: String,
    val description: String,
    val image: String,
    val link: String,
    val enclosureUrl: String,
)

fun PodcastEpisodeDto.toPodcastEpisode(): PodcastEpisode {
    return PodcastEpisode(this.id, this.title ,this.description, this.image, this.link, this.enclosureUrl)
}