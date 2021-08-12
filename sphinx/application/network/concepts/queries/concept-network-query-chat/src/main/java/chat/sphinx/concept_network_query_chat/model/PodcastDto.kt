package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
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
) {

    fun isValidPodcast() : Boolean {
        return episodes.isNotEmpty() && title.isNotEmpty()
    }
}

fun PodcastDto.toPodcast(): Podcast {
    val podcastEpisodes: MutableList<PodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(episode.toPodcastEpisode())
    }

    return Podcast(id, title, description, author, image, value.toPodcastValue(), podcastEpisodes)
}