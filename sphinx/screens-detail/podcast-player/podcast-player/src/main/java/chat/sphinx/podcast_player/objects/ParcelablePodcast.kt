package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.wrapper_chat.FeedUrl
import chat.sphinx.wrapper_chat.toFeedUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.toChatId
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastDestination
import chat.sphinx.wrapper_podcast.PodcastEpisode
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt


@Parcelize
class ParcelablePodcast(
    val id: Long,
    val title: String,
    val description: String?,
    val author: String?,
    val image: String?,
    val episodeId: Long?,
    val episodeDuration: Long?,
    val datePublished: Long?,
    val chatId: Long,
    val feedUrl: String,
    val model: ParcelablePodcastModel?,
    val destinations: List<ParcelablePodcastDestination>,
    val episodes: List<ParcelablePodcastEpisode>,
): Parcelable

fun Podcast.toParcelablePodcast(): ParcelablePodcast {
    val podcastEpisodes: MutableList<ParcelablePodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(episode.toParcelablePodcastEpisode())
    }

    val podcastDestinations: MutableList<ParcelablePodcastDestination> = ArrayList(destinations.size)

    for (destination in destinations) {
        podcastDestinations.add(destination.toParcelablePodcastDestination())
    }

    return ParcelablePodcast(
        id.value.toLong(),
        title.value,
        description?.value,
        author?.value,
        image?.value,
        episodeId,
        episodeDuration,
        datePublished?.time,
        chatId.value,
        feedUrl.value,
        model?.toParcelablePodcastModel(),
        podcastDestinations,
        podcastEpisodes
    )
}

fun ParcelablePodcast.toPodcast(): Podcast {
    val podcastEpisodes: MutableList<PodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(episode.toPodcastEpisode())
    }

    val podcastDestinations: MutableList<PodcastDestination> = ArrayList(destinations.size)

    for (destination in destinations) {
        podcastDestinations.add(destination.toPodcastDestination())
    }

    var podcast = Podcast(
        id = id.toString().toFeedId() ?: FeedId("null"),
        title =title.toFeedTitle() ?: FeedTitle("null"),
        description =description?.toFeedDescription(),
        author = author?.toFeedAuthor(),
        image = image?.toPhotoUrl(),
        datePublished = datePublished?.toDateTime(),
        chatId = chatId?.toChatId() ?: ChatId(-1),
        feedUrl = feedUrl?.toFeedUrl() ?: FeedUrl("null")
    )
    podcast.model = model?.toPodcastModel()
    podcast.destinations = podcastDestinations
    podcast.episodes = podcastEpisodes
    podcast.episodeId = episodeId
    podcast.episodeDuration = episodeDuration

    return podcast
}