package chat.sphinx.podcast_player.objects

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt


@Parcelize
class ParcelablePodcast(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val episodeId: Long?,
    val episodeDuration: Long?,
    val value: ParcelablePodcastValue,
    val episodes: List<ParcelablePodcastEpisode>,
): Parcelable

fun Podcast.toParcelablePodcast(): ParcelablePodcast {
    val podcastEpisodes: MutableList<ParcelablePodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(episode.toParcelablePodcastEpisode())
    }

    return ParcelablePodcast(id, title, description, author, image, episodeId, episodeDuration, value.toParcelablePodcastValue(), podcastEpisodes)
}

fun ParcelablePodcast.toPodcast(): Podcast {
    val podcastEpisodes: MutableList<PodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(episode.toPodcastEpisode())
    }

    var podcast = Podcast(id, title, description, author, image, value.toPodcastValue(), podcastEpisodes)
    podcast.episodeId = episodeId
    podcast.episodeDuration = episodeDuration

    return podcast
}