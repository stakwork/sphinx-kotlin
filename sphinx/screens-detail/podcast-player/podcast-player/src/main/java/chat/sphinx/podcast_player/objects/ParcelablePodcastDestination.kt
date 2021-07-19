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
): Parcelable {
}

fun PodcastDestinationDto.toParcelablePodcastDestination(): ParcelablePodcastDestination {
    return ParcelablePodcastDestination(this.split, this.address, this.type)
}

fun ParcelablePodcastDestination.toPodcastDestination(): PodcastDestination {
    return PodcastDestination(this.split, this.address, this.type)
}