package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_chat.FeedUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastDestination
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
)

fun PodcastDto.toPodcast(
    chatId: ChatId,
    feedUrl: FeedUrl,
): Podcast? {

    val podcastId = id.toString().toFeedId()
    val podcastTitle = title.toFeedTitle()

    if (podcastId == null || podcastTitle == null) {
        return null
    }

    val podcastEpisodes: MutableList<PodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(
            episode.toPodcastEpisode(podcastId)
        )
    }

    val podcastDestinations: MutableList<PodcastDestination> = ArrayList(value.destinations.size)

    for (destination in value.destinations) {
        podcastDestinations.add(
            destination.toPodcastDestination(podcastId)
        )
    }

    var podcast = Podcast(
        id = podcastId,
        title = podcastTitle,
        description =description.toFeedDescription(),
        author = author.toFeedAuthor(),
        image = image.toPhotoUrl(),
        datePublished = null,
        chatId = chatId,
        feedUrl = feedUrl
    )
    podcast.model = value.model.toPodcastModel(podcastId)
    podcast.destinations = podcastDestinations
    podcast.episodes = podcastEpisodes

    return podcast
}