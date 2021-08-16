package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDestinationDto
import chat.sphinx.wrapper_podcast.PodcastDestination
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelablePodcastDestination(
    val split: Long,
    val address: String,
    val type: String,
    val customKey: String?,
    val customValue: String?,
): Parcelable

fun PodcastDestination.toParcelablePodcastDestination(): ParcelablePodcastDestination {
    return ParcelablePodcastDestination(this.split, this.address, this.type, this.customKey, this.customValue)
}

fun ParcelablePodcastDestination.toPodcastDestination(): PodcastDestination {
    return PodcastDestination(this.split, this.address, this.type, this.customKey, this.customValue)
}