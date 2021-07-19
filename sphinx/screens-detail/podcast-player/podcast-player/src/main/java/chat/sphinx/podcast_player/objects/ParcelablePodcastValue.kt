package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastValueDto
import chat.sphinx.wrapper_podcast.PodcastDestination
import chat.sphinx.wrapper_podcast.PodcastValue
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelablePodcastValue(
    val model: ParcelablePodcastModel,
    val destinations: List<ParcelablePodcastDestination>,
): Parcelable {
}

fun PodcastValueDto.toParcelablePodcastValue(): ParcelablePodcastValue {
    val podcastDestinations = mutableListOf<ParcelablePodcastDestination>()

    for (destination in this.destinations) {
        podcastDestinations.add(destination.toParcelablePodcastDestination())
    }

    return ParcelablePodcastValue(this.model.toParcelablePodcastModel(), podcastDestinations)
}

fun ParcelablePodcastValue.toPodcastValue(): PodcastValue {
    val podcastDestinations = mutableListOf<PodcastDestination>()

    for (destination in this.destinations) {
        podcastDestinations.add(destination.toPodcastDestination())
    }

    return PodcastValue(this.model.toPodcastModel(), podcastDestinations)
}