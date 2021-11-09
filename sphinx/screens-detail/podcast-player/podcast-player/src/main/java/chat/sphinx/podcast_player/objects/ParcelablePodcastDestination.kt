package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDestinationDto
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.PodcastDestination
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelablePodcastDestination(
    val split: Long,
    val address: String,
    val type: String,
    val podcastId: String,
): Parcelable

fun PodcastDestination.toParcelablePodcastDestination(): ParcelablePodcastDestination {
    return ParcelablePodcastDestination(
        this.split.value.toLong(),
        this.address.value,
        this.type.value,
        this.podcastId.value
    )
}

fun ParcelablePodcastDestination.toPodcastDestination(): PodcastDestination {
    return PodcastDestination(
        this.split.toDouble().toFeedDestinationSplit() ?: FeedDestinationSplit(0.0),
        this.address.toFeedDestinationAddress() ?: FeedDestinationAddress("null"),
        this.type.toFeedDestinationType() ?: FeedDestinationType("null"),
        this.podcastId.toFeedId() ?: FeedId("null")
    )
}