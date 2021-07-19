package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastModelDto
import chat.sphinx.wrapper_podcast.PodcastModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelablePodcastModel(
    val type: String,
    val suggested: Double,
): Parcelable

fun PodcastModel.toParcelablePodcastModel(): ParcelablePodcastModel {
    return ParcelablePodcastModel(this.type, this.suggested)
}

fun ParcelablePodcastModel.toPodcastModel(): PodcastModel {
    return PodcastModel(this.type, this.suggested)
}