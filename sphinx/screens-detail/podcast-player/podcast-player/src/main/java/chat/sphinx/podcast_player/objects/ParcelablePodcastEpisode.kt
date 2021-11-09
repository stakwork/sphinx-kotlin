package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastEpisodeDto
import chat.sphinx.wrapper_chat.FeedUrl
import chat.sphinx.wrapper_chat.toFeedUrl
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelablePodcastEpisode(
    val id: Long,
    val title: String,
    val description: String?,
    val image: String?,
    val link: String?,
    val enclosureUrl: String,
    val podcastId: String,
): Parcelable

fun PodcastEpisode.toParcelablePodcastEpisode(): ParcelablePodcastEpisode {
    return ParcelablePodcastEpisode(
        this.id.value.toLong(),
        this.title.value,
        this.description?.value,
        this.image?.value,
        this.link?.value,
        this.enclosureUrl.value,
        this.podcastId.value
    )
}

fun ParcelablePodcastEpisode.toPodcastEpisode(): PodcastEpisode {
    return PodcastEpisode(
        this.id.toString().toFeedId() ?: FeedId("null"),
        this.title.toFeedTitle() ?: FeedTitle("null"),
        this.description?.toFeedDescription(),
        this.image?.toPhotoUrl(),
        this.link?.toFeedUrl(),
        this.enclosureUrl.toFeedUrl() ?: FeedUrl("null"),
        this.podcastId.toFeedId() ?: FeedId("null")
    )
}