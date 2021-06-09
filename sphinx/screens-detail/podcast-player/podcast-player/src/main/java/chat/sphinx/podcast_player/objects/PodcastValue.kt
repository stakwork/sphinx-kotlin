package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastValueDto
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PodcastValue(
    val model: PodcastModel,
    val destinations: List<PodcastDestination>,
): Parcelable {
}

fun PodcastValueDto.toPodcastValue(): PodcastValue {
    val podcastDestinations = mutableListOf<PodcastDestination>()

    for (destination in this.destinations) {
        podcastDestinations.add(destination.toPodcastDestination())
    }

    return PodcastValue(this.model.toPodcastModel(), podcastDestinations)
}